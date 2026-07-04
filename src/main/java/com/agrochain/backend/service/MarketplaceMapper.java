package com.agrochain.backend.service;

import com.agrochain.backend.dto.ListingResponse;
import com.agrochain.backend.model.MarketplaceListing;

public final class MarketplaceMapper {

    private MarketplaceMapper() {
    }

    public static ListingResponse toResponse(MarketplaceListing listing, Double sellerRating) {
        return ListingResponse.builder()
                .id(listing.getId())
                .category(listing.getCategory())
                .name(listing.getName())
                .description(listing.getDescription())
                .priceType(listing.getPriceType())
                .price(listing.getPrice())
                .quantity(listing.getQuantity())
                .photoUrls(listing.getPhotoUrls())
                .region(listing.getRegion())
                .district(listing.getDistrict())
                .contactPreference(listing.getContactPreference())
                .status(listing.getStatus())
                .viewCount(listing.getViewCount())
                .sellerName(listing.getSeller().getFullName())
                .sellerPhone(listing.getSeller().getPhoneNumber())
                .sellerRating(sellerRating)
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .build();
    }
}
