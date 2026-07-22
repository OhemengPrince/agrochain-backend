package com.agrochain.backend.service;

import com.agrochain.backend.dto.InitiateProducePurchaseRequest;
import com.agrochain.backend.dto.ProducePurchaseResponse;
import com.agrochain.backend.dto.PaystackInitResponse;
import com.agrochain.backend.dto.PaystackVerifyResponse;
import com.agrochain.backend.dto.PurchaseInitiationResponse;
import com.agrochain.backend.dto.PurchaseReviewRequest;
import com.agrochain.backend.exception.BadRequestException;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.exception.UnauthorizedException;
import com.agrochain.backend.model.BatchStatus;
import com.agrochain.backend.model.EarningsTransactionStatus;
import com.agrochain.backend.model.MomoNetwork;
import com.agrochain.backend.model.NotificationType;
import com.agrochain.backend.model.ProduceBatch;
import com.agrochain.backend.model.ProducePurchase;
import com.agrochain.backend.model.PurchaseStatus;
import com.agrochain.backend.model.Review;
import com.agrochain.backend.model.TransactionType;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.ProduceBatchRepository;
import com.agrochain.backend.repository.ProducePurchaseRepository;
import com.agrochain.backend.repository.ReviewRepository;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// Mirrors MarketplacePurchaseService — see its class comment for the shared
// design notes (self-contained purchase entity, direct Paystack charge, no
// hosted-checkout URL). The one structural difference: ProduceBatch.quantityKg
// doubles as "available quantity" and is decremented on payment / restored on
// cancellation, since there's no separate inventory field on the batch.
@Service
@RequiredArgsConstructor
@Slf4j
public class ProducePurchaseService {

    private static final String PAYSTACK_CHARGE_URL = "https://api.paystack.co/charge";
    private static final String PAYSTACK_VERIFY_URL = "https://api.paystack.co/transaction/verify/";
    private static final BigDecimal BUYER_FEE_RATE = new BigDecimal("0.05");
    private static final BigDecimal AGROCHAIN_FEE_RATE = new BigDecimal("0.15");
    private static final BigDecimal SELLER_NET_RATE = new BigDecimal("0.90");
    private static final BigDecimal CANCELLATION_FEE_RATE = new BigDecimal("0.05");
    static final int AUTO_CONFIRM_HOURS = 48;

    private final ProducePurchaseRepository purchaseRepository;
    private final ProduceBatchRepository batchRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final EarningsService earningsService;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate;

    @Value("${paystack.secret.key}")
    private String paystackSecretKey;

