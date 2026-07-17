package com.agrochain.backend.dto;

import com.agrochain.backend.model.EarningsTransactionStatus;
import com.agrochain.backend.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {

    private Long id;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal agrochainFee;
    private BigDecimal netAmount;
    private EarningsTransactionStatus status;
    private String description;
    private String reference;
    private String counterpartyName;
    private String paymentMethod;
    private String paymentNumber;
    private String bankName;
    private LocalDateTime createdAt;
}
