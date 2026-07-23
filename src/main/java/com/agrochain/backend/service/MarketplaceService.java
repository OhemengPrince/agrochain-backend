package com.agrochain.backend.service;

import com.agrochain.backend.dto.CreateListingRequest;
import com.agrochain.backend.dto.ListingResponse;
import com.agrochain.backend.exception.ResourceNotFoundException;
import com.agrochain.backend.exception.UnauthorizedException;
import com.agrochain.backend.model.ListingCategory;
import com.agrochain.backend.model.ListingStatus;
import com.agrochain.backend.model.MarketplaceListing;
import com.agrochain.backend.model.NotificationType;
import com.agrochain.backend.model.Review;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.MarketplaceListingRepository;
import com.agrochain.backend.repository.ReviewRepository;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private final MarketplaceListingRepository marketplaceListingRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final FollowService followService;

    public List<ListingResponse> getListings(ListingCategory category, String region, String query,
                                              BigDecimal minPrice, BigDecimal maxPrice) {
        return marketplaceListingRepository.search(category, region, query, minPrice, maxPrice).stream()
                .map(listing -> MarketplaceMapper.toResponse(listing, getSellerRating(listing.getSeller())))
                .toList();
    }

    public ListingResponse getListingById(Long id) {
        MarketplaceListing listing = findListingOrThrow(id);
        listing.setViewCount(listing.getViewCount() + 1);
        MarketplaceListing saved = marketplaceListingRepository.save(listing);
        return MarketplaceMapper.toResponse(saved, getSellerRating(saved.getSeller()));
    }

    public List<ListingResponse> getMyListings(String userEmail) {
        User seller = getUserOrThrow(userEmail);
        return marketplaceListingRepository.findBySeller(seller).stream()
                .map(listing -> MarketplaceMapper.toResponse(listing, getSellerRating(seller)))
                .toList();
    }

    public ListingResponse createListing(String userEmail, CreateListingRequest request) {
        User seller = getUserOrThrow(userEmail);

        MarketplaceListing listing = MarketplaceListing.builder()
                .seller(seller)
                .category(request.getCategory())
                .name(request.getName())
                .description(request.getDescription())
                .priceType(request.getPriceType())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .photoUrls(request.getPhotoUrls() == null || request.getPhotoUrls().isEmpty()
                        ? null
                        : String.join(",", request.getPhotoUrls()))
                .region(request.getRegion())
                .district(request.getDistrict())
                .contactPreference(request.getContactPreference())
                .status(ListingStatus.ACTIVE)
                .viewCount(0)
                .build();

        MarketplaceListing saved = marketplaceListingRepository.save(listing);

        followService.notifyFollowers(seller.getId(),
                seller.getFullName() + " posted " + saved.getName() + " for GHS " + saved.getPrice(),
                NotificationType.NEW_LISTING);

        return MarketplaceMapper.toResponse(saved, getSellerRating(seller));
    }

    public ListingResponse updateListingStatus(String userEmail, Long id, ListingStatus status) {
        MarketplaceListing listing = findListingOrThrow(id);
        requireOwnership(listing, userEmail);

        listing.setStatus(status);
        MarketplaceListing saved = marketplaceListingRepository.save(listing);
        return MarketplaceMapper.toResponse(saved, getSellerRating(saved.getSeller()));
    }

    public void deleteListing(String userEmail, Long id) {
        MarketplaceListing listing = findListingOrThrow(id);
        requireOwnership(listing, userEmail);
        marketplaceListingRepository.delete(listing);
    }

    private Double getSellerRating(User seller) {
        List<Review> reviews = reviewRepository.findByReviewee(seller);
        if (reviews.isEmpty()) {
            return null;
        }
        return reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
    }

    private void requireOwnership(MarketplaceListing listing, String userEmail) {
        if (!listing.getSeller().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("You do not own this listing");
        }
    }

    private MarketplaceListing findListingOrThrow(Long id) {
        return marketplaceListingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
