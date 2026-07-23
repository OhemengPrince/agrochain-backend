package com.agrochain.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyEarningsSummaryDto {

    // "YYYY-MM"
    private String month;
    private BigDecimal totalIncome;
    private BigDecimal totalFees;
    private BigDecimal totalWithdrawals;
}
