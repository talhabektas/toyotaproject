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
// parseMessage metodu içinde:
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
            double bid = Double.parseDouble(parts[1]);
            double ask = Double.parseDouble(parts[2]);
            LocalDateTime timestamp;

            try {
                timestamp = LocalDateTime.parse(parts[3]);
            } catch (Exception e) {
                logger.error("Error parsing timestamp: {}", parts[3], e);
                timestamp = LocalDateTime.now(); // Geçici çözüm olarak şimdiki zamanı kullan
            }

            LocalDateTime dbUpdateTime = LocalDateTime.now();

            return new RateEntity(rateName, bid, ask, timestamp, dbUpdateTime);
        } catch (Exception e) {
            logger.error("Error parsing message: {}", message, e);
            return null;
        }
    }

    /**
     * Parse the TCP Simulator format
     * Format: PF1_USDTRY|22:number:34.15|25:number:37.06|5:timestamp:2025-03-12T23:59:21
     */
    private static RateEntity parseTcpSimulatorFormat(String message) {
        try {
            String[] parts = message.split("\\|");
            if (parts.length < 4) {
                logger.error("Invalid TCP simulator format, expected at least 4 parts: {}", message);
                return null;
            }

            String rateName = parts[0];

            // Parse bid value (22:number:34.15)
            String bidPart = parts[1];
            String[] bidSplit = bidPart.split(":");
            if (bidSplit.length < 3) {
                logger.error("Invalid bid format: {}", bidPart);
                return null;
            }
            double bid = Double.parseDouble(bidSplit[2]);

            // Parse ask value (25:number:37.06)
            String askPart = parts[2];
            String[] askSplit = askPart.split(":");
            if (askSplit.length < 3) {
                logger.error("Invalid ask format: {}", askPart);
                return null;
            }
            double ask = Double.parseDouble(askSplit[2]);

            // Parse timestamp (5:timestamp:2025-03-12T23:59:21)
            String timestampPart = parts[3];
            String[] timestampSplit = timestampPart.split(":");
            if (timestampSplit.length < 3) {
                logger.error("Invalid timestamp format: {}", timestampPart);
                return null;
            }
            // Join the remaining parts as they might contain colons in the ISO timestamp
            String timestampStr = String.join(":", timestampSplit).substring(timestampSplit[0].length() + timestampSplit[1].length() + 2);
            LocalDateTime rateUpdateTime = LocalDateTime.parse(timestampStr);
            LocalDateTime dbUpdateTime = LocalDateTime.now();

            return new RateEntity(rateName, bid, ask, rateUpdateTime, dbUpdateTime);
        } catch (NumberFormatException e) {
            logger.error("Error parsing numeric values in TCP simulator format: {}", message, e);
            return null;
        } catch (DateTimeParseException e) {
            logger.error("Error parsing timestamp in TCP simulator format: {}", message, e);
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error parsing TCP simulator format: {}", message, e);
            return null;
        }
    }

    /**
     * Parse the standard format
     * Format: PF1_USDTRY|34.15|37.06|2025-03-12T23:59:21
     */
    private static RateEntity parseStandardFormat(String message) {
        try {
            String[] parts = message.split("\\|");
            if (parts.length < 4) {
                logger.error("Invalid standard format, expected at least 4 parts: {}", message);
                return null;
            }

            String rateName = parts[0];
            double bid = Double.parseDouble(parts[1]);
            double ask = Double.parseDouble(parts[2]);
            LocalDateTime rateUpdateTime = LocalDateTime.parse(parts[3]);
            LocalDateTime dbUpdateTime = LocalDateTime.now();

            return new RateEntity(rateName, bid, ask, rateUpdateTime, dbUpdateTime);
        } catch (NumberFormatException e) {
            logger.error("Error parsing bid or ask values in standard format: {}", message, e);
            return null;
        } catch (DateTimeParseException e) {
            logger.error("Error parsing timestamp in standard format: {}", message, e);
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error parsing standard format: {}", message, e);
            return null;
        }
    }
}