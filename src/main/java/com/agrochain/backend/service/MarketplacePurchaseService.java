package com.agrochain.backend.service;

import com.agrochain.backend.dto.InitiateMarketplacePurchaseRequest;
import com.agrochain.backend.dto.MarketplacePurchaseResponse;
import com.agrochain.backend.dto.PaystackInitResponse;
import com.agrochain.backend.dto.PaystackVerifyResponse;
import com.agrochain.backend.dto.PurchaseInitiationResponse;
import com.agrochain.backend.dto.PurchaseReviewRequest;
import com.agrochain.backend.exception.BadRequestException;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.exception.UnauthorizedException;
import com.agrochain.backend.model.EarningsTransactionStatus;
import com.agrochain.backend.model.ListingStatus;
import com.agrochain.backend.model.MarketplaceListing;
import com.agrochain.backend.model.MarketplacePurchase;
import com.agrochain.backend.model.MomoNetwork;
import com.agrochain.backend.model.NotificationType;
import com.agrochain.backend.model.PurchaseStatus;
import com.agrochain.backend.model.Review;
import com.agrochain.backend.model.TransactionType;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.MarketplaceListingRepository;
import com.agrochain.backend.repository.MarketplacePurchaseRepository;
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

/**
 * Mirrors the Booking -> Payment -> Earnings pattern already established for
 * equipment rentals, but self-contained (MarketplacePurchase carries both the
 * order and the Paystack charge fields rather than splitting into two
 * entities), matching the task's literal entity design.
 *
 * Paystack integration here uses this project's existing direct-charge
 * pattern (POST /charge with a mobile_money or bank object), not hosted
 * checkout — so PurchaseInitiationResponse.paystackUrl is always null; the
 * buyer confirms via a USSD/OTP prompt on their device instead. Bank charges
 * additionally require an OTP-submission step Paystack's API returns but
 * this task didn't ask for a continuation endpoint to handle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplacePurchaseService {

    private static final String PAYSTACK_CHARGE_URL = "https://api.paystack.co/charge";
    private static final String PAYSTACK_VERIFY_URL = "https://api.paystack.co/transaction/verify/";
    private static final BigDecimal BUYER_FEE_RATE = new BigDecimal("0.05");
    private static final BigDecimal AGROCHAIN_FEE_RATE = new BigDecimal("0.15");
    private static final BigDecimal SELLER_NET_RATE = new BigDecimal("0.90");
    private static final BigDecimal CANCELLATION_FEE_RATE = new BigDecimal("0.05");
    static final int AUTO_CONFIRM_HOURS = 48;

    private final MarketplacePurchaseRepository purchaseRepository;
    private final MarketplaceListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final EarningsService earningsService;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate;

    @Value("${paystack.secret.key}")
    private String paystackSecretKey;

    public PurchaseInitiationResponse initiatePurchase(String buyerEmail, Long listingId,
                                                         InitiateMarketplacePurchaseRequest request) {
        User buyer = getUserOrThrow(buyerEmail);
        MarketplaceListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BadRequestException("This listing is not available for purchase");
        }
        if (listing.getSeller().getId().equals(buyer.getId())) {
            throw new BadRequestException("You cannot purchase your own listing");
        }

        BigDecimal baseAmount = listing.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal buyerFee = baseAmount.multiply(BUYER_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = baseAmount.add(buyerFee);
        BigDecimal agrochainFee = baseAmount.multiply(AGROCHAIN_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sellerNet = baseAmount.multiply(SELLER_NET_RATE).setScale(2, RoundingMode.HALF_UP);

        MarketplacePurchase purchase = MarketplacePurchase.builder()
                .buyer(buyer)
                .listing(listing)
                .quantity(request.getQuantity())
                .unitPrice(listing.getPrice())
                .baseAmount(baseAmount)
                .totalAmount(totalAmount)
                .agrochainFee(agrochainFee)
                .sellerNet(sellerNet)
                .status(PurchaseStatus.PENDING_PAYMENT)
                .paymentMethod(request.getPaymentMethod())
                .build();
        MarketplacePurchase saved = purchaseRepository.save(purchase);

        String reference = initiateCharge(buyer, totalAmount, request);
        saved.setPaystackReference(reference);
        saved = purchaseRepository.save(saved);

        return PurchaseInitiationResponse.builder()
                .purchaseId(saved.getId())
                .paystackUrl(null)
                .totalAmount(totalAmount)
                .build();
    }

    // Re-verifies against Paystack's own /transaction/verify rather than
    // trusting the caller's claim of success — this endpoint has a purchase
    // ID in the path (unlike Paystack's real, single, account-wide webhook
    // URL), so it's reachable by the buyer directly and must not blindly
    // trust an unauthenticated "payment succeeded" assertion.
    @Transactional
    public MarketplacePurchaseResponse confirmPayment(String buyerEmail, Long purchaseId) {
        MarketplacePurchase purchase = findOrThrow(purchaseId);
        requireBuyer(purchase, buyerEmail);

        if (purchase.getStatus() != PurchaseStatus.PENDING_PAYMENT) {
            return toResponse(purchase);
        }
        if (!verifyPaystackCharge(purchase.getPaystackReference())) {
            throw new BadRequestException("Payment could not be verified");
        }

        purchase.setStatus(PurchaseStatus.PAID);
        purchase.setPaidAt(LocalDateTime.now());
        MarketplacePurchase saved = purchaseRepository.save(purchase);

        User seller = purchase.getListing().getSeller();
        earningsService.addPendingEarning(seller.getId(), TransactionType.MARKETPLACE_SALE_INCOME,
                purchase.getBaseAmount(), "Sale income for " + purchase.getListing().getName(),
                purchase.getPaystackReference() + "-INCOME", purchase.getBuyer().getFullName(),
                null, purchase.getListing().getId());

        earningsService.recordTransaction(purchase.getBuyer().getId(), TransactionType.MARKETPLACE_PURCHASE,
                purchase.getTotalAmount(), purchase.getAgrochainFee(), purchase.getTotalAmount(),
                EarningsTransactionStatus.COMPLETED, "Payment for " + purchase.getListing().getName(),
                purchase.getPaystackReference() + "-PAYMENT", seller.getFullName(), null,
                purchase.getListing().getId(), purchase.getPaymentMethod(), null, null);

        notificationService.createNotification(seller.getId(), "New Order",
                "You have a new order for " + purchase.getListing().getName() + "!", NotificationType.NEW_ORDER);

        return toResponse(saved);
    }

    public MarketplacePurchaseResponse markShipped(String sellerEmail, Long purchaseId) {
        MarketplacePurchase purchase = findOrThrow(purchaseId);
        requireSeller(purchase, sellerEmail);
        if (purchase.getStatus() != PurchaseStatus.PAID) {
            throw new BadRequestException("Only paid orders can be marked as shipped");
        }

        LocalDateTime now = LocalDateTime.now();
        purchase.setStatus(PurchaseStatus.DELIVERED);
        purchase.setShippedAt(now);
        purchase.setDeliveredAt(now);
        purchase.setAutoConfirmAt(now.plusHours(AUTO_CONFIRM_HOURS));
        MarketplacePurchase saved = purchaseRepository.save(purchase);

        notificationService.createNotification(purchase.getBuyer().getId(), "Order Delivered",
                purchase.getListing().getSeller().getFullName()
                        + " has marked your order as delivered. Please confirm receipt.",
                NotificationType.ORDER_SHIPPED);

        return toResponse(saved);
    }

    @Transactional
    public MarketplacePurchaseResponse confirmReceipt(String buyerEmail, Long purchaseId) {
        MarketplacePurchase purchase = findOrThrow(purchaseId);
        requireBuyer(purchase, buyerEmail);
        if (purchase.getStatus() != PurchaseStatus.DELIVERED) {
            throw new BadRequestException("Only delivered orders can be confirmed");
        }

        LocalDateTime now = LocalDateTime.now();
        purchase.setStatus(PurchaseStatus.COMPLETED);
        purchase.setBuyerConfirmedAt(now);
        purchase.setCompletedAt(now);
        MarketplacePurchase saved = purchaseRepository.save(purchase);

        earningsService.confirmEarning(purchase.getPaystackReference() + "-INCOME");

        User seller = purchase.getListing().getSeller();
        notificationService.createNotification(seller.getId(), "Order Completed",
                "Buyer confirmed receipt! GHS " + purchase.getSellerNet() + " added to your earnings.",
                NotificationType.ORDER_COMPLETED);

        return toResponse(saved);
    }

    @Transactional
    public MarketplacePurchaseResponse cancelPurchase(String buyerEmail, Long purchaseId) {
        MarketplacePurchase purchase = findOrThrow(purchaseId);
        requireBuyer(purchase, buyerEmail);
        if (purchase.getStatus() != PurchaseStatus.PENDING_PAYMENT && purchase.getStatus() != PurchaseStatus.PAID) {
            throw new BadRequestException("This order can no longer be cancelled");
        }

        boolean wasPaid = purchase.getStatus() == PurchaseStatus.PAID;
        purchase.setStatus(PurchaseStatus.CANCELLED);
        MarketplacePurchase saved = purchaseRepository.save(purchase);

        if (wasPaid) {
            earningsService.reverseEarning(purchase.getPaystackReference() + "-INCOME");

            BigDecimal cancellationFee = purchase.getTotalAmount().multiply(CANCELLATION_FEE_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal refundAmount = purchase.getTotalAmount().subtract(cancellationFee);

            earningsService.recordTransaction(purchase.getBuyer().getId(), TransactionType.REFUND,
                    purchase.getTotalAmount(), cancellationFee, refundAmount, EarningsTransactionStatus.COMPLETED,
                    "Refund for cancelled order of " + purchase.getListing().getName(),
                    purchase.getPaystackReference() + "-REFUND", purchase.getListing().getSeller().getFullName(),
                    null, purchase.getListing().getId(), null, null, null);
        }

        notificationService.createNotification(purchase.getListing().getSeller().getId(), "Order Cancelled",
                "Buyer cancelled order for " + purchase.getListing().getName(), NotificationType.ORDER_CANCELLED);

        return toResponse(saved);
    }

    public void submitReview(String buyerEmail, Long purchaseId, PurchaseReviewRequest request) {
        MarketplacePurchase purchase = findOrThrow(purchaseId);
        requireBuyer(purchase, buyerEmail);

        if (purchase.getStatus() != PurchaseStatus.COMPLETED) {
            throw new BadRequestException("Only completed orders can be reviewed");
        }
        if (reviewRepository.findByMarketplacePurchase(purchase).isPresent()) {
            throw new BadRequestException("This order has already been reviewed");
        }

        Review review = Review.builder()
                .marketplacePurchase(purchase)
                .reviewer(purchase.getBuyer())
                .reviewee(purchase.getListing().getSeller())
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
        for (MarketplacePurchase purchase : purchaseRepository.findByStatusAndAutoConfirmAtBefore(
                PurchaseStatus.DELIVERED, now)) {
            purchase.setStatus(PurchaseStatus.COMPLETED);
            purchase.setCompletedAt(now);
            purchaseRepository.save(purchase);

            earningsService.confirmEarning(purchase.getPaystackReference() + "-INCOME");

            notificationService.createNotification(purchase.getListing().getSeller().getId(),
                    "Payment Released",
                    "Payment auto-released for " + purchase.getListing().getName(),
                    NotificationType.PAYMENT_RELEASED);
        }
    }

    public Page<MarketplacePurchaseResponse> getMyPurchases(String buyerEmail, Pageable pageable) {
        User buyer = getUserOrThrow(buyerEmail);
        return purchaseRepository.findByBuyerOrderByCreatedAtDesc(buyer, pageable).map(this::toResponse);
    }

    public Page<MarketplacePurchaseResponse> getIncomingPurchases(String sellerEmail, Pageable pageable) {
        User seller = getUserOrThrow(sellerEmail);
        return purchaseRepository.findByListingSeller(seller, pageable).map(this::toResponse);
    }

    private String initiateCharge(User buyer, BigDecimal amount, InitiateMarketplacePurchaseRequest request) {
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

    private void requireBuyer(MarketplacePurchase purchase, String email) {
        if (!purchase.getBuyer().getEmail().equals(email)) {
            throw new UnauthorizedException("You do not own this order");
        }
    }

    private void requireSeller(MarketplacePurchase purchase, String email) {
        if (!purchase.getListing().getSeller().getEmail().equals(email)) {
            throw new UnauthorizedException("You do not own this listing");
        }
    }

    private MarketplacePurchase findOrThrow(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    MarketplacePurchaseResponse toResponse(MarketplacePurchase purchase) {
        return MarketplacePurchaseResponse.builder()
                .id(purchase.getId())
                .listingId(purchase.getListing().getId())
                .listingName(purchase.getListing().getName())
                .buyerId(purchase.getBuyer().getId())
                .buyerName(purchase.getBuyer().getFullName())
                .sellerId(purchase.getListing().getSeller().getId())
                .sellerName(purchase.getListing().getSeller().getFullName())
                .quantity(purchase.getQuantity())
                .unitPrice(purchase.getUnitPrice())
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
