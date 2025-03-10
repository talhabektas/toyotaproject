package com.example.mainapp.services;

import java.util.Set;

/**
 * Interface for rate calculation service
 */
public interface RateCalculationService {

    /**
     * Subscribe to a rate
     * @param rateName Rate name
     * @return true if successful
     */
    boolean subscribeRate(String rateName);

    /**
     * Subscribe to a rate on a specific platform
     * @param platformName Platform name
     * @param rateName Rate name
     * @return true if successful
     */
    boolean subscribeRate(String platformName, String rateName);

    /**
     * Unsubscribe from a rate
     * @param rateName Rate name
     * @return true if successful
     */
    boolean unsubscribeRate(String rateName);

    /**
     * Unsubscribe from a rate on a specific platform
     * @param platformName Platform name
     * @param rateName Rate name
     * @return true if successful
     */
    boolean unsubscribeRate(String platformName, String rateName);

    /**
     * Get all available rate names
     * @return Set of rate names
     */
    Set<String> getAllRateNames();

    /**
     * Get all available platform names
     * @return Set of platform names
     */
    Set<String> getAllPlatformNames();

    /**
     * Start the service
     */
    void start();

    /**
     * Stop the service
     */
    void stop();
}