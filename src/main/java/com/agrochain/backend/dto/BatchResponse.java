package com.agrochain.backend.dto;

import com.agrochain.backend.model.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponse {

    private Long id;
    private String cropName;
    private String variety;
    private BigDecimal quantityKg;
    private BigDecimal pricePerKg;
    private String region;
    private String district;
    private LocalDate plantedDate;
    private BatchStatus status;
    private String qrCodeValue;
    private String inputs;
    private Long farmerId;
    private String farmerName;
    private String farmerPhone;
    private List<BatchStageResponse> stages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
