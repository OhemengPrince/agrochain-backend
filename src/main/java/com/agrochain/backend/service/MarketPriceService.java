package com.agrochain.backend.service;

import com.agrochain.backend.dto.MarketPriceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Market prices for the produce catalogue's price ticker.
 *
 * World Bank does not actually publish commodity prices (the "Pink Sheet")
 * through any REST/JSON API — only as Excel/PDF downloads at URLs that change
 * path yearly, and the specific indicator codes this was originally speced
 * against don't exist in their Indicators API. Until a real feed is wired up,
 * every crop here is a manually-maintained snapshot, the same as Tomato and
 * Cassava always were.
 *
 * The USD→GHS conversion and 1-hour cache are still real and exercised now,
 * so swapping CROP_SEEDS for a live fetch later is the only change needed.
 */
@Service
@Slf4j
public class MarketPriceService {

    private static final double USD_TO_GHS = 15.5;
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    // Manually maintained. usdPerTonne/previousUsdPerTonne are illustrative,
    // ballpark-realistic figures for the world commodity market (not scraped
    // from a live source) — update periodically alongside Tomato/Cassava.
    private record CommoditySeed(String crop, double usdPerTonne, double previousUsdPerTonne, double bagKg) {}

    private static final List<CommoditySeed> COMMODITY_SEEDS = List.of(
            new CommoditySeed("Maize", 195.0, 185.0, 100.0),
            new CommoditySeed("Cocoa", 8200.0, 8600.0, 64.0),
            new CommoditySeed("Rice", 480.0, 460.0, 50.0),
            new CommoditySeed("Groundnut", 1250.0, 1230.0, 100.0)
    );

    // Ghana-specific — no USD/World Bank figure exists for these at all, so
    // they're priced directly in GHS. Update manually.
    private record LocalSeed(String crop, double ghsPrice, double previousGhsPrice, String unit) {}

    private static final List<LocalSeed> LOCAL_SEEDS = List.of(
            new LocalSeed("Tomato", 180.0, 180.0, "crate"),
            new LocalSeed("Cassava", 120.0, 120.0, "bag")
    );

    private final AtomicReference<List<MarketPriceDto>> cache = new AtomicReference<>();
    private volatile LocalDateTime cachedAt;

    public List<MarketPriceDto> getPrices() {
        List<MarketPriceDto> current = cache.get();
        if (current != null && cachedAt != null && Duration.between(cachedAt, LocalDateTime.now()).compareTo(CACHE_TTL) < 0) {
            return current;
        }

        try {
            List<MarketPriceDto> fresh = computePrices();
            cache.set(fresh);
            cachedAt = LocalDateTime.now();
            return fresh;
        } catch (Exception e) {
            // Never let a computation/fetch failure surface to the frontend —
            // serve the last good cache, or the hardcoded defaults if we've
            // never successfully computed a list at all.
            log.error("Failed to compute market prices, falling back", e);
            List<MarketPriceDto> stale = cache.get();
            if (stale != null) {
                return stale;
            }
            return computePrices();
        }
    }

    private List<MarketPriceDto> computePrices() {
        String now = LocalDateTime.now().toString();
        List<MarketPriceDto> result = new ArrayList<>();

        for (CommoditySeed seed : COMMODITY_SEEDS) {
            double bagFraction = seed.bagKg() / 1000.0;
            double price = round(seed.usdPerTonne() * USD_TO_GHS * bagFraction);
            double previousPrice = seed.previousUsdPerTonne() * USD_TO_GHS * bagFraction;
            double change = previousPrice == 0 ? 0 : round((price - previousPrice) / previousPrice * 100);

            result.add(MarketPriceDto.builder()
                    .crop(seed.crop())
                    .price(price)
                    .unit("bag")
                    .currency("GHS")
                    .change(change)
                    .trend(trendFor(change))
                    .source("Manual")
                    .lastUpdated(now)
                    .build());
        }

        for (LocalSeed seed : LOCAL_SEEDS) {
            double change = seed.previousGhsPrice() == 0 ? 0
                    : round((seed.ghsPrice() - seed.previousGhsPrice()) / seed.previousGhsPrice() * 100);

            result.add(MarketPriceDto.builder()
                    .crop(seed.crop())
                    .price(seed.ghsPrice())
                    .unit(seed.unit())
                    .currency("GHS")
                    .change(change)
                    .trend(trendFor(change))
                    .source("Manual")
                    .lastUpdated(now)
                    .build());
        }

        return result;
    }

    private String trendFor(double change) {
        if (change > 0) return "UP";
        if (change < 0) return "DOWN";
        return "FLAT";
    }

    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