    public PurchaseInitiationResponse initiatePurchase(String buyerEmail, Long batchId,
                                                         InitiateProducePurchaseRequest request) {
        User buyer = getUserOrThrow(buyerEmail);
        ProduceBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found"));

        if (batch.getStatus() != BatchStatus.READY_FOR_SALE) {
            throw new BadRequestException("This batch is not available for purchase");
        }
        if (batch.getFarmer().getId().equals(buyer.getId())) {
            throw new BadRequestException("You cannot purchase your own batch");
        }
        if (batch.getPricePerKg() == null) {
            throw new BadRequestException("This batch does not have a price set yet");
        }
        if (batch.getQuantityKg().compareTo(request.getQuantityKg()) < 0) {
            throw new BadRequestException("Only " + batch.getQuantityKg() + "kg available");
        }

        BigDecimal baseAmount = batch.getPricePerKg().multiply(request.getQuantityKg())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal buyerFee = baseAmount.multiply(BUYER_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = baseAmount.add(buyerFee);
        BigDecimal agrochainFee = baseAmount.multiply(AGROCHAIN_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sellerNet = baseAmount.multiply(SELLER_NET_RATE).setScale(2, RoundingMode.HALF_UP);

        ProducePurchase purchase = ProducePurchase.builder()
                .buyer(buyer)
                .batch(batch)
                .quantityKg(request.getQuantityKg())
                .pricePerKg(batch.getPricePerKg())
                .baseAmount(baseAmount)
                .totalAmount(totalAmount)
                .agrochainFee(agrochainFee)
                .sellerNet(sellerNet)
                .status(PurchaseStatus.PENDING_PAYMENT)
                .paymentMethod(request.getPaymentMethod())
                .build();
        ProducePurchase saved = purchaseRepository.save(purchase);

        String reference = initiateCharge(buyer, totalAmount, request);
        saved.setPaystackReference(reference);
        saved = purchaseRepository.save(saved);

        return PurchaseInitiationResponse.builder()
                .purchaseId(saved.getId())
                .paystackUrl(null)
                .totalAmount(totalAmount)
                .build();
    }

    @Transactional
    public ProducePurchaseResponse confirmPayment(String buyerEmail, Long purchaseId) {
        ProducePurchase purchase = findOrThrow(purchaseId);
        requireBuyer(purchase, buyerEmail);

        if (purchase.getStatus() != PurchaseStatus.PENDING_PAYMENT) {
            return toResponse(purchase);
        }
        if (!verifyPaystackCharge(purchase.getPaystackReference())) {
            throw new BadRequestException("Payment could not be verified");
        }

        purchase.setStatus(PurchaseStatus.PAID);
        purchase.setPaidAt(LocalDateTime.now());
        ProducePurchase saved = purchaseRepository.save(purchase);

        ProduceBatch batch = purchase.getBatch();
        batch.setQuantityKg(batch.getQuantityKg().subtract(purchase.getQuantityKg()));
        batchRepository.save(batch);

        User farmer = batch.getFarmer();
        earningsService.addPendingEarning(farmer.getId(), TransactionType.PRODUCE_SALE_INCOME,
                purchase.getBaseAmount(), "Sale income for " + batch.getCropName(),
                purchase.getPaystackReference() + "-INCOME", purchase.getBuyer().getFullName(), null, null);

        earningsService.recordTransaction(purchase.getBuyer().getId(), TransactionType.PRODUCE_PURCHASE,
                purchase.getTotalAmount(), purchase.getAgrochainFee(), purchase.getTotalAmount(),
                EarningsTransactionStatus.COMPLETED, "Payment for " + batch.getCropName(),
                purchase.getPaystackReference() + "-PAYMENT", farmer.getFullName(), null, null,
                purchase.getPaymentMethod(), null, null);

        notificationService.createNotification(farmer.getId(), "New Order",
                "You have a new order for " + batch.getCropName() + "!", NotificationType.NEW_ORDER);

        return toResponse(saved);
    }

    public ProducePurchaseResponse markDelivered(String farmerEmail, Long purchaseId) {
        ProducePurchase purchase = findOrThrow(purchaseId);
        requireFarmer(purchase, farmerEmail);
        if (purchase.getStatus() != PurchaseStatus.PAID) {
            throw new BadRequestException("Only paid orders can be marked as delivered");
        }

        LocalDateTime now = LocalDateTime.now();
        purchase.setStatus(PurchaseStatus.DELIVERED);
        purchase.setShippedAt(now);
        purchase.setDeliveredAt(now);
        purchase.setAutoConfirmAt(now.plusHours(AUTO_CONFIRM_HOURS));
        ProducePurchase saved = purchaseRepository.save(purchase);

        notificationService.createNotification(purchase.getBuyer().getId(), "Order Delivered",
                purchase.getBatch().getFarmer().getFullName()
                        + " has marked your order as delivered. Please confirm receipt.",
                NotificationType.ORDER_SHIPPED);

        return toResponse(saved);
    }

    @Transactional
    public ProducePurchaseResponse confirmReceipt(String buyerEmail, Long purchaseId) {
        ProducePurchase purchase = findOrThrow(purchaseId);
        requireBuyer(purchase, buyerEmail);
        if (purchase.getStatus() != PurchaseStatus.DELIVERED) {
            throw new BadRequestException("Only delivered orders can be confirmed");
        }

        LocalDateTime now = LocalDateTime.now();
        purchase.setStatus(PurchaseStatus.COMPLETED);
        purchase.setBuyerConfirmedAt(now);
        purchase.setCompletedAt(now);
        ProducePurchase saved = purchaseRepository.save(purchase);

        earningsService.confirmEarning(purchase.getPaystackReference() + "-INCOME");

        User farmer = purchase.getBatch().getFarmer();
        notificationService.createNotification(farmer.getId(), "Order Completed",
                "Buyer confirmed receipt! GHS " + purchase.getSellerNet() + " added to your earnings.",
                NotificationType.ORDER_COMPLETED);

        return toResponse(saved);
    }

    @Transactional
    public ProducePurchaseResponse cancelPurchase(String buyerEmail, Long purchaseId) {
        ProducePurchase purchase = findOrThrow(purchaseId);
        requireBuyer(purchase, buyerEmail);
        if (purchase.getStatus() != PurchaseStatus.PENDING_PAYMENT && purchase.getStatus() != PurchaseStatus.PAID) {
            throw new BadRequestException("This order can no longer be cancelled");
        }

        boolean wasPaid = purchase.getStatus() == PurchaseStatus.PAID;
        purchase.setStatus(PurchaseStatus.CANCELLED);
        ProducePurchase saved = purchaseRepository.save(purchase);

        if (wasPaid) {
            ProduceBatch batch = purchase.getBatch();
            batch.setQuantityKg(batch.getQuantityKg().add(purchase.getQuantityKg()));
            batchRepository.save(batch);

            earningsService.reverseEarning(purchase.getPaystackReference() + "-INCOME");

            BigDecimal cancellationFee = purchase.getTotalAmount().multiply(CANCELLATION_FEE_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal refundAmount = purchase.getTotalAmount().subtract(cancellationFee);

            earningsService.recordTransaction(purchase.getBuyer().getId(), TransactionType.REFUND,
                    purchase.getTotalAmount(), cancellationFee, refundAmount, EarningsTransactionStatus.COMPLETED,
                    "Refund for cancelled order of " + purchase.getBatch().getCropName(),
                    purchase.getPaystackReference() + "-REFUND", purchase.getBatch().getFarmer().getFullName(),
                    null, null, null, null, null);
        }

        notificationService.createNotification(purchase.getBatch().getFarmer().getId(), "Order Cancelled",
                "Buyer cancelled order for " + purchase.getBatch().getCropName(), NotificationType.ORDER_CANCELLED);

        return toResponse(saved);
    }

    public void submitReview(String buyerEmail, Long purchaseId, PurchaseReviewRequest request) {
        ProducePurchase purchase = findOrThrow(purchaseId);
        requireBuyer(purchase, buyerEmail);

        if (purchase.getStatus() != PurchaseStatus.COMPLETED) {
            throw new BadRequestException("Only completed orders can be reviewed");
        }
        if (reviewRepository.findByProducePurchase(purchase).isPresent()) {
            throw new BadRequestException("This order has already been reviewed");
        }

        Review review = Review.builder()
                .producePurchase(purchase)
                .reviewer(purchase.getBuyer())
                .reviewee(purchase.getBatch().getFarmer())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        reviewRepository.save(review);
    }

    // Called hourly by AutoConfirmScheduler — releases payment for any
    // delivered order the buyer never manually confirmed within the window.
    @Transactional
    public void autoConfirmDeliveries() {
        LocalDateTime now = LocalDateTime.now();
        for (ProducePurchase purchase : purchaseRepository.findByStatusAndAutoConfirmAtBefore(
                PurchaseStatus.DELIVERED, now)) {
            purchase.setStatus(PurchaseStatus.COMPLETED);
            purchase.setCompletedAt(now);
            purchaseRepository.save(purchase);

            earningsService.confirmEarning(purchase.getPaystackReference() + "-INCOME");

            notificationService.createNotification(purchase.getBatch().getFarmer().getId(),
                    "Payment Released",
                    "Payment auto-released for " + purchase.getBatch().getCropName(),
                    NotificationType.PAYMENT_RELEASED);
        }
    }

    public Page<ProducePurchaseResponse> getMyPurchases(String buyerEmail, Pageable pageable) {
        User buyer = getUserOrThrow(buyerEmail);
        return purchaseRepository.findByBuyerOrderByCreatedAtDesc(buyer, pageable).map(this::toResponse);
    }

    public Page<ProducePurchaseResponse> getIncomingPurchases(String farmerEmail, Pageable pageable) {
        User farmer = getUserOrThrow(farmerEmail);
        return purchaseRepository.findByBatchFarmer(farmer, pageable).map(this::toResponse);
    }

    private String initiateCharge(User buyer, BigDecimal amount, InitiateProducePurchaseRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", buyer.getEmail());
        body.put("amount", amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact());
        body.put("currency", "GHS");

        if ("bank".equalsIgnoreCase(request.getPaymentMethod())) {
            Map<String, Object> bank = new HashMap<>();
            bank.put("code", request.getBankCode());
            bank.put("account_number", request.getAccountNumber());
            body.put("bank", bank);
        } else {
            Map<String, Object> momo = new HashMap<>();
            momo.put("phone", request.getPhoneNumber());
            momo.put("provider", toPaystackProviderCode(request.getNetwork()));
            body.put("mobile_money", momo);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paystackSecretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        PaystackInitResponse response;
        try {
            response = restTemplate.postForObject(PAYSTACK_CHARGE_URL, new HttpEntity<>(body, headers),
                    PaystackInitResponse.class);
        } catch (RestClientException e) {
            log.error("Paystack charge request failed: {}", e.getMessage());
            throw new BadRequestException("Payment initiation failed: " + e.getMessage());
        }

        if (response == null || !Boolean.TRUE.equals(response.getStatus()) || response.getData() == null) {
            String message = response != null ? response.getMessage() : "No response from Paystack";
            throw new BadRequestException("Payment initiation failed: " + message);
        }
        return response.getData().getReference();
    }

    private boolean verifyPaystackCharge(String reference) {
        if (reference == null) {
            return false;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paystackSecretKey);
        try {
            PaystackVerifyResponse response = restTemplate.exchange(PAYSTACK_VERIFY_URL + reference, HttpMethod.GET,
                    new HttpEntity<>(headers), PaystackVerifyResponse.class).getBody();
            return response != null && response.getData() != null
                    && "success".equalsIgnoreCase(response.getData().getStatus());
        } catch (RestClientException e) {
            log.error("Paystack verify failed: {}", e.getMessage());
            return false;
        }
    }

    private String toPaystackProviderCode(MomoNetwork network) {
        if (network == null) {
            throw new BadRequestException("Network is required for MoMo payment");
        }
        return switch (network) {
            case MTN -> "mtn";
            case VODAFONE -> "vod";
            case AIRTELTIGO -> "atl";
        };
    }

    private void requireBuyer(ProducePurchase purchase, String email) {
        if (!purchase.getBuyer().getEmail().equals(email)) {
            throw new UnauthorizedException("You do not own this order");
        }
    }

    private void requireFarmer(ProducePurchase purchase, String email) {
        if (!purchase.getBatch().getFarmer().getEmail().equals(email)) {
            throw new UnauthorizedException("You do not own this batch");
        }
    }

    private ProducePurchase findOrThrow(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    ProducePurchaseResponse toResponse(ProducePurchase purchase) {
        return ProducePurchaseResponse.builder()
                .id(purchase.getId())
                .batchId(purchase.getBatch().getId())
                .cropName(purchase.getBatch().getCropName())
                .buyerId(purchase.getBuyer().getId())
                .buyerName(purchase.getBuyer().getFullName())
                .farmerId(purchase.getBatch().getFarmer().getId())
                .farmerName(purchase.getBatch().getFarmer().getFullName())
                .quantityKg(purchase.getQuantityKg())
                .pricePerKg(purchase.getPricePerKg())
                .baseAmount(purchase.getBaseAmount())
                .totalAmount(purchase.getTotalAmount())
                .agrochainFee(purchase.getAgrochainFee())
                .sellerNet(purchase.getSellerNet())
                .status(purchase.getStatus())
                .paystackReference(purchase.getPaystackReference())
                .paymentMethod(purchase.getPaymentMethod())
                .paidAt(purchase.getPaidAt())
                .shippedAt(purchase.getShippedAt())
                .deliveredAt(purchase.getDeliveredAt())
                .buyerConfirmedAt(purchase.getBuyerConfirmedAt())
                .autoConfirmAt(purchase.getAutoConfirmAt())
                .completedAt(purchase.getCompletedAt())
                .createdAt(purchase.getCreatedAt())
                .build();
    }
}
