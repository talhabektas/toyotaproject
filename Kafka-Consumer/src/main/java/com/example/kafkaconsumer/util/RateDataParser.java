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
     * @param message Message from Kafka
     * @return RateEntity or null if parsing fails
     */
    public static RateEntity parseMessage(String message) {
        if (message == null || message.isEmpty()) {
            logger.error("Cannot parse null or empty message");
            return null;
        }

        try {
            // Basit format: rateName|bid|ask|timestamp
            String[] parts = message.split("\\|");
            if (parts.length < 4) {
                logger.error("Invalid message format: {}", message);
                return null;
            }

            String rateName = parts[0];

            // Bid değerini parse et
            double bid;
            try {
                bid = Double.parseDouble(parts[1]);
            } catch (NumberFormatException e) {
                logger.error("Error parsing bid value: {}", parts[1], e);
                return null;
            }

            // Ask değerini parse et
            double ask;
            try {
                ask = Double.parseDouble(parts[2]);
            } catch (NumberFormatException e) {
                logger.error("Error parsing ask value: {}", parts[2], e);
                return null;
            }

            // Timestamp'i parse et
            LocalDateTime timestamp;
            try {
                timestamp = LocalDateTime.parse(parts[3]);
            } catch (DateTimeParseException e) {
                logger.error("Error parsing timestamp: {}", parts[3], e);
                // Veritabanı tutarlılığı için şimdiki zamanı kullan
                timestamp = LocalDateTime.now();
            }

            // Veritabanı güncelleme zamanını ayarla
            LocalDateTime dbUpdateTime = LocalDateTime.now();

            return new RateEntity(rateName, bid, ask, timestamp, dbUpdateTime);
        } catch (Exception e) {
            logger.error("Error parsing message: {}", message, e);
            return null;
        }
    }
}