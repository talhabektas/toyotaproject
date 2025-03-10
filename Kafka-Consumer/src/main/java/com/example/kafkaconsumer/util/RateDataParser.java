package com.example.kafkaconsumer.util;

import com.example.kafkaconsumer.model.RateEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Utility class for parsing rate data
 */
public class RateDataParser {

    private static final Logger logger = LoggerFactory.getLogger(RateDataParser.class);

    private RateDataParser() {
        // Private constructor to prevent instantiation
    }

    /**
     * Parse a Kafka message into a RateEntity
     * @param message Message in format "rateName|bid|ask|timestamp"
     * @return RateEntity or null if parsing fails
     */
    public static RateEntity parseMessage(String message) {
        if (message == null || message.isEmpty()) {
            logger.error("Cannot parse null or empty message");
            return null;
        }

        try {
            String[] parts = message.split("\\|");
            if (parts.length < 4) {
                logger.error("Invalid message format, expected at least 4 parts: {}", message);
                return null;
            }

            String rateName = parts[0];
            double bid = Double.parseDouble(parts[1]);
            double ask = Double.parseDouble(parts[2]);
            LocalDateTime rateUpdateTime = LocalDateTime.parse(parts[3]);
            LocalDateTime dbUpdateTime = LocalDateTime.now();

            return new RateEntity(rateName, bid, ask, rateUpdateTime, dbUpdateTime);
        } catch (NumberFormatException e) {
            logger.error("Error parsing bid or ask values: {}", message, e);
        } catch (DateTimeParseException e) {
            logger.error("Error parsing timestamp: {}", message, e);
        } catch (Exception e) {
            logger.error("Unexpected error parsing message: {}", message, e);
        }

        return null;
    }
}