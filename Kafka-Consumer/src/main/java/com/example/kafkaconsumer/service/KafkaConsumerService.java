package com.example.kafkaconsumer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Service for consuming Kafka messages
 */
@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final RatePersistenceService ratePersistenceService;

    /**
     * Constructor
     * @param ratePersistenceService Rate persistence service
     */
    @Autowired
    public KafkaConsumerService(RatePersistenceService ratePersistenceService) {
        this.ratePersistenceService = ratePersistenceService;
    }

    /**
     * Listen for messages on the rates topic
     * @param message Message payload
     */
    @KafkaListener(topics = "${kafka.topic.rates}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String message) {
        logger.debug("Received message: payload={}", message);

        boolean success = ratePersistenceService.processMessage(message);
        if (success) {
            logger.debug("Successfully processed message");
        } else {
            logger.error("Failed to process message");
        }
    }
}