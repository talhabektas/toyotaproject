package com.example.demo.service;


import com.example.demo.config.SimulatorConfig;
import com.example.demo.model.RateData;
import com.example.demo.util.RandomRateGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class RateSimulationService {
    private static final Logger logger = LogManager.getLogger(RateSimulationService.class);

    private final SimulatorConfig config;
    private final RandomRateGenerator rateGenerator;
    private final Map<String, RateData> rateDataMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Consumer<RateData>>> subscribers = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;
    private AtomicInteger updateCount = new AtomicInteger(0);
    private boolean running = false;


    public RateSimulationService(SimulatorConfig config) {
        this.config = config;
        this.rateGenerator = new RandomRateGenerator(
                config.getMinRateChange(),
                config.getMaxRateChange());

        initializeRates();
    }


    private void initializeRates() {
        config.getInitialRates().forEach((rateName, rateConfig) -> {
            RateData rateData = new RateData(
                    rateName,
                    rateConfig.getInitialBid(),
                    rateConfig.getInitialAsk(),
                    LocalDateTime.now()
            );
            rateDataMap.put(rateName, rateData);
            logger.info("Initialized rate: {}", rateData);
        });
    }

    public synchronized void start() {
        if (running) {
            logger.warn("Simulation is already running");
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                this::updateRates,
                0,
                config.getUpdateIntervalMs(),
                TimeUnit.MILLISECONDS
        );

        running = true;
        logger.info("Rate simulation started with update interval: {}ms", config.getUpdateIntervalMs());
    }


    public synchronized void stop() {
        if (!running) {
            logger.warn("Simulation is not running");
            return;
        }

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        running = false;
        logger.info("Rate simulation stopped");
    }

    private void updateRates() {
        int currentCount = updateCount.incrementAndGet();
        int maxUpdates = config.getMaxUpdates();

        if (maxUpdates > 0 && currentCount > maxUpdates) {
            logger.info("Reached maximum update count ({}), stopping simulation", maxUpdates);
            stop();
            return;
        }

        rateDataMap.forEach((rateName, oldRateData) -> {
            RateData newRateData = rateGenerator.generateNextRate(oldRateData);
            rateDataMap.put(rateName, newRateData);

            notifySubscribers(rateName, newRateData);

            logger.debug("Updated rate: {}", newRateData);
        });
    }


    public boolean subscribe(String subscriberId, String rateName, Consumer<RateData> callback) {
        if (!rateDataMap.containsKey(rateName)) {
            logger.warn("Rate not found: {}", rateName);
            return false;
        }

        Map<String, Consumer<RateData>> subscriberMap = subscribers.computeIfAbsent(
                rateName, k -> new ConcurrentHashMap<>());

        subscriberMap.put(subscriberId, callback);

        callback.accept(rateDataMap.get(rateName));

        logger.info("Subscriber {} subscribed to rate {}", subscriberId, rateName);
        return true;
    }


    public boolean unsubscribe(String subscriberId, String rateName) {
        Map<String, Consumer<RateData>> subscriberMap = subscribers.get(rateName);
        if (subscriberMap != null) {
            subscriberMap.remove(subscriberId);
            logger.info("Subscriber {} unsubscribed from rate {}", subscriberId, rateName);
            return true;
        }
        return false;
    }
    public void unsubscribeAll(String subscriberId) {
        subscribers.forEach((rateName, subscriberMap) -> {
            if (subscriberMap.remove(subscriberId) != null) {
                logger.info("Subscriber {} unsubscribed from rate {}", subscriberId, rateName);
            }
        });
    }


    private void notifySubscribers(String rateName, RateData rateData) {
        Map<String, Consumer<RateData>> subscriberMap = subscribers.get(rateName);
        if (subscriberMap != null) {
            subscriberMap.forEach((subscriberId, callback) -> {
                try {
                    callback.accept(rateData);
                } catch (Exception e) {
                    logger.error("Error notifying subscriber {} for rate {}", subscriberId, rateName, e);
                }
            });
        }
    }


    public Map<String, RateData> getAllRates() {
        return new HashMap<>(rateDataMap);
    }


    public RateData getRateData(String rateName) {
        return rateDataMap.get(rateName);
    }
}