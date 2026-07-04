package com.agrochain.backend.controller;

import com.agrochain.backend.dto.InitiatePaymentRequest;
import com.agrochain.backend.dto.PaymentResponse;
import com.agrochain.backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(Authentication authentication,
                                                            @Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.ok(paymentService.initiatePayment(authentication.getName(), request));
    }

    @GetMapping("/verify/{reference}")
    public ResponseEntity<PaymentResponse> verifyPayment(@PathVariable String reference) {
        return ResponseEntity.ok(paymentService.verifyPayment(reference));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PaymentResponse>> getHistory(Authentication authentication) {
        return ResponseEntity.ok(paymentService.getHistory(authentication.getName()));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestHeader(value = "x-paystack-signature", required = false) String signature,
            @RequestBody String rawPayload) {
        paymentService.handleWebhook(signature, rawPayload);
        return ResponseEntity.ok(Map.of("message", "Webhook processed."));
    }
}
