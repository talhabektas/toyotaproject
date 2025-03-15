package com.example.fixsimulator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FIX simülator için konfigürasyon sınıfı
 */
@Configuration
public class FixSimulatorConfig {

    private static final Logger logger = LogManager.getLogger(FixSimulatorConfig.class);

    @Value("${simulation.updateIntervalMs:5000}")
    private long updateIntervalMs;

    @Value("${simulation.maxUpdates:-1}")
    private int maxUpdates;

    @Value("${simulation.minRateChange:-0.005}")
    private double minRateChange;

    @Value("${simulation.maxRateChange:0.005}")
    private double maxRateChange;

    @Value("${rates.config:config/rates-config.json}")
    private String ratesConfigPath;

    private Map<String, RateConfig> initialRates = new HashMap<>();

    @PostConstruct
    public void init() {
        loadRatesFromJson(ratesConfigPath);
    }

    public void loadRatesFromJson(String jsonFile) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(jsonFile)));
            ObjectMapper mapper = new ObjectMapper();
            CollectionType listType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, RateConfig.class);
            List<RateConfig> ratesList = mapper.readValue(content, listType);

            for (RateConfig rate : ratesList) {
                initialRates.put(rate.getRateName(), rate);
            }

            logger.info("Loaded {} initial rates from: {}", initialRates.size(), jsonFile);
        } catch (IOException e) {
            logger.error("Failed to load rates from JSON file: {}", jsonFile, e);
            throw new RuntimeException("Failed to load rates configuration", e);
        }
    }

    // Getter ve Setter metodları
    public long getUpdateIntervalMs() {
        return updateIntervalMs;
    }

    public void setUpdateIntervalMs(long updateIntervalMs) {
        this.updateIntervalMs = updateIntervalMs;
    }

    public int getMaxUpdates() {
        return maxUpdates;
    }

    public void setMaxUpdates(int maxUpdates) {
        this.maxUpdates = maxUpdates;
    }

    public double getMinRateChange() {
        return minRateChange;
    }

    public void setMinRateChange(double minRateChange) {
        this.minRateChange = minRateChange;
    }

    public double getMaxRateChange() {
        return maxRateChange;
    }

    public void setMaxRateChange(double maxRateChange) {
        this.maxRateChange = maxRateChange;
    }

    public Map<String, RateConfig> getInitialRates() {
        return initialRates;
    }

    public void setInitialRates(Map<String, RateConfig> initialRates) {
        this.initialRates = initialRates;
    }

    /**
     * Kur bilgilerini tutacak iç sınıf
     */
    public static class RateConfig {
        private String rateName;
        private double initialBid;
        private double initialAsk;

        public String getRateName() {
            return rateName;
        }

        public void setRateName(String rateName) {
            this.rateName = rateName;
        }

        public double getInitialBid() {
            return initialBid;
        }

        public void setInitialBid(double initialBid) {
            this.initialBid = initialBid;
        }

        public double getInitialAsk() {
            return initialAsk;
        }

        public void setInitialAsk(double initialAsk) {
            this.initialAsk = initialAsk;
        }
    }
}