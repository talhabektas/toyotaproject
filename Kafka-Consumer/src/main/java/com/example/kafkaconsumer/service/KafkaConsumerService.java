package com.example.kafkaconsumer.service;

import com.example.kafkaconsumer.model.RateEntity;
import com.example.kafkaconsumer.util.RateDataParser;
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
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset) {

        logger.info("Received message: key={}, topic={}, partition={}, offset={}, payload={}",
                key, topic, partition, offset, message);

        try {
            // Parse the message into an entity
            RateEntity rateEntity = RateDataParser.parseMessage(message);

            if (rateEntity != null) {
                // Save to database
                boolean success = ratePersistenceService.processMessage(message);

                if (success) {
                    logger.info("Successfully processed and saved message for key: {}", key);
                } else {
                    logger.error("Failed to process message for key: {}", key);
                }
            } else {
                logger.error("Failed to parse message: {}", message);
            }
        } catch (Exception e) {
            logger.error("Error processing message: {}", message, e);
        }
    }
}