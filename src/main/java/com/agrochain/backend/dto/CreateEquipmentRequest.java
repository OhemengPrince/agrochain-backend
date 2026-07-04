package com.agrochain.backend.dto;

import com.agrochain.backend.model.EquipmentCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEquipmentRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Category is required")
    private EquipmentCategory category;

    private String description;

    @NotNull(message = "Daily rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Daily rate must be greater than 0")
    private BigDecimal dailyRate;

    @NotBlank(message = "Region is required")
    private String region;

    @NotBlank(message = "District is required")
    private String district;

    private String imageUrl;
}
