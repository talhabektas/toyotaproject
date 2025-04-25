package com.example.mainapp;

import com.example.mainapp.services.RateCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class
 */
@SpringBootApplication
@EnableKafka
@EnableRetry
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class MainAppApplication {

    private static final Logger logger = LoggerFactory.getLogger(MainAppApplication.class);

    @Autowired
    private RateCalculationService rateCalculationService;

    /**
     * Application entry point
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MainAppApplication.class, args);
    }

    /**
     * Run when application starts
     * @return CommandLineRunner
     */
    @Bean
    public CommandLineRunner runner() {
        return args -> {
            logger.info("Starting financial data processing application");

            // Start rate calculation service
            rateCalculationService.start();

            // Subscribe to default rates
            subscribeDefaultRates();

            logger.info("Application startup completed");
        };
    }

    /**
     * Subscribe to default rates
     */
    private void subscribeDefaultRates() {
        // Subscribe to platform-specific rates (raw data)
        String[] defaultRates = {
                "PF1_USDTRY", "PF1_EURUSD", "PF1_GBPUSD",
                "PF2_USDTRY", "PF2_EURUSD", "PF2_GBPUSD"
        };

        for (String rate : defaultRates) {
            boolean success = rateCalculationService.subscribeRate(rate);
            if (success) {
                logger.info("Successfully subscribed to rate: {}", rate);
            } else {
                logger.warn("Failed to subscribe to rate: {}", rate);
            }
        }
    }
}