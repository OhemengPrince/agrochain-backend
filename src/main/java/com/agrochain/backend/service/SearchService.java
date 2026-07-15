package com.agrochain.backend.service;

import com.agrochain.backend.dto.EquipmentSearchResult;
import com.agrochain.backend.dto.MarketplaceSearchResult;
import com.agrochain.backend.dto.PersonSearchResult;
import com.agrochain.backend.dto.ProduceSearchResult;
import com.agrochain.backend.dto.SearchResponse;
import com.agrochain.backend.model.Equipment;
import com.agrochain.backend.model.MarketplaceListing;
import com.agrochain.backend.model.ProduceBatch;
import com.agrochain.backend.model.User;
import com.agrochain.backend.repository.EquipmentRepository;
import com.agrochain.backend.repository.MarketplaceListingRepository;
import com.agrochain.backend.repository.ProduceBatchRepository;
import com.agrochain.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final ProduceBatchRepository produceBatchRepository;
    private final MarketplaceListingRepository marketplaceListingRepository;

    public SearchResponse search(String query, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(limit, 1));

        List<PersonSearchResult> people = userRepository.searchVerified(query, pageable).stream()
                .map(row -> {
                    User user = (User) row[0];
                    Double avgRating = (Double) row[1];
                    return PersonSearchResult.builder()
                            .id(user.getId())
                            .fullName(user.getFullName())
                            .role(user.getRole())
                            .region(user.getRegion())
                            .profilePhotoUrl(user.getProfilePhotoUrl())
                            .isVerified(user.isVerified())
                            .averageRating(avgRating == null ? 0.0 : Math.round(avgRating * 10) / 10.0)
                            .build();
                })
                .toList();

        List<EquipmentSearchResult> equipment = equipmentRepository.searchAvailable(query, pageable).stream()
                .map(this::toEquipmentResult)
                .toList();

        List<ProduceSearchResult> produce = produceBatchRepository.searchReadyForSale(query, pageable).stream()
                .map(this::toProduceResult)
                .toList();

        List<MarketplaceSearchResult> marketplace = marketplaceListingRepository.searchActive(query, pageable).stream()
                .map(this::toMarketplaceResult)
                .toList();

        int totalResults = people.size() + equipment.size() + produce.size() + marketplace.size();

        return SearchResponse.builder()
                .query(query)
                .people(people)
                .equipment(equipment)
                .produce(produce)
                .marketplace(marketplace)
                .totalResults(totalResults)
                .build();
    }

    private EquipmentSearchResult toEquipmentResult(Equipment equipment) {
        return EquipmentSearchResult.builder()
                .id(equipment.getId())
                .name(equipment.getName())
                .category(equipment.getCategory())
                .dailyRate(equipment.getDailyRate())
                .region(equipment.getRegion())
                .imageUrl(equipment.getImageUrl())
                .isAvailable(equipment.isAvailable())
                .ownerName(equipment.getOwner().getFullName())
                .ownerId(equipment.getOwner().getId())
                .build();
    }

    private ProduceSearchResult toProduceResult(ProduceBatch batch) {
        return ProduceSearchResult.builder()
                .id(batch.getId())
                .cropName(batch.getCropName())
                .variety(batch.getVariety())
                .quantityKg(batch.getQuantityKg())
                .region(batch.getRegion())
                .status(batch.getStatus())
                .farmerName(batch.getFarmer().getFullName())
                .farmerId(batch.getFarmer().getId())
                .build();
    }

    private MarketplaceSearchResult toMarketplaceResult(MarketplaceListing listing) {
        return MarketplaceSearchResult.builder()
                .id(listing.getId())
                .name(listing.getName())
                .category(listing.getCategory())
                .price(listing.getPrice())
                .priceType(listing.getPriceType())
                .region(listing.getRegion())
                .photoUrls(listing.getPhotoUrls())
                .sellerName(listing.getSeller().getFullName())
                .sellerId(listing.getSeller().getId())
                .build();
    }
}
