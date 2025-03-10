package com.example.mainapp.collector.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.mainapp.collector.DataCollector;
import com.example.mainapp.model.Rate;
import com.example.mainapp.model.RateFields;
import com.example.mainapp.model.RateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * REST platform connection manager class
 */
public class RestPlatformConnector extends DataCollector {

    private static final Logger logger = LoggerFactory.getLogger(RestPlatformConnector.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, Rate> lastRates = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private String baseUrl;
    private long pollingIntervalMs;

    /**
     * Constructor
     * @param platformName Platform name
     * @param config Platform configuration
     */
    public RestPlatformConnector(String platformName, Properties config) {
        super(platformName, config);
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // For Java 8 date/time support
    }

    @Override
    public boolean connect(String platformName, String userid, String password) {
        if (connected.get()) {
            logger.warn("Already connected to platform: {}", platformName);
            return true;
        }

        try {
            this.baseUrl = config.getProperty("rest.baseUrl", "http://localhost:8080/api/rates");
            this.pollingIntervalMs = Long.parseLong(config.getProperty("rest.pollingIntervalMs", "5000"));

            logger.info("Connecting to REST API {} for platform {}", baseUrl, platformName);

            // Test the connection with a request
            ResponseEntity<String> response = restTemplate.getForEntity(baseUrl, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                connected.set(true);

                if (callback != null) {
                    callback.onConnect(platformName, true);
                }

                logger.info("Connected to REST API for platform {}", platformName);
                return true;
            } else {
                logger.error("Failed to connect to REST API for platform {}: {}",
                        platformName, response.getStatusCode());

                if (callback != null) {
                    callback.onConnect(platformName, false);
                }

                return false;
            }
        } catch (Exception e) {
            logger.error("Failed to connect to platform: {}", platformName, e);

            if (callback != null) {
                callback.onConnect(platformName, false);
            }

            return false;
        }
    }

    @Override
    public boolean disconnect(String platformName, String userid, String password) {
        if (!connected.get()) {
            logger.warn("Not connected to platform: {}", platformName);
            return true;
        }

        // No actual disconnection for REST, just stop polling
        connected.set(false);

        if (callback != null) {
            callback.onDisConnect(platformName, true);
        }

        logger.info("Disconnected from platform: {}", platformName);
        return true;
    }

    @Override
    public boolean subscribe(String platformName, String rateName) {
        if (!connected.get()) {
            logger.error("Cannot subscribe - not connected to platform: {}", platformName);
            return false;
        }

        // Fetch initial data immediately
        boolean success = fetchAndProcessRate(rateName);
        handleSubscriptionResult(rateName, success);
        return success;
    }

    @Override
    public boolean unsubscribe(String platformName, String rateName) {
        if (!connected.get()) {
            logger.error("Cannot unsubscribe - not connected to platform: {}", platformName);
            return false;
        }

        handleUnsubscriptionResult(rateName, true);
        return true;
    }

    @Override
    public void run() {
        connect(platformName, null, null);

        if (connected.get()) {
            // Update all subscribed rates at regular intervals
            scheduler.scheduleAtFixedRate(() -> {
                if (!running.get() || !connected.get()) {
                    return;
                }

                // Take a copy (to avoid ConcurrentModificationException)
                Set<String> rates = new HashSet<>(subscribedRates);

                for (String rateName : rates) {
                    try {
                        fetchAndProcessRate(rateName);
                    } catch (Exception e) {
                        logger.error("Error fetching rate {} from platform {}", rateName, platformName, e);

                        if (callback != null) {
                            callback.onRateStatus(platformName, rateName, RateStatus.ERROR);
                        }
                    }
                }
            }, 0, pollingIntervalMs, TimeUnit.MILLISECONDS);

            // Keep main thread alive
            try {
                while (running.get() && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                logger.info("REST platform collector thread interrupted");
                Thread.currentThread().interrupt();
            } finally {
                cleanup();
            }
        }
    }

    /**
     * Clean up resources
     */
    private void cleanup() {
        scheduler.shutdownNow();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Scheduler did not terminate in time");
            }
        } catch (InterruptedException e) {
            logger.error("Scheduler shutdown interrupted", e);
            Thread.currentThread().interrupt();
        }

        disconnect(platformName, null, null);
    }

    /**
     * Fetch and process the specified rate data from REST API
     * @param rateName Rate name
     * @return true if successful
     */
    private boolean fetchAndProcessRate(String rateName) {
        try {
            String url = baseUrl + "/" + rateName;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> rateData = response.getBody();

                String responseRateName = (String) rateData.get("rateName");
                Double bid = (Double) rateData.get("bid");
                Double ask = (Double) rateData.get("ask");
                String timestampStr = (String) rateData.get("timestamp");

                // Parse timestamp
                LocalDateTime timestamp = LocalDateTime.parse(timestampStr);

                // Create rate object
                Rate rate = new Rate(responseRateName, platformName, bid, ask, timestamp, false);

                // Notify callback
                if (callback != null) {
                    Rate previousRate = lastRates.get(rateName);

                    if (previousRate == null) {
                        // First time data is available
                        callback.onRateAvailable(platformName, rateName, rate);
                        logger.debug("Rate available - {}: {}", rateName, rate);
                    } else {
                        // Update existing data
                        RateFields rateFields = new RateFields(bid, ask, timestamp);
                        callback.onRateUpdate(platformName, rateName, rateFields);
                        logger.debug("Rate update - {}: {}", rateName, rateFields);
                    }
                }

                // Store last rate
                lastRates.put(rateName, rate);

                return true;
            } else {
                logger.error("Failed to fetch rate {} from platform {}: {}",
                        rateName, platformName, response.getStatusCode());

                if (callback != null) {
                    callback.onRateStatus(platformName, rateName, RateStatus.UNAVAILABLE);
                }

                return false;
            }
        } catch (Exception e) {
            logger.error("Error fetching rate {} from platform {}", rateName, platformName, e);

            if (callback != null) {
                callback.onRateStatus(platformName, rateName, RateStatus.ERROR);
            }

            return false;
        }
    }
}
