package com.example.platformsimulatorrest.config;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Simülatör uygulaması için konfigürasyon sınıfı
 */
@Configuration
public class SimulatorConfig {
    private static final Logger logger = LoggerFactory.getLogger(SimulatorConfig.class);

    // Simülasyon ayarları
    @Value("${simulation.updateIntervalMs}")
    private long updateIntervalMs;

    @Value("${simulation.maxUpdates:#{-1}}")
    private int maxUpdates;

    @Value("${simulation.minRateChange}")
    private double minRateChange;

    @Value("${simulation.maxRateChange}")
    private double maxRateChange;

    // Kur konfigürasyon dosyası
    @Value("classpath:rates-config.json")
    private Resource ratesConfigResource;

    // Başlangıç kurları
    private Map<String, RateConfig> initialRates = new HashMap<>();

    /**
     * Uygulama başladığında çalışacak metot
     */
    @PostConstruct
    public void init() {
        loadRatesFromJson();
    }

    /**
     * JSON dosyasından başlangıç kur bilgilerini yükler
     */
    private void loadRatesFromJson() {
        try (InputStream inputStream = ratesConfigResource.getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            CollectionType listType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, RateConfig.class);
            List<RateConfig> ratesList = mapper.readValue(inputStream, listType);

            for (RateConfig rate : ratesList) {
                initialRates.put(rate.getRateName(), rate);
            }

            logger.info("Loaded {} initial rates from: {}", initialRates.size(), ratesConfigResource.getFilename());
        } catch (IOException e) {
            logger.error("Failed to load rates from JSON file: {}", ratesConfigResource.getFilename(), e);
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
     * İç sınıf: Kur konfigürasyonu
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