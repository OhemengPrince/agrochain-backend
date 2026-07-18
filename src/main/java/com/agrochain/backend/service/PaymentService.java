package com.agrochain.backend.service;

import com.agrochain.backend.dto.InitiatePaymentRequest;
import com.agrochain.backend.dto.PaymentResponse;
import com.agrochain.backend.dto.PaystackInitResponse;
import com.agrochain.backend.dto.PaystackVerifyResponse;
import com.agrochain.backend.exception.BadRequestException;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.exception.UnauthorizedException;
import com.agrochain.backend.model.*;
import com.agrochain.backend.repository.BookingRepository;
import com.agrochain.backend.repository.MarketplaceListingRepository;
import com.agrochain.backend.repository.PaymentRepository;
import com.agrochain.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final String PAYSTACK_CHARGE_URL = "https://api.paystack.co/charge";
    private static final String PAYSTACK_VERIFY_URL = "https://api.paystack.co/transaction/verify/";
    private static final String HMAC_SHA512 = "HmacSHA512";

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final MarketplaceListingRepository marketplaceListingRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EarningsService earningsService;

    @Value("${paystack.secret.key}")
    private String paystackSecretKey;

    public PaymentResponse initiatePayment(String userEmail, InitiatePaymentRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = null;
        if (request.getBookingId() != null) {
            booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
            if (!booking.getFarmer().getEmail().equals(userEmail)) {
                throw new UnauthorizedException("You do not own this booking");
            }
        }

        MarketplaceListing listing = null;
        if (request.getListingId() != null) {
            listing = marketplaceListingRepository.findById(request.getListingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        }

        Map<String, Object> momo = new HashMap<>();
        momo.put("phone", request.getMomoNumber());
        momo.put("provider", toPaystackProviderCode(request.getNetwork()));

        Map<String, Object> body = new HashMap<>();
        body.put("email", user.getEmail());
        body.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100))
                .setScale(0, java.math.RoundingMode.HALF_UP).intValueExact());
        body.put("currency", "GHS");
        body.put("mobile_money", momo);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paystackSecretKey);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        PaystackInitResponse paystackResponse;
        try {
            paystackResponse = restTemplate.postForObject(PAYSTACK_CHARGE_URL, new HttpEntity<>(body, headers),
                    PaystackInitResponse.class);
        } catch (RestClientException e) {
            log.error("Paystack charge request failed: {}", e.getMessage());
            throw new BadRequestException("Payment initiation failed: " + e.getMessage());
        }

        if (paystackResponse == null || !Boolean.TRUE.equals(paystackResponse.getStatus()) || paystackResponse.getData() == null) {
            String message = paystackResponse != null ? paystackResponse.getMessage() : "No response from Paystack";
            throw new BadRequestException("Payment initiation failed: " + message);
        }

        Payment payment = Payment.builder()
                .user(user)
                .booking(booking)
                .listing(listing)
                .amount(request.getAmount())
                .currency("GHS")
                .network(request.getNetwork())
                .momoNumber(request.getMomoNumber())
                .paystackReference(paystackResponse.getData().getReference())
                .status(TransactionStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);
        return PaymentMapper.toResponse(saved);
    }

    public PaymentResponse verifyPayment(String userEmail, String reference) {
        Payment payment = paymentRepository.findByPaystackReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for this reference"));

        if (!payment.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("You do not own this payment");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paystackSecretKey);

        PaystackVerifyResponse verifyResponse;
        try {
            verifyResponse = restTemplate.exchange(PAYSTACK_VERIFY_URL + reference, HttpMethod.GET,
                    new HttpEntity<>(headers), PaystackVerifyResponse.class).getBody();
        } catch (RestClientException e) {
            log.error("Paystack verify request failed: {}", e.getMessage());
            throw new BadRequestException("Payment verification failed: " + e.getMessage());
        }

        if (verifyResponse != null && verifyResponse.getData() != null
                && "success".equalsIgnoreCase(verifyResponse.getData().getStatus())) {
            payment.setStatus(TransactionStatus.SUCCESS);
            applySuccessToBooking(payment);
        } else {
            payment.setStatus(TransactionStatus.FAILED);
        }

        Payment saved = paymentRepository.save(payment);
        return PaymentMapper.toResponse(saved);
    }

    public List<PaymentResponse> getHistory(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return paymentRepository.findByUser(user).stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }

    public void handleWebhook(String signature, String rawPayload) {
        if (!isValidSignature(signature, rawPayload)) {
            throw new UnauthorizedException("Invalid webhook signature");
        }

        JsonNode json;
        try {
            json = objectMapper.readTree(rawPayload);
        } catch (Exception e) {
            throw new BadRequestException("Invalid webhook payload");
        }

        String event = json.path("event").asText("");
        String reference = json.path("data").path("reference").asText(null);
        if (reference == null) {
            return;
        }

        paymentRepository.findByPaystackReference(reference).ifPresent(payment -> {
            if ("charge.success".equals(event)) {
                payment.setStatus(TransactionStatus.SUCCESS);
                applySuccessToBooking(payment);
            } else {
                payment.setStatus(TransactionStatus.FAILED);
            }
            paymentRepository.save(payment);
        });
    }

    // AgroChain commission: buyer pays listed price + 5%, seller's pending
    // earning is credited on the listed price (EarningsService nets it down to
    // 90% internally), and released to available_balance later — currently at
    // BookingService.completeBooking(), not here — see that method for why.
    private static final BigDecimal BUYER_FEE_RATE = new BigDecimal("0.05");

    private void applySuccessToBooking(Payment payment) {
        if (payment.getBooking() == null) {
            return;
        }
        Booking booking = payment.getBooking();
        booking.setPaymentStatus(PaymentStatus.PAID);
        bookingRepository.save(booking);

        BigDecimal baseAmount = booking.getTotalCost();
        String reference = payment.getPaystackReference();

        earningsService.addPendingEarning(booking.getEquipment().getOwner().getId(),
                TransactionType.EQUIPMENT_RENTAL_INCOME, baseAmount,
                "Rental income for " + booking.getEquipment().getName(), reference + "-INCOME",
                booking.getFarmer().getFullName(), booking.getId(), null);

        BigDecimal buyerFee = baseAmount.multiply(BUYER_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal buyerTotal = baseAmount.add(buyerFee);
        earningsService.recordTransaction(booking.getFarmer().getId(), TransactionType.EQUIPMENT_RENTAL_PAYMENT,
                buyerTotal, buyerFee, buyerTotal, EarningsTransactionStatus.COMPLETED,
                "Payment for " + booking.getEquipment().getName(), reference + "-PAYMENT",
                booking.getEquipment().getOwner().getFullName(), booking.getId(), null,
                "MOMO", payment.getMomoNumber(), null);
    }

    private boolean isValidSignature(String signature, String rawPayload) {
        if (signature == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA512);
            mac.init(new SecretKeySpec(paystackSecretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA512));
            byte[] computed = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            return MessageDigest.isEqual(computedHex.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to verify Paystack webhook signature: {}", e.getMessage());
            return false;
        }
    }

    private String toPaystackProviderCode(MomoNetwork network) {
        return switch (network) {
            case MTN -> "mtn";
            case VODAFONE -> "vod";
            case AIRTELTIGO -> "atl";
        };
    }
}
