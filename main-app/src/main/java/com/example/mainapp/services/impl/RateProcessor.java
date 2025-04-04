package com.example.mainapp.services.impl;

import com.example.mainapp.coordinator.Coordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Rate calculation processor that periodically calculates derived rates
 */
@Service
@EnableScheduling
public class RateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RateProcessor.class);

    private final Coordinator coordinator;

    @Autowired
    public RateProcessor(Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Periodically calculate all derived rates based on raw data
     */
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void calculateRates() {
        logger.debug("Calculating derived rates");

        // Get available raw rates
        Set<String> rawRateNames = coordinator.getAllRateNames();

        // Check if all required dependencies are available
        boolean hasUsdTryDependencies = rawRateNames.contains("PF1_USDTRY") && rawRateNames.contains("PF2_USDTRY");
        boolean hasEurUsdDependencies = rawRateNames.contains("PF1_EURUSD") && rawRateNames.contains("PF2_EURUSD");
        boolean hasGbpUsdDependencies = rawRateNames.contains("PF1_GBPUSD") && rawRateNames.contains("PF2_GBPUSD");

        // Calculate USD/TRY
        if (hasUsdTryDependencies) {
            boolean success = coordinator.calculateRate("USDTRY");
            logger.info("USDTRY calculation " + (success ? "successful" : "failed"));
        }

        // Calculate EUR/TRY
        if (hasUsdTryDependencies && hasEurUsdDependencies) {
            boolean success = coordinator.calculateRate("EURTRY");
            logger.info("EURTRY calculation " + (success ? "successful" : "failed"));
        }

        // Calculate GBP/TRY
        if (hasUsdTryDependencies && hasGbpUsdDependencies) {
            boolean success = coordinator.calculateRate("GBPTRY");
            logger.info("GBPTRY calculation " + (success ? "successful" : "failed"));
        }
    }
}