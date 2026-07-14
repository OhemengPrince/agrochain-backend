package com.agrochain.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketPriceDto {

    private String crop;
    private double price;
    private String unit;
    private String currency;
    private double change;
    private String trend;
    private String source;
    private String lastUpdated;
}
