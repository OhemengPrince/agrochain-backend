package com.agrochain.backend.dto;

import com.agrochain.backend.model.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProduceSearchResult {

    private Long id;
    private String cropName;
    private String variety;
    private BigDecimal quantityKg;
    private String region;
    private BatchStatus status;
    private String farmerName;
    private Long farmerId;
}
