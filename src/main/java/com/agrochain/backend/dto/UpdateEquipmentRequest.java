package com.agrochain.backend.dto;

import com.agrochain.backend.model.EquipmentCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
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

    @DecimalMin(value = "0.0", inclusive = false, message = "Daily rate must be greater than 0")
    private BigDecimal dailyRate;

    private String region;
    private String district;
    private String imageUrl;

    @JsonProperty("isAvailable")
    private Boolean isAvailable;
}
