package com.example.mainapp.coordinator;


import com.example.mainapp.collector.PlatformConnector;
import com.example.mainapp.model.Rate;

import java.util.List;
import java.util.Set;

/**
 * Interface defining the core functionality of the coordinator
 */
public interface Coordinator extends CoordinatorCallBack {

    /**
     * Starts the coordinator
     */
    void start();

    /**
     * Stops the coordinator
     */
    void stop();

    /**
     * Adds a platform connector
     * @param connector The platform connector
     */
    void addConnector(PlatformConnector connector);

    /**
     * Removes a platform connector
     * @param platformName The platform name
     * @return True if the operation was successful
     */
    boolean removeConnector(String platformName);

    /**
     * Returns all platform connectors
     * @return A list of platform connectors
     */
    List<PlatformConnector> getConnectors();

    /**
     * Subscribes to a rate on all platforms
     * @param rateName The rate name
     * @return A set of platform names where the subscription was successful
     */
    Set<String> subscribeRate(String rateName);

    /**
     * Subscribes to a rate on a specific platform
     * @param platformName The platform name
     * @param rateName The rate name
     * @return True if the subscription was successful
     */
    boolean subscribeRate(String platformName, String rateName);

    /**
     * Unsubscribes from a rate on all platforms
     * @param rateName The rate name
     * @return A set of platform names where the unsubscription was successful
     */
    Set<String> unsubscribeRate(String rateName);

    /**
     * Unsubscribes from a rate on a specific platform
     * @param platformName The platform name
     * @param rateName The rate name
     * @return True if the unsubscription was successful
     */
    boolean unsubscribeRate(String platformName, String rateName);

    /**
     * Returns a rate data from the cache
     * @param rateName The rate name
     * @return The rate data, or null if not found
     */
    Rate getRate(String rateName);

    /**
     * Returns a rate data from a specific platform
     * @param platformName The platform name
     * @param rateName The rate name
     * @return The rate data, or null if not found
     */
    Rate getRate(String platformName, String rateName);

    /**
     * Returns all available rate names
     * @return A set of rate names
     */
    Set<String> getAllRateNames();

    /**
     * Returns all available platform names
     * @return A set of platform names
     */
    Set<String> getAllPlatformNames();

    /**
     * Calculates a rate
     * @param targetRateName The target rate name
     * @return True if the calculation was successful
     */
    boolean calculateRate(String targetRateName);
}