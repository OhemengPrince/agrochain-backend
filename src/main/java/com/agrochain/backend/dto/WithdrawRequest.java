package com.agrochain.backend.dto;

import com.agrochain.backend.model.MomoNetwork;
import com.agrochain.backend.model.WithdrawalMethod;
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
public class WithdrawRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10.00", message = "Minimum withdrawal is GHS 10.00")
    private BigDecimal amount;

    @NotNull(message = "Method is required")
    private WithdrawalMethod method;

    // MoMo fields
    private MomoNetwork network;
    private String phoneNumber;

    // Bank fields
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountName;
}
