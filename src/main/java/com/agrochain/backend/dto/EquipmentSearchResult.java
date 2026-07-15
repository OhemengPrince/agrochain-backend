package com.agrochain.backend.dto;

import com.agrochain.backend.model.EquipmentCategory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"available"})
public class EquipmentSearchResult {

    private Long id;
    private String name;
    private EquipmentCategory category;
    private BigDecimal dailyRate;
    private String region;
    private String imageUrl;

    @JsonProperty("isAvailable")
    private boolean isAvailable;

    private String ownerName;
    private Long ownerId;
}
