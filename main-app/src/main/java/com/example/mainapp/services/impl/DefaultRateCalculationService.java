package com.example.mainapp.services.impl;

import com.example.mainapp.collector.PlatformConnector;
import com.example.mainapp.collector.factory.ConnectorFactory;
import com.example.mainapp.coordinator.Coordinator;
import com.example.mainapp.services.RateCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Default implementation of RateCalculationService
 */
@Service
public class DefaultRateCalculationService implements RateCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRateCalculationService.class);

    private final Coordinator coordinator;
    private final ConnectorFactory connectorFactory;

    @Value("${app.platform-connectors-config}")
    private Resource platformConnectorsConfig;

    @Value("${app.platform-configs-directory}")
    private String platformConfigsDirectory;

    /**
     * Constructor
     * @param coordinator Coordinator
     * @param connectorFactory Connector factory
     */
    @Autowired
    public DefaultRateCalculationService(Coordinator coordinator, ConnectorFactory connectorFactory) {
        this.coordinator = coordinator;
        this.connectorFactory = connectorFactory;
    }

    /**
     * Initialize the service
     */
    @PostConstruct
    public void init() {
        loadConnectors();
    }

    /**
     * Load platform connectors from configuration
     */
    private void loadConnectors() {
        try (InputStream is = platformConnectorsConfig.getInputStream()) {
            Properties props = new Properties();
            props.load(is);

            for (String platformName : props.stringPropertyNames()) {
                String connectorClassName = props.getProperty(platformName);

                // Load platform-specific configuration
                Properties platformConfig = loadPlatformConfig(platformName);

                // Create connector
                PlatformConnector connector = connectorFactory.createConnector(
                        platformName, connectorClassName, platformConfig);

                if (connector != null) {
                    // Add to coordinator
                    coordinator.addConnector(connector);
                    logger.info("Added connector for platform {}: {}", platformName, connectorClassName);
                } else {
                    logger.error("Failed to create connector for platform {}", platformName);
                }
            }

        } catch (IOException e) {
            logger.error("Error loading platform connectors configuration", e);
        }
    }

    /**
     * Load platform-specific configuration
     * @param platformName Platform name
     * @return Platform configuration
     */
    private Properties loadPlatformConfig(String platformName) {
        Properties platformConfig = new Properties();
        String configFile = platformConfigsDirectory + "/" + platformName + ".properties";

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (is != null) {
                platformConfig.load(is);
                logger.info("Loaded configuration for platform {}", platformName);
            } else {
                logger.warn("Configuration file not found for platform {}: {}", platformName, configFile);
            }
        } catch (IOException e) {
            logger.error("Error loading configuration for platform {}", platformName, e);
        }

        return platformConfig;
    }

    @Override
    public void start() {
        coordinator.start();
        logger.info("Rate calculation service started");
    }

    @Override
    public void stop() {
        coordinator.stop();
        logger.info("Rate calculation service stopped");
    }

    /**
     * Clean up resources
     */
    @PreDestroy
    public void cleanup() {
        stop();
    }

    @Override
    public boolean subscribeRate(String rateName) {
        Set<String> platforms = coordinator.subscribeRate(rateName);
        return !platforms.isEmpty();
    }

    @Override
    public boolean subscribeRate(String platformName, String rateName) {
        return coordinator.subscribeRate(platformName, rateName);
    }

    @Override
    public boolean unsubscribeRate(String rateName) {
        Set<String> platforms = coordinator.unsubscribeRate(rateName);
        return !platforms.isEmpty();
    }

    @Override
    public boolean unsubscribeRate(String platformName, String rateName) {
        return coordinator.unsubscribeRate(platformName, rateName);
    }

    @Override
    public Set<String> getAllRateNames() {
        return coordinator.getAllRateNames();
    }

    @Override
    public Set<String> getAllPlatformNames() {
        return coordinator.getAllPlatformNames();
    }
}