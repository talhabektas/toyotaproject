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

        this.baseUrl = config.getProperty("rest.baseUrl", "http://rest-simulator:8080/api/rates");

        logger.info("RestPlatformConnector initialized for {} with baseUrl={}", platformName, this.baseUrl);
    }


    @Override
    public boolean connect(String platformName, String userid, String password) {
        if (connected.get()) {
            logger.warn("Already connected to platform: {}", platformName);
            return true;
        }

        // Bağlantı parametrelerini yükle
        String configuredUrl = config.getProperty("rest.baseUrl", "http://localhost:8080/api/rates");
        this.pollingIntervalMs = Long.parseLong(config.getProperty("rest.pollingIntervalMs", "5000"));
        int maxRetries = Integer.parseInt(config.getProperty("connection.retryCount", "5"));
        long retryInterval = Long.parseLong(config.getProperty("connection.retryIntervalMs", "5000"));

        logger.info("Attempting to connect to REST API {} for platform {}", configuredUrl, platformName);

        // Bağlantı deneme sayacı
        int attempts = 0;

        while (attempts < maxRetries && !connected.get()) {
            attempts++;

            try {
                // İlk olarak yapılandırılmış URL ile dene
                logger.info("Attempt {}/{}: Connecting to {}", attempts, maxRetries, configuredUrl);
                ResponseEntity<String> response = restTemplate.getForEntity(configuredUrl, String.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    this.baseUrl = configuredUrl;
                    connected.set(true);

                    if (callback != null) {
                        callback.onConnect(platformName, true);
                    }

                    logger.info("Successfully connected to REST API for platform {}", platformName);
                    return true;
                }
            } catch (Exception e) {
                logger.warn("Attempt {}/{}: Failed to connect to {} - {}",
                        attempts, maxRetries, configuredUrl, e.getMessage());

                // Farklı URL varyasyonları dene
                String[] alternativeUrls = {
                        "http://localhost:8080/api/rates",
                        "http://rest-simulator:8080/api/rates",
                        "http://platform-simulator-rest:8080/api/rates"
                };

                // Her alternatif URL'yi dene
                for (String altUrl : alternativeUrls) {
                    if (altUrl.equals(configuredUrl)) continue; // Zaten denenmiş

                    try {
                        logger.info("Trying alternative URL: {}", altUrl);
                        ResponseEntity<String> altResponse = restTemplate.getForEntity(altUrl, String.class);

                        if (altResponse.getStatusCode() == HttpStatus.OK) {
                            this.baseUrl = altUrl;
                            connected.set(true);

                            if (callback != null) {
                                callback.onConnect(platformName, true);
                            }

                            logger.info("Successfully connected using alternative URL: {}", altUrl);
                            return true;
                        }
                    } catch (Exception ignored) {
                        // Bu alternatif de başarısız oldu, bir sonrakine geç
                        logger.debug("Alternative URL failed: {}", altUrl);
                    }
                }
            }

            // Başarısız deneme sonrası bekle
            if (!connected.get() && attempts < maxRetries) {
                try {
                    logger.info("Waiting {} ms before next connection attempt...", retryInterval);
                    Thread.sleep(retryInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Tüm denemeler başarısız oldu
        logger.error("All connection attempts failed for platform: {}", platformName);

        if (callback != null) {
            callback.onConnect(platformName, false);
        }

        return false;
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
        String url = baseUrl + "/" + rateName;
        logger.info("Fetching rate data from URL: {}", url);

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> rateData = response.getBody();
                logger.debug("Received rate data: {}", rateData);

                String responseRateName = (String) rateData.get("rateName");
                if (responseRateName == null) {
                    logger.error("Rate data missing 'rateName' field: {}", rateData);
                    return false;
                }

                Double bid = (Double) rateData.get("bid");
                Double ask = (Double) rateData.get("ask");
                String timestampStr = (String) rateData.get("timestamp");

                // Parse timestamp
                LocalDateTime timestamp;
                try {
                    timestamp = LocalDateTime.parse(timestampStr);
                } catch (Exception e) {
                    logger.error("Error parsing timestamp '{}': {}", timestampStr, e.getMessage());
                    timestamp = LocalDateTime.now(); // Default to current time
                }

                // Create rate object
                Rate rate = new Rate(responseRateName, platformName, bid, ask, timestamp, false);
                logger.info("Created rate object: {}", rate);

                // Notify callback
                if (callback != null) {
                    Rate previousRate = lastRates.get(rateName);

                    if (previousRate == null) {
                        // First time data is available
                        callback.onRateAvailable(platformName, rateName, rate);
                        logger.info("Rate available - {}: {}", rateName, rate);
                    } else {
                        // Update existing data
                        RateFields rateFields = new RateFields(bid, ask, timestamp);
                        callback.onRateUpdate(platformName, rateName, rateFields);
                        logger.info("Rate update - {}: {}", rateName, rateFields);
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
            logger.error("Error fetching rate {} from platform {}: {}",
                    rateName, platformName, e.getMessage());

            if (callback != null) {
                callback.onRateStatus(platformName, rateName, RateStatus.ERROR);
            }
            return false;
        }
    }
}
