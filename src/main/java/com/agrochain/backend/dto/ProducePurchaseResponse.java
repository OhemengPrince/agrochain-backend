package com.agrochain.backend.dto;

import com.agrochain.backend.model.PurchaseStatus;
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
public class ProducePurchaseResponse {

    private Long id;
    private Long batchId;
    private String cropName;
    private Long buyerId;
    private String buyerName;
    private Long farmerId;
    private String farmerName;
    private BigDecimal quantityKg;
    private BigDecimal pricePerKg;
    private BigDecimal baseAmount;
    private BigDecimal totalAmount;
    private BigDecimal agrochainFee;
    private BigDecimal sellerNet;
    private PurchaseStatus status;
    private String paystackReference;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime buyerConfirmedAt;
    private LocalDateTime autoConfirmAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
