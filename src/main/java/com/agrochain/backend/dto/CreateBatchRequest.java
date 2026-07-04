package com.agrochain.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBatchRequest {

    @NotBlank(message = "Crop name is required")
    private String cropName;

    private String variety;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than 0")
    private BigDecimal quantityKg;

    @NotBlank(message = "Region is required")
    private String region;

    @NotBlank(message = "District is required")
    private String district;

    private LocalDate plantedDate;

    private String inputs;
}
