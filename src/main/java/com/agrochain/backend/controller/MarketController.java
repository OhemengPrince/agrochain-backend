package com.agrochain.backend.controller;

import com.agrochain.backend.dto.MarketPriceDto;
import com.agrochain.backend.service.MarketPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketPriceService marketPriceService;

    @GetMapping("/prices")
    public ResponseEntity<List<MarketPriceDto>> getPrices() {
        return ResponseEntity.ok(marketPriceService.getPrices());
    }
}
