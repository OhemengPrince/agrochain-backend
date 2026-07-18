package com.agrochain.backend.dto;

import com.agrochain.backend.model.MomoNetwork;
import jakarta.validation.constraints.DecimalMin;
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
public class InitiateProducePurchaseRequest {

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal quantityKg;

    @NotNull
    private String paymentMethod;

    private MomoNetwork network;
    private String phoneNumber;
    private String bankCode;
    private String accountNumber;
}
