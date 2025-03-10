package com.example.demo.config;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class SimulatorConfig {
    private static final Logger logger = LogManager.getLogger(SimulatorConfig.class);

    private int port;
    private int threadPoolSize;

    private long updateIntervalMs;
    private int maxUpdates;
    private double minRateChange;
    private double maxRateChange;

    private Map<String, RateConfig> initialRates = new HashMap<>();


    public void loadFromProperties(String propertiesFile) {
        Properties props = new Properties();

        try (InputStream input = new FileInputStream(propertiesFile)) {
            props.load(input);

            this.port = Integer.parseInt(props.getProperty("tcp.server.port", "8081"));
            this.threadPoolSize = Integer.parseInt(props.getProperty("tcp.server.threadPoolSize", "10"));

            this.updateIntervalMs = Long.parseLong(props.getProperty("simulation.updateIntervalMs", "8000"));
            this.maxUpdates = Integer.parseInt(props.getProperty("simulation.maxUpdates", "-1")); // -1 means unlimited
            this.minRateChange = Double.parseDouble(props.getProperty("simulation.minRateChange", "-0.02"));
            this.maxRateChange = Double.parseDouble(props.getProperty("simulation.maxRateChange", "0.02"));

            logger.info("Properties loaded from: {}", propertiesFile);
        } catch (IOException e) {
            logger.error("Failed to load properties file: {}", propertiesFile, e);
            throw new RuntimeException("Failed to load configuration", e);
        }
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

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