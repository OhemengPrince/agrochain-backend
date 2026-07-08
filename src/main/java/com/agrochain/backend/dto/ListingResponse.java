package com.agrochain.backend.dto;

import com.agrochain.backend.model.ContactPreference;
import com.agrochain.backend.model.ListingCategory;
import com.agrochain.backend.model.ListingStatus;
import com.agrochain.backend.model.PriceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingResponse {

    private Long id;
    private ListingCategory category;
    private String name;
    private String description;
    private PriceType priceType;
    private BigDecimal price;
    private String quantity;
    private String photoUrls;
    private String region;
    private String district;
    private ContactPreference contactPreference;
    private ListingStatus status;
    private Integer viewCount;
    private Long sellerId;
    private String sellerName;
    private String sellerPhone;
    private Double sellerRating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
