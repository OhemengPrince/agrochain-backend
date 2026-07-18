package com.agrochain.backend.dto;

import com.agrochain.backend.model.MomoNetwork;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateMarketplacePurchaseRequest {

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    private String paymentMethod;

    private MomoNetwork network;
    private String phoneNumber;
    private String bankCode;
    private String accountNumber;
}
