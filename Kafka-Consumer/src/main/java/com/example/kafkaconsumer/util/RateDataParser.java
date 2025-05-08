package com.example.kafkaconsumer.util;

import com.example.kafkaconsumer.model.RateEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class for parsing rate data
 */
public class RateDataParser {
    private static final Logger logger = LoggerFactory.getLogger(RateDataParser.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Parse a Kafka message into a RateEntity
     *
     * @param message Message from Kafka
     * @return RateEntity or null if parsing fails
     */
    public static RateEntity parseMessage(String message) {
        if (message == null || message.isEmpty()) {
            logger.error("Boş mesaj ayrıştırılamaz");
            return null;
        }

        try {
            // Format: rateName|bid|ask|timestamp
            String[] parts = message.split("\\|");
            if (parts.length < 4) {
                logger.error("Geçersiz mesaj formatı: {}", message);
                return null;
            }

            String rateName = parts[0];
            double bid = Double.parseDouble(parts[1]);
            double ask = Double.parseDouble(parts[2]);
            LocalDateTime timestamp = LocalDateTime.parse(parts[3]);
            LocalDateTime dbUpdateTime = LocalDateTime.now();

            return new RateEntity(rateName, bid, ask, timestamp, dbUpdateTime);
        } catch (Exception e) {
            logger.error("Mesaj ayrıştırma hatası: {}", message, e);
            return null;
        }
    }
}