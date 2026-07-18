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
public class MarketplacePurchaseResponse {

    private Long id;
    private Long listingId;
    private String listingName;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private Integer quantity;
    private BigDecimal unitPrice;
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
