package com.agrochain.backend.controller;

import com.agrochain.backend.dto.CreateListingRequest;
import com.agrochain.backend.dto.ListingResponse;
import com.agrochain.backend.dto.UpdateListingStatusRequest;
import com.agrochain.backend.model.ListingCategory;
import com.agrochain.backend.service.MarketplaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/marketplace")
@RequiredArgsConstructor
@Slf4j
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @GetMapping("/listings")
    public ResponseEntity<List<ListingResponse>> getListings(
            @RequestParam(required = false) ListingCategory category,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(marketplaceService.getListings(category, query));
    }

    @GetMapping("/listings/mine")
    public ResponseEntity<List<ListingResponse>> getMyListings(Authentication authentication) {
        return ResponseEntity.ok(marketplaceService.getMyListings(authentication.getName()));
    }

    @GetMapping("/listings/{id}")
    public ResponseEntity<ListingResponse> getListingById(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.getListingById(id));
    }

    @PostMapping("/listings")
    public ResponseEntity<ListingResponse> createListing(Authentication authentication,
                                                          @Valid @RequestBody CreateListingRequest request) {
        log.info("Received listing request: {}", request);
        log.info("Category: {}", request.getCategory());
        log.info("PriceType: {}", request.getPriceType());
        log.info("ContactPreference: {}", request.getContactPreference());
        log.info("PhotoUrls: {}", request.getPhotoUrls());
        return ResponseEntity.ok(marketplaceService.createListing(authentication.getName(), request));
    }

    @PatchMapping("/listings/{id}/status")
    public ResponseEntity<ListingResponse> updateListingStatus(Authentication authentication,
                                                                @PathVariable Long id,
                                                                @Valid @RequestBody UpdateListingStatusRequest request) {
        return ResponseEntity.ok(marketplaceService.updateListingStatus(authentication.getName(), id, request.getStatus()));
    }

    @DeleteMapping("/listings/{id}")
    public ResponseEntity<Void> deleteListing(Authentication authentication, @PathVariable Long id) {
        marketplaceService.deleteListing(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
