package com.agrochain.backend.dto;

import com.agrochain.backend.model.BookingStatus;
import com.agrochain.backend.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private Long farmerId;
    private String farmerName;
    private Long ownerId;
    private String ownerName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalCost;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
