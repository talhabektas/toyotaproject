package com.example.mainapp.collector.factory;

import com.example.mainapp.collector.PlatformConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.util.Properties;

/**
 * Factory class for creating platform connectors dynamically
 */
@Component
public class ConnectorFactory {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorFactory.class);

    /**
     * Create a platform connector dynamically based on class name
     * @param platformName Platform name
     * @param connectorClassName Connector class name
     * @param config Configuration properties
     * @return Created connector or null if creation fails
     */
    public PlatformConnector createConnector(String platformName, String connectorClassName, Properties config) {
        try {
            logger.info("Creating connector for platform {} using class {}", platformName, connectorClassName);

            // Load the class
            Class<?> connectorClass = Class.forName(connectorClassName);

            // Check if the class implements PlatformConnector
            if (!PlatformConnector.class.isAssignableFrom(connectorClass)) {
                logger.error("Class {} does not implement PlatformConnector interface", connectorClassName);
                return null;
            }

            // Get constructor
            Constructor<?> constructor = connectorClass.getConstructor(String.class, Properties.class);

            // Create instance
            return (PlatformConnector) constructor.newInstance(platformName, config);

        } catch (ClassNotFoundException e) {
            logger.error("Connector class not found: {}", connectorClassName, e);
        } catch (NoSuchMethodException e) {
            logger.error("Connector class {} does not have required constructor", connectorClassName, e);
        } catch (Exception e) {
            logger.error("Failed to create connector for platform {}", platformName, e);
        }

        return null;
    }
}