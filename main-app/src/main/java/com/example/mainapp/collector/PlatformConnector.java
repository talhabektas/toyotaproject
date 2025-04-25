package com.example.mainapp.collector;

import com.example.mainapp.coordinator.CoordinatorCallBack;

/**
 * Common interface for managing platform connections
 */
public interface PlatformConnector {

    /**
     * Method used to establish a connection
     * @param platformName The platform name
     * @param userid The user ID
     * @param password The password
     * @return True if the connection was successful
     */
    boolean connect(String platformName, String userid, String password);

    /**
     * Method used to disconnect
     * @param platformName The platform name
     * @param userid The user ID
     * @param password The password
     * @return True if the operation was successful
     */
    boolean disconnect(String platformName, String userid, String password);

    /**
     * Method to be called to subscribe to a rate
     * @param platformName The platform name
     * @param rateName The rate name
     * @return True if the operation was successful
     */
    boolean subscribe(String platformName, String rateName);

    /**
     * Method to be called to end a rate subscription
     * @param platformName The platform name
     * @param rateName The rate name
     * @return True if the operation was successful
     */
    boolean unsubscribe(String platformName, String rateName);

    /**
     * Sets the coordinator callback
     * @param callback The coordinator callback
     */
    void setCallback(CoordinatorCallBack callback);

    /**
     * Returns the platform name
     * @return The platform name
     */
    String getPlatformName();

    /**
     * Starts the connector
     */
    void start();

    /**
     * Stops the connector
     */
    void stop();
}