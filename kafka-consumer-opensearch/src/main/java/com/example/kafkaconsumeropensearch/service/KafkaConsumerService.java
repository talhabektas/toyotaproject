package com.example.kafkaconsumeropensearch.service;

import com.example.kafkaconsumeropensearch.model.RateData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka mesajlarını tüketen servis
 */
@Service
public class KafkaConsumerService {

    private static final Logger logger = LogManager.getLogger(KafkaConsumerService.class);

    private final OpenSearchService openSearchService;

    /**
     * Constructor
     * @param openSearchService OpenSearch servis
     */
    @Autowired
    public KafkaConsumerService(OpenSearchService openSearchService) {
        this.openSearchService = openSearchService;
    }

    /**
     * Kafka rates topiğini dinler
     * @param message Mesaj
     * @param key Mesaj anahtarı
     */
    @KafkaListener(topics = "${kafka.topic.rates}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        logger.debug("Kafka mesajı alındı: anahtar={}, mesaj={}", key, message);

        try {
            // Mesajı RateData nesnesine dönüştür
            RateData rateData = RateData.fromKafkaMessage(message);

            // OpenSearch'e kaydet
            openSearchService.indexRateData(rateData);

            logger.info("Kur verisi OpenSearch'e aktarıldı: {}", rateData.getRateName());
        } catch (Exception e) {
            logger.error("Kafka mesajı işlenirken hata: {}", message, e);
        }
    }
}