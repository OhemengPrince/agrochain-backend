package com.agrochain.backend.dto;

import com.agrochain.backend.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String paystackReference;
    private TransactionStatus status;
    private BigDecimal amount;
    private String momoNumber;
}
