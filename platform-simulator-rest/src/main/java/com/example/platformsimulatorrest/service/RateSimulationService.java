package com.example.platformsimulatorrest.service;

import com.example.platformsimulatorrest.config.SimulatorConfig;
import com.example.platformsimulatorrest.model.RateData;
import com.example.platformsimulatorrest.util.RandomRateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateSimulationService {
    private static final Logger logger = LoggerFactory.getLogger(RateSimulationService.class);

    private final SimulatorConfig config;
    private final RandomRateGenerator rateGenerator;
    private final Map<String, RateData> rateDataMap = new ConcurrentHashMap<>();

    private AtomicInteger updateCount = new AtomicInteger(0);
    private boolean running = false;

    @Autowired
    public RateSimulationService(SimulatorConfig config) {
        this.config = config;
        this.rateGenerator = new RandomRateGenerator(
                config.getMinRateChange(),
                config.getMaxRateChange());
        logger.info("RateSimulationService initialized with config: {}", config);
    }

    @PostConstruct
    public void init() {
        initializeRates();
        start();
    }

    @PreDestroy
    public void cleanup() {
        stop();
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

        running = true;
        logger.info("Rate simulation started with update interval: {}ms", config.getUpdateIntervalMs());
    }

    public synchronized void stop() {
        if (!running) {
            logger.warn("Simulation is not running");
            return;
        }

        running = false;
        logger.info("Rate simulation stopped");
    }

    @Scheduled(fixedDelayString = "${simulation.updateIntervalMs}")
    public void updateRates() {
        if (!running) {
            return;
        }

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
            logger.debug("Updated rate: {}", newRateData);
        });
    }

    public RateData getRateData(String rateName) {
        return rateDataMap.get(rateName);
    }

    public Map<String, RateData> getAllRates() {
        return new HashMap<>(rateDataMap);
    }
}