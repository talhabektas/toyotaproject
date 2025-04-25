package com.example.mainapp.cache.impl;

import com.example.mainapp.cache.RateCache;
import com.example.mainapp.model.Rate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local in-memory implementation of RateCache using ConcurrentHashMap
 */
@Component
@ConditionalOnProperty(name = "cache.type", havingValue = "local") // Sadece cache.type=local olduğunda aktif olur
public class LocalMapRateCache implements RateCache {

    private static final Logger logger = LoggerFactory.getLogger(LocalMapRateCache.class);

    // Main cache structure: rateName -> Rate object
    private final Map<String, Rate> rateCache = new ConcurrentHashMap<>();

    // Secondary index: platformName:rateName -> rateName (for platform-specific lookups)
    private final Map<String, String> platformRateIndex = new ConcurrentHashMap<>();

    @Override
    public void putRate(Rate rate) {
        if (rate == null) {
            logger.warn("Attempted to cache null rate");
            return;
        }

        String rateName = rate.getRateName();
        String platformName = rate.getPlatformName();

        // Store in main cache
        rateCache.put(rateName, rate);

        // Update platform index
        if (platformName != null) {
            String platformKey = createPlatformKey(platformName, rateName);
            platformRateIndex.put(platformKey, rateName);
        }

        logger.debug("Cached rate: {}", rate);
    }

    @Override
    public Rate getRate(String rateName) {
        Rate rate = rateCache.get(rateName);

        if (rate == null) {
            logger.debug("Rate not found in cache: {}", rateName);
        }

        return rate;
    }

    @Override
    public Rate getRate(String platformName, String rateName) {
        String platformKey = createPlatformKey(platformName, rateName);
        String indexedRateName = platformRateIndex.get(platformKey);

        if (indexedRateName != null) {
            return rateCache.get(indexedRateName);
        }

        logger.debug("Rate not found in cache for platform {}: {}", platformName, rateName);
        return null;
    }

    @Override
    public boolean removeRate(String rateName) {
        Rate removed = rateCache.remove(rateName);

        if (removed != null) {
            // Also remove from platform index
            String platformName = removed.getPlatformName();
            if (platformName != null) {
                String platformKey = createPlatformKey(platformName, rateName);
                platformRateIndex.remove(platformKey);
            }

            logger.debug("Removed rate from cache: {}", rateName);
            return true;
        }

        return false;
    }

    @Override
    public boolean removeRate(String platformName, String rateName) {
        String platformKey = createPlatformKey(platformName, rateName);
        String indexedRateName = platformRateIndex.remove(platformKey);

        if (indexedRateName != null) {
            Rate removed = rateCache.remove(indexedRateName);

            if (removed != null) {
                logger.debug("Removed rate from cache for platform {}: {}", platformName, rateName);
                return true;
            }

            // Inconsistent state - add back to index
            platformRateIndex.put(platformKey, indexedRateName);
        }

        return false;
    }

    @Override
    public Set<String> getAllRateNames() {
        return new HashSet<>(rateCache.keySet());
    }

    @Override
    public void clearCache() {
        rateCache.clear();
        platformRateIndex.clear();
        logger.info("Cache cleared");
    }

    /**
     * Create a platform-specific key for the index
     * @param platformName Platform name
     * @param rateName Rate name
     * @return Combined key
     */
    private String createPlatformKey(String platformName, String rateName) {
        return platformName + ":" + rateName;
    }
}