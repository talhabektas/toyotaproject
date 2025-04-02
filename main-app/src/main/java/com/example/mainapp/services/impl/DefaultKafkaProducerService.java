package com.example.mainapp.services.impl;

import com.example.mainapp.model.Rate;
import com.example.mainapp.services.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class DefaultKafkaProducerService implements KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultKafkaProducerService.class);

    @Value("${kafka.topic.rates}")
    private String ratesTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public DefaultKafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void sendRate(Rate rate) {
        if (rate == null) {
            logger.warn("Attempted to send null rate to Kafka");
            return;
        }

        try {
            // Convert rate to Kafka format
            String message = rate.toKafkaString();

            // Use rate name as key
            String key = rate.getRateName();

            logger.info("Sending rate to Kafka: key={}, message={}", key, message);

            // Send to Kafka with improved error handling
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(ratesTopic, key, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.debug("Successfully sent rate {} to Kafka: topic={}, partition={}, offset={}",
                            key, result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to send rate {} to Kafka: {}", key, ex.getMessage(), ex);
                }
            });

            // Optional: You can use the following to make the operation synchronous if needed
            // future.get(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            logger.error("Error sending rate to Kafka: {}", e.getMessage(), e);
        }
    }
}