package com.example.mainapp.services.impl;

import com.example.mainapp.collector.PlatformConnector;
import com.example.mainapp.collector.factory.ConnectorFactory;
import com.example.mainapp.coordinator.Coordinator;
import com.example.mainapp.services.RateCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;

@Service
public class DefaultRateCalculationService implements RateCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRateCalculationService.class);

    private final Coordinator coordinator;
    private final ConnectorFactory connectorFactory;

    @Value("${app.platform-connectors-config:classpath:connectors.properties}")
    private String platformConnectorsConfig;

    @Value("${app.platform-configs-directory:platform-configs}")
    private String platformConfigsDirectory;

    @Autowired
    public DefaultRateCalculationService(Coordinator coordinator, ConnectorFactory connectorFactory) {
        this.coordinator = coordinator;
        this.connectorFactory = connectorFactory;
    }

    @PostConstruct
    public void initialize() {
        loadPlatformConnectors();
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

    @Override
    public boolean  subscribeRate(String rateName) {
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

    /**
     * Load platform connectors from the configuration file and add them to the coordinator
     */
    private void loadPlatformConnectors() {
        Properties connectorsProps = new Properties();

        try {
            // Load the connectors properties
            File propsFile = new File(platformConnectorsConfig);
            try (InputStream inputStream = propsFile.exists()
                    ? new FileInputStream(propsFile)
                    : getClass().getClassLoader().getResourceAsStream("connectors.properties")) {

                if (inputStream == null) {
                    logger.error("Could not find connectors configuration file");
                    return;
                }

                connectorsProps.load(inputStream);
                logger.info("Loaded platform connectors configuration from: {}", platformConnectorsConfig);
            }

            // Create and add connectors
            for (String platformName : connectorsProps.stringPropertyNames()) {
                String connectorClassName = connectorsProps.getProperty(platformName);

                // Create platform-specific properties
                Properties platformProps = loadPlatformConfig(platformName);

                try {
                    // Create connector instance
                    PlatformConnector connector = connectorFactory.createConnector(platformName, connectorClassName, platformProps);

                    if (connector != null) {
                        // Add connector to coordinator
                        coordinator.addConnector(connector);
                        logger.info("Added connector for platform {}: {}", platformName, connectorClassName);
                    } else {
                        logger.error("Failed to create connector for platform {}: {}", platformName, connectorClassName);
                    }
                } catch (Exception e) {
                    logger.error("Error creating connector for platform {}: {}", platformName, e.getMessage(), e);
                }
            }

        } catch (IOException e) {
            logger.error("Error loading platform connectors configuration", e);
        }
    }

    /**
     * Load platform-specific configuration
     * @param platformName Platform name
     * @return Platform properties
     */
    private Properties loadPlatformConfig(String platformName) {
        Properties props = new Properties();

        // Try to load platform-specific properties file
        String configFileName = platformName.toLowerCase() + "-platform.properties";
        File configFile = Paths.get(platformConfigsDirectory, configFileName).toFile();

        try {
            if (configFile.exists()) {
                try (InputStream inputStream = new FileInputStream(configFile)) {
                    props.load(inputStream);
                    logger.info("Loaded platform configuration from: {}", configFile.getAbsolutePath());
                }
            } else {
                // Try to load from classpath
                try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("platform-configs/" + configFileName)) {
                    if (inputStream != null) {
                        props.load(inputStream);
                        logger.info("Loaded platform configuration from classpath: platform-configs/{}", configFileName);
                    } else {
                        logger.warn("No configuration found for platform: {}", platformName);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error loading configuration for platform: {}", platformName, e);
        }

        return props;
    }
}