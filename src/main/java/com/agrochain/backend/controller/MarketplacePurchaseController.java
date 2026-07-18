package com.agrochain.backend.controller;

import com.agrochain.backend.dto.InitiateMarketplacePurchaseRequest;
import com.agrochain.backend.dto.MarketplacePurchaseResponse;
import com.agrochain.backend.dto.PurchaseInitiationResponse;
import com.agrochain.backend.service.MarketplacePurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/marketplace")
@RequiredArgsConstructor
public class MarketplacePurchaseController {

    private final MarketplacePurchaseService purchaseService;

    @PostMapping("/listings/{listingId}/purchase")
    public ResponseEntity<PurchaseInitiationResponse> purchase(Authentication authentication,
            @PathVariable Long listingId, @Valid @RequestBody InitiateMarketplacePurchaseRequest request) {
        return ResponseEntity.ok(purchaseService.initiatePurchase(authentication.getName(), listingId, request));
    }

    @PostMapping("/purchases/{id}/confirm-payment")
    public ResponseEntity<MarketplacePurchaseResponse> confirmPayment(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.confirmPayment(authentication.getName(), id));
    }

    @PatchMapping("/purchases/{id}/mark-shipped")
    public ResponseEntity<MarketplacePurchaseResponse> markShipped(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.markShipped(authentication.getName(), id));
    }

    @PatchMapping("/purchases/{id}/confirm-receipt")
    public ResponseEntity<MarketplacePurchaseResponse> confirmReceipt(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.confirmReceipt(authentication.getName(), id));
    }

    @PostMapping("/purchases/{id}/cancel")
    public ResponseEntity<MarketplacePurchaseResponse> cancel(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.cancelPurchase(authentication.getName(), id));
    }

    @GetMapping("/purchases/mine")
    public ResponseEntity<Page<MarketplacePurchaseResponse>> getMine(Authentication authentication,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(purchaseService.getMyPurchases(authentication.getName(), pageable));
    }

    @GetMapping("/purchases/incoming")
    public ResponseEntity<Page<MarketplacePurchaseResponse>> getIncoming(Authentication authentication,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(purchaseService.getIncomingPurchases(authentication.getName(), pageable));
    }
}
