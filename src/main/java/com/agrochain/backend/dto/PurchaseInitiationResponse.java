package com.agrochain.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// paystackUrl is null for MoMo/bank direct charges (this project's Paystack
// integration triggers a USSD/OTP prompt on the buyer's device rather than a
// hosted checkout redirect) — kept for shape-compatibility with the spec.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseInitiationResponse {

    private Long purchaseId;
    private String paystackUrl;
    private BigDecimal totalAmount;
}
