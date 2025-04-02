package com.example.kafkaconsumer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final RatePersistenceService ratePersistenceService;

    @Autowired
    public KafkaConsumerService(RatePersistenceService ratePersistenceService) {
        this.ratePersistenceService = ratePersistenceService;
    }

    @KafkaListener(topics = "${kafka.topic.rates}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        logger.info("Received message: key={}, payload={}", key, message);

        try {
            boolean success = ratePersistenceService.processMessage(message);
            if (success) {
                logger.info("Successfully processed message for key: {}", key);
            } else {
                logger.error("Failed to process message for key: {}", key);
            }
        } catch (Exception e) {
            logger.error("Error processing message: {}", message, e);
        }
    }
}