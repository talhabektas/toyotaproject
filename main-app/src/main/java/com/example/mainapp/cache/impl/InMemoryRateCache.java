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

@Component
@ConditionalOnProperty(name = "cache.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryRateCache implements RateCache {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryRateCache.class);

    private final Map<String, Rate> rateCache = new ConcurrentHashMap<>();
    private final Map<String, String> platformRateIndex = new ConcurrentHashMap<>();
    private final Set<String> rateNames = new HashSet<>();

    @Override
    public void putRate(Rate rate) {
        if (rate == null) {
            logger.warn("Attempted to cache null rate");
            return;
        }

        String rateName = rate.getRateName();
        String platformName = rate.getPlatformName();

        // Store rate object
        rateCache.put(rateName, rate);

        // Store platform index
        if (platformName != null) {
            String platformKey = platformName + ":" + rateName;
            platformRateIndex.put(platformKey, rateName);
        }

        // Add to set of rate names
        rateNames.add(rateName);

        logger.debug("Cached rate in memory: {}", rate);
    }

    @Override
    public Rate getRate(String rateName) {
        Rate rate = rateCache.get(rateName);
        if (rate == null) {
            logger.debug("Rate not found in memory cache: {}", rateName);
        }
        return rate;
    }

    @Override
    public Rate getRate(String platformName, String rateName) {
        String platformKey = platformName + ":" + rateName;
        String indexedRateName = platformRateIndex.get(platformKey);

        if (indexedRateName != null) {
            return getRate(indexedRateName);
        }

        logger.debug("Rate not found in memory cache for platform {}: {}", platformName, rateName);
        return null;
    }

    @Override
    public boolean removeRate(String rateName) {
        Rate rate = rateCache.remove(rateName);
        if (rate != null) {
            String platformName = rate.getPlatformName();
            if (platformName != null) {
                String platformKey = platformName + ":" + rateName;
                platformRateIndex.remove(platformKey);
            }
            rateNames.remove(rateName);
            logger.debug("Removed rate from memory cache: {}", rateName);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeRate(String platformName, String rateName) {
        String platformKey = platformName + ":" + rateName;
        String indexedRateName = platformRateIndex.remove(platformKey);

        if (indexedRateName != null) {
            rateCache.remove(indexedRateName);
            rateNames.remove(indexedRateName);
            logger.debug("Removed rate from memory cache for platform {}: {}", platformName, rateName);
            return true;
        }
        return false;
    }

    @Override
    public Set<String> getAllRateNames() {
        return new HashSet<>(rateNames);
    }

    @Override
    public void clearCache() {
        rateCache.clear();
        platformRateIndex.clear();
        rateNames.clear();
        logger.info("Memory cache cleared");
    }
}
