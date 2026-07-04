package com.agrochain.backend.dto;

import com.agrochain.backend.model.ContactPreference;
import com.agrochain.backend.model.ListingCategory;
import com.agrochain.backend.model.PriceType;
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
public class CreateListingRequest {

    @NotNull(message = "Category is required")
    private ListingCategory category;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Price type is required")
    private PriceType priceType;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    private String quantity;

    private String photoUrls;

    @NotBlank(message = "Region is required")
    private String region;

    @NotBlank(message = "District is required")
    private String district;

    @NotNull(message = "Contact preference is required")
    private ContactPreference contactPreference;
}
