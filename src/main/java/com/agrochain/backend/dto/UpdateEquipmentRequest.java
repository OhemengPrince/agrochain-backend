package com.agrochain.backend.dto;

import com.agrochain.backend.model.EquipmentCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEquipmentRequest {

    private String name;
    private EquipmentCategory category;
    private String description;
    private BigDecimal dailyRate;
    private String region;
    private String district;
    private String imageUrl;
    private Boolean isAvailable;
}
