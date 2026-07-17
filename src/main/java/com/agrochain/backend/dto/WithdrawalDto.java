package com.agrochain.backend.dto;

import com.agrochain.backend.model.WithdrawalMethod;
import com.agrochain.backend.model.WithdrawalStatus;
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
public class WithdrawalDto {

    private Long id;
    private BigDecimal amount;
    private WithdrawalMethod method;
    private String network;
    private String phoneNumber;
    private String bankName;
    private String accountNumber;
    private String accountName;
    private WithdrawalStatus status;
    private LocalDateTime createdAt;
}
