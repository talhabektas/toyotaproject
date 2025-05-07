package com.example.kafkaconsumeropensearch.util;

import com.example.kafkaconsumeropensearch.model.RateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Kur verilerini çözümlemek için yardımcı sınıf
 */
public class RateDataParser {
    private static final Logger logger = LoggerFactory.getLogger(RateDataParser.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private RateDataParser() {
        // Private constructor to prevent instantiation
    }

    /**
     * Kafka mesajından RateData nesnesi oluşturur
     * @param message Kafka mesajı (format: rateName|bid|ask|timestamp)
     * @return RateData nesnesi veya null (hata durumunda)
     */
    public static RateData parseMessage(String message) {
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
            double bid;
            double ask;
            LocalDateTime timestamp;

            try {
                bid = Double.parseDouble(parts[1]);
            } catch (NumberFormatException e) {
                logger.error("Bid değeri ayrıştırma hatası: {}", parts[1], e);
                return null;
            }

            try {
                ask = Double.parseDouble(parts[2]);
            } catch (NumberFormatException e) {
                logger.error("Ask değeri ayrıştırma hatası: {}", parts[2], e);
                return null;
            }

            try {
                String timestampStr = parts[3].trim();
                // Farklı tarih formatlarını destekleme
                if (timestampStr.contains(".")) {
                    // Milisaniye içeren format
                    timestamp = LocalDateTime.parse(timestampStr);
                } else {
                    // Milisaniye içermeyen format
                    timestamp = LocalDateTime.parse(timestampStr, FORMATTER);
                }
            } catch (DateTimeParseException e) {
                logger.error("Tarih ayrıştırma hatası: {}", parts[3], e);
                timestamp = LocalDateTime.now(); // Hata durumunda şu anki zamanı kullan
            }

            return new RateData(rateName, bid, ask, timestamp);
        } catch (Exception e) {
            logger.error("Mesaj ayrıştırma hatası: {}", message, e);
            return null;
        }
    }
}