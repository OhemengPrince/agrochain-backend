package com.agrochain.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    private String query;
    private List<PersonSearchResult> people;
    private List<EquipmentSearchResult> equipment;
    private List<ProduceSearchResult> produce;
    private List<MarketplaceSearchResult> marketplace;
    private int totalResults;
}
