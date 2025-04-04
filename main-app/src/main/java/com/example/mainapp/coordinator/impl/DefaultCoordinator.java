package com.example.mainapp.coordinator.impl;

import com.example.mainapp.cache.RateCache;
import com.example.mainapp.calculator.RateCalculator;
import com.example.mainapp.collector.PlatformConnector;
import com.example.mainapp.coordinator.Coordinator;
import com.example.mainapp.model.Rate;
import com.example.mainapp.model.RateFields;
import com.example.mainapp.model.RateStatus;
import com.example.mainapp.services.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DefaultCoordinator implements Coordinator {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCoordinator.class);
    private static final double TOLERANCE_THRESHOLD = 0.01; // %1 tolerans eşiği

    private final Map<String, PlatformConnector> connectors = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> calculatedRateDependencies = new ConcurrentHashMap<>();
    private final Map<String, Rate> lastRates = new ConcurrentHashMap<>(); // Son kur değerlerini tut

    private final RateCache rateCache;
    private final RateCalculator rateCalculator;
    private final KafkaProducerService kafkaProducerService;

    @Autowired
    public DefaultCoordinator(RateCache rateCache, RateCalculator rateCalculator,
                              KafkaProducerService kafkaProducerService) {
        this.rateCache = rateCache;
        this.rateCalculator = rateCalculator;
        this.kafkaProducerService = kafkaProducerService;

        initializeCalculatedRateDependencies();
    }

    private void initializeCalculatedRateDependencies() {
        // USDTRY dependencies: PF1_USDTRY and PF2_USDTRY
        Set<String> usdtryDeps = new HashSet<>();
        usdtryDeps.add("PF1_USDTRY");
        usdtryDeps.add("PF2_USDTRY");
        calculatedRateDependencies.put("USDTRY", usdtryDeps);

        // EURTRY dependencies: PF1_EURUSD, PF2_EURUSD, PF1_USDTRY, and PF2_USDTRY
        Set<String> eurtryDeps = new HashSet<>();
        eurtryDeps.add("PF1_EURUSD");
        eurtryDeps.add("PF2_EURUSD");
        eurtryDeps.add("PF1_USDTRY");
        eurtryDeps.add("PF2_USDTRY");
        calculatedRateDependencies.put("EURTRY", eurtryDeps);

        // GBPTRY dependencies: PF1_GBPUSD, PF2_GBPUSD, PF1_USDTRY, and PF2_USDTRY
        Set<String> gbptryDeps = new HashSet<>();
        gbptryDeps.add("PF1_GBPUSD");
        gbptryDeps.add("PF2_GBPUSD");
        gbptryDeps.add("PF1_USDTRY");
        gbptryDeps.add("PF2_USDTRY");
        calculatedRateDependencies.put("GBPTRY", gbptryDeps);
    }

    @Override
    public void start() {
        logger.info("Starting coordinator");
        connectors.values().forEach(PlatformConnector::start);
    }

    @Override
    public void stop() {
        logger.info("Stopping coordinator");
        connectors.values().forEach(PlatformConnector::stop);
    }

    @Override
    public void addConnector(PlatformConnector connector) {
        String platformName = connector.getPlatformName();
        logger.info("Adding connector for platform: {}", platformName);

        connector.setCallback(this);
        connectors.put(platformName, connector);
    }

    @Override
    public boolean removeConnector(String platformName) {
        logger.info("Removing connector for platform: {}", platformName);

        PlatformConnector connector = connectors.remove(platformName);
        if (connector != null) {
            connector.stop();
            return true;
        }
        return false;
    }

    @Override
    public List<PlatformConnector> getConnectors() {
        return new ArrayList<>(connectors.values());
    }

    @Override
    public Set<String> subscribeRate(String rateName) {
        logger.info("Subscribing to rate {} on all platforms", rateName);

        Set<String> successfulPlatforms = new HashSet<>();

        for (Map.Entry<String, PlatformConnector> entry : connectors.entrySet()) {
            String platformName = entry.getKey();
            PlatformConnector connector = entry.getValue();

            if (connector.subscribe(platformName, rateName)) {
                successfulPlatforms.add(platformName);
            }
        }

        return successfulPlatforms;
    }

    @Override
    public boolean subscribeRate(String platformName, String rateName) {
        logger.info("Subscribing to rate {} on platform {}", rateName, platformName);

        PlatformConnector connector = connectors.get(platformName);
        if (connector != null) {
            return connector.subscribe(platformName, rateName);
        }

        logger.warn("Platform {} not found", platformName);
        return false;
    }

    @Override
    public Set<String> unsubscribeRate(String rateName) {
        logger.info("Unsubscribing from rate {} on all platforms", rateName);

        Set<String> successfulPlatforms = new HashSet<>();

        for (Map.Entry<String, PlatformConnector> entry : connectors.entrySet()) {
            String platformName = entry.getKey();
            PlatformConnector connector = entry.getValue();

            if (connector.unsubscribe(platformName, rateName)) {
                successfulPlatforms.add(platformName);
            }
        }

        return successfulPlatforms;
    }

    @Override
    public boolean unsubscribeRate(String platformName, String rateName) {
        logger.info("Unsubscribing from rate {} on platform {}", rateName, platformName);

        PlatformConnector connector = connectors.get(platformName);
        if (connector != null) {
            return connector.unsubscribe(platformName, rateName);
        }

        logger.warn("Platform {} not found", platformName);
        return false;
    }

    @Override
    public Rate getRate(String rateName) {
        return rateCache.getRate(rateName);
    }

    @Override
    public Rate getRate(String platformName, String rateName) {
        return rateCache.getRate(platformName, rateName);
    }

    @Override
    public Set<String> getAllRateNames() {
        return rateCache.getAllRateNames();
    }

    @Override
    public Set<String> getAllPlatformNames() {
        return connectors.keySet();
    }

    @Override
    public boolean calculateRate(String targetRateName) {
        logger.info("Calculating rate: {}", targetRateName);

        Set<String> dependencies = calculatedRateDependencies.get(targetRateName);
        if (dependencies == null) {
            logger.warn("No dependencies defined for calculated rate: {}", targetRateName);
            return false;
        }

        // Check if all dependencies are available
        Map<String, Rate> dependencyRates = new HashMap<>();
        for (String depRateName : dependencies) {
            Rate depRate = rateCache.getRate(depRateName);
            if (depRate == null) {
                logger.warn("Dependency rate {} not available for calculating {}", depRateName, targetRateName);
                return false;
            }
            dependencyRates.put(depRateName, depRate);
        }

        try {
            // Calculate the rate
            Rate calculatedRate = rateCalculator.calculate(targetRateName, dependencyRates);
            if (calculatedRate != null) {
                // Veri temizleme - tolerans kontrolü
                Rate previousRate = lastRates.get(targetRateName);
                if (isWithinTolerance(calculatedRate, previousRate)) {
                    // Cache the calculated rate
                    rateCache.putRate(calculatedRate);
                    lastRates.put(targetRateName, calculatedRate);

                    // Send to Kafka
                    kafkaProducerService.sendRate(calculatedRate);
                    logger.info("Successfully calculated and sent rate to Kafka: {}", calculatedRate);

                    return true;
                } else {
                    logger.warn("Calculated rate exceeds tolerance threshold: {}", calculatedRate);
                    if (previousRate != null) {
                        logger.info("Using previous rate value: {}", previousRate);
                        return true;
                    }
                    // İlk hesaplama ise toleransı geçse bile kabul et
                    rateCache.putRate(calculatedRate);
                    lastRates.put(targetRateName, calculatedRate);
                    kafkaProducerService.sendRate(calculatedRate);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error calculating rate {}", targetRateName, e);
        }

        return false;
    }

    // Coordinator callback methods implementation
    @Override
    public void onConnect(String platformName, boolean status) {
        logger.info("Platform {} connection status: {}", platformName, status);
    }

    @Override
    public void onDisConnect(String platformName, boolean status) {
        logger.info("Platform {} disconnection status: {}", platformName, status);
    }

    @Override
    public void onRateAvailable(String platformName, String rateName, Rate rate) {
        logger.info("Rate {} available from platform {}", rateName, platformName);

        // Veri temizleme - tolerans kontrolü
        String fullRateName = platformName + "_" + rateName;
        Rate previousRate = lastRates.get(fullRateName);
        if (isWithinTolerance(rate, previousRate)) {
            // Add to cache
            rateCache.putRate(rate);
            lastRates.put(fullRateName, rate);

            // Send to Kafka
            kafkaProducerService.sendRate(rate);
            logger.debug("Sent rate to Kafka: {}", rate);

            // Check if this rate is a dependency for any calculated rates
            checkAndCalculateDependentRates(rateName);
        } else {
            logger.warn("Rate change exceeds tolerance threshold: {}", rate);
            if (previousRate != null) {
                logger.info("Using previous rate value: {}", previousRate);
            } else {
                // İlk veri ise toleransı geçse bile kabul et
                rateCache.putRate(rate);
                lastRates.put(fullRateName, rate);
                kafkaProducerService.sendRate(rate);
                checkAndCalculateDependentRates(rateName);
            }
        }
    }

    @Override
    public void onRateUpdate(String platformName, String rateName, RateFields rateFields) {
        logger.info("Rate {} updated from platform {}", rateName, platformName);

        // Get existing rate from cache
        Rate existingRate = rateCache.getRate(platformName, rateName);
        if (existingRate != null) {
            // Create updated rate
            Rate updatedRate = new Rate(
                    existingRate.getRateName(),
                    existingRate.getPlatformName(),
                    rateFields.getBid(),
                    rateFields.getAsk(),
                    rateFields.getTimestamp(),
                    existingRate.isCalculated()
            );

            // Veri temizleme - tolerans kontrolü
            String fullRateName = platformName + "_" + rateName;
            Rate previousRate = lastRates.get(fullRateName);
            if (isWithinTolerance(updatedRate, previousRate)) {
                // Update cache
                rateCache.putRate(updatedRate);
                lastRates.put(fullRateName, updatedRate);

                // Send to Kafka
                kafkaProducerService.sendRate(updatedRate);
                logger.debug("Sent updated rate to Kafka: {}", updatedRate);

                // Check if this rate is a dependency for any calculated rates
                checkAndCalculateDependentRates(rateName);
            } else {
                logger.warn("Rate update exceeds tolerance threshold: {}", updatedRate);
                if (previousRate != null) {
                    logger.info("Using previous rate value: {}", previousRate);
                }
            }
        } else {
            logger.warn("Received update for unknown rate {} from platform {}", rateName, platformName);
        }
    }

    @Override
    public void onRateStatus(String platformName, String rateName, RateStatus rateStatus) {
        logger.info("Rate {} status from platform {}: {}", rateName, platformName, rateStatus);

        if (rateStatus == RateStatus.UNAVAILABLE || rateStatus == RateStatus.ERROR) {
            // Remove from cache
            rateCache.removeRate(platformName, rateName);
        }
    }

    private void checkAndCalculateDependentRates(String updatedRateName) {
        for (Map.Entry<String, Set<String>> entry : calculatedRateDependencies.entrySet()) {
            String calculatedRateName = entry.getKey();
            Set<String> dependencies = entry.getValue();

            if (dependencies.contains(updatedRateName)) {
                logger.debug("Rate {} is a dependency for calculated rate {}, triggering calculation",
                        updatedRateName, calculatedRateName);
                calculateRate(calculatedRateName);
            }
        }
    }

    /**
     * Tolerans kontrolü - yeni değerin eski değerle karşılaştırılması
     * @param newRate Yeni kur değeri
     * @param previousRate Eski kur değeri
     * @return Tolerans içinde ise true
     */
    private boolean isWithinTolerance(Rate newRate, Rate previousRate) {
        if (previousRate == null) {
            return true; // İlk veri ise tolerans kontrolü yapma
        }

        double bidDiff = Math.abs((newRate.getBid() - previousRate.getBid()) / previousRate.getBid());
        double askDiff = Math.abs((newRate.getAsk() - previousRate.getAsk()) / previousRate.getAsk());

        boolean isValid = bidDiff <= TOLERANCE_THRESHOLD && askDiff <= TOLERANCE_THRESHOLD;

        if (!isValid) {
            logger.warn("Rate change exceeds tolerance limit: bid diff = {}%, ask diff = {}%",
                    bidDiff * 100, askDiff * 100);
        }

        return isValid;
    }
}