package com.agrochain.backend.dto;

import com.agrochain.backend.model.EquipmentCategory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties({"available"})
public class EquipmentResponse {

    private Long id;
    private String name;
    private EquipmentCategory category;
    private String description;
    private BigDecimal dailyRate;
    private String region;
    private String district;
    private String imageUrl;

    @JsonProperty("isAvailable")
    private boolean isAvailable;

    private String ownerName;
    private String ownerPhone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
