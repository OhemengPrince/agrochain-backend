package com.agrochain.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutoConfirmScheduler {

    private final MarketplacePurchaseService marketplacePurchaseService;
    private final ProducePurchaseService producePurchaseService;

    @Scheduled(fixedDelay = 3600000)
    public void autoConfirmDeliveries() {
        marketplacePurchaseService.autoConfirmDeliveries();
        producePurchaseService.autoConfirmDeliveries();
    }
}
