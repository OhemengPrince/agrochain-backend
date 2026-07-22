package com.agrochain.backend.controller;

import com.agrochain.backend.dto.InitiateProducePurchaseRequest;
import com.agrochain.backend.dto.ProducePurchaseResponse;
import com.agrochain.backend.dto.PurchaseInitiationResponse;
import com.agrochain.backend.dto.PurchaseReviewRequest;
import com.agrochain.backend.service.ProducePurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/produce")
@RequiredArgsConstructor
public class ProducePurchaseController {

    private final ProducePurchaseService purchaseService;

    @PostMapping("/batches/{batchId}/purchase")
    public ResponseEntity<PurchaseInitiationResponse> purchase(Authentication authentication,
            @PathVariable Long batchId, @Valid @RequestBody InitiateProducePurchaseRequest request) {
        return ResponseEntity.ok(purchaseService.initiatePurchase(authentication.getName(), batchId, request));
    }

    @PostMapping("/purchases/{id}/confirm-payment")
    public ResponseEntity<ProducePurchaseResponse> confirmPayment(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.confirmPayment(authentication.getName(), id));
    }

    @PatchMapping("/purchases/{id}/mark-delivered")
    public ResponseEntity<ProducePurchaseResponse> markDelivered(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.markDelivered(authentication.getName(), id));
    }

    @PatchMapping("/purchases/{id}/confirm-receipt")
    public ResponseEntity<ProducePurchaseResponse> confirmReceipt(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.confirmReceipt(authentication.getName(), id));
    }

    @PostMapping("/purchases/{id}/cancel")
    public ResponseEntity<ProducePurchaseResponse> cancel(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.cancelPurchase(authentication.getName(), id));
    }

    @PostMapping("/purchases/{id}/review")
    public ResponseEntity<Map<String, String>> submitReview(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody PurchaseReviewRequest request) {
        purchaseService.submitReview(authentication.getName(), id, request);
        return ResponseEntity.ok(Map.of("message", "Review submitted successfully."));
    }

    @GetMapping("/purchases/mine")
    public ResponseEntity<Page<ProducePurchaseResponse>> getMine(Authentication authentication,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(purchaseService.getMyPurchases(authentication.getName(), pageable));
    }

    @GetMapping("/purchases/incoming")
    public ResponseEntity<Page<ProducePurchaseResponse>> getIncoming(Authentication authentication,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(purchaseService.getIncomingPurchases(authentication.getName(), pageable));
    }
}
