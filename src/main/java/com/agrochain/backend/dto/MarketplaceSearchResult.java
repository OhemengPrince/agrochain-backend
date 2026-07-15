package com.agrochain.backend.dto;

import com.agrochain.backend.model.ListingCategory;
import com.agrochain.backend.model.PriceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceSearchResult {

    private Long id;
    private String name;
    private ListingCategory category;
    private BigDecimal price;
    private PriceType priceType;
    private String region;
    private String photoUrls;
    private String sellerName;
    private Long sellerId;
}
