package com.agrochain.backend.dto;

import com.agrochain.backend.model.ListingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateListingStatusRequest {

    @NotNull(message = "Status is required")
    private ListingStatus status;
}
