package com.example.mainapp.cache.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.mainapp.cache.RateCache;
import com.example.mainapp.model.Rate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Redis implementation of RateCache
 */
@Component
@ConditionalOnProperty(name = "cache.type", havingValue = "redis")
public class RedisRateCache implements RateCache {

    private static final Logger logger = LoggerFactory.getLogger(RedisRateCache.class);

    private static final String RATE_KEY_PREFIX = "rate:";
    private static final String PLATFORM_RATE_KEY_PREFIX = "platform:";
    private static final String RATE_NAMES_KEY = "rate:names";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Constructor
     * @param redisTemplate Redis template
     * @param objectMapper Object mapper for serialization
     */
    @Autowired
    public RedisRateCache(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void putRate(Rate rate) {
        if (rate == null) {
            logger.warn("Attempted to cache null rate");
            return;
        }

        String rateName = rate.getRateName();
        String platformName = rate.getPlatformName();

        try {
            // Store rate object
            String rateKey = RATE_KEY_PREFIX + rateName;
            redisTemplate.opsForValue().set(rateKey, rate);

            // Store platform index
            if (platformName != null) {
                String platformKey = createPlatformKey(platformName, rateName);
                redisTemplate.opsForValue().set(platformKey, rateName);
            }

            // Add to set of rate names
            redisTemplate.opsForSet().add(RATE_NAMES_KEY, rateName);

            logger.debug("Cached rate in Redis: {}", rate);
        } catch (Exception e) {
            logger.error("Error storing rate in Redis: {}", rateName, e);
        }
    }

    @Override
    public Rate getRate(String rateName) {
        try {
            String rateKey = RATE_KEY_PREFIX + rateName;
            Rate rate = (Rate) redisTemplate.opsForValue().get(rateKey);

            if (rate == null) {
                logger.debug("Rate not found in Redis cache: {}", rateName);
            }

            return rate;
        } catch (Exception e) {
            logger.error("Error retrieving rate from Redis: {}", rateName, e);
            return null;
        }
    }

    @Override
    public Rate getRate(String platformName, String rateName) {
        try {
            String platformKey = createPlatformKey(platformName, rateName);
            String indexedRateName = (String) redisTemplate.opsForValue().get(platformKey);

            if (indexedRateName != null) {
                return getRate(indexedRateName);
            }

            logger.debug("Rate not found in Redis cache for platform {}: {}", platformName, rateName);
            return null;
        } catch (Exception e) {
            logger.error("Error retrieving rate from Redis for platform {}: {}", platformName, rateName, e);
            return null;
        }
    }

    @Override
    public boolean removeRate(String rateName) {
        try {
            String rateKey = RATE_KEY_PREFIX + rateName;

            // Get the rate first to get the platform name
            Rate rate = getRate(rateName);
            if (rate != null) {
                // Remove platform index
                String platformName = rate.getPlatformName();
                if (platformName != null) {
                    String platformKey = createPlatformKey(platformName, rateName);
                    redisTemplate.delete(platformKey);
                }

                // Remove from rate names set
                redisTemplate.opsForSet().remove(RATE_NAMES_KEY, rateName);

                // Remove the rate
                Boolean deleted = redisTemplate.delete(rateKey);

                logger.debug("Removed rate from Redis cache: {}", rateName);
                return deleted != null && deleted;
            }

            return false;
        } catch (Exception e) {
            logger.error("Error removing rate from Redis: {}", rateName, e);
            return false;
        }
    }

    @Override
    public boolean removeRate(String platformName, String rateName) {
        try {
            String platformKey = createPlatformKey(platformName, rateName);
            String indexedRateName = (String) redisTemplate.opsForValue().get(platformKey);

            if (indexedRateName != null) {
                // Delete platform index
                redisTemplate.delete(platformKey);

                // Delete rate
                String rateKey = RATE_KEY_PREFIX + indexedRateName;
                Boolean deleted = redisTemplate.delete(rateKey);

                // Remove from rate names set
                redisTemplate.opsForSet().remove(RATE_NAMES_KEY, indexedRateName);

                logger.debug("Removed rate from Redis cache for platform {}: {}", platformName, rateName);
                return deleted != null && deleted;
            }

            return false;
        } catch (Exception e) {
            logger.error("Error removing rate from Redis for platform {}: {}", platformName, rateName, e);
            return false;
        }
    }

    @Override
    public Set<String> getAllRateNames() {
        try {
            SetOperations<String, Object> setOps = redisTemplate.opsForSet();
            Set<Object> members = setOps.members(RATE_NAMES_KEY);

            if (members != null) {
                Set<String> rateNames = new HashSet<>();
                for (Object member : members) {
                    rateNames.add((String) member);
                }
                return rateNames;
            }

            return new HashSet<>();
        } catch (Exception e) {
            logger.error("Error retrieving all rate names from Redis", e);
            return new HashSet<>();
        }
    }

    @Override
    public void clearCache() {
        try {
            // Get all rate names
            Set<String> rateNames = getAllRateNames();

            // Delete each rate
            for (String rateName : rateNames) {
                removeRate(rateName);
            }

            // Delete the rate names set
            redisTemplate.delete(RATE_NAMES_KEY);

            logger.info("Redis cache cleared");
        } catch (Exception e) {
            logger.error("Error clearing Redis cache", e);
        }
    }

    /**
     * Create a platform-specific key for the index
     * @param platformName Platform name
     * @param rateName Rate name
     * @return Combined key
     */
    private String createPlatformKey(String platformName, String rateName) {
        return PLATFORM_RATE_KEY_PREFIX + platformName + ":" + rateName;
    }
}