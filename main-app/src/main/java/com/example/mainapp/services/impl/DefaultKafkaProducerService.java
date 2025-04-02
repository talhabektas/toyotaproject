package com.example.mainapp.services.impl;

import com.example.mainapp.model.Rate;
import com.example.mainapp.services.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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

            // Send to Kafka
            kafkaTemplate.send(ratesTopic, key, message)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.debug("Successfully sent rate {} to Kafka: {}", key, message);
                        } else {
                            logger.error("Failed to send rate {} to Kafka", key, ex);
                        }
                    });

        } catch (Exception e) {
            logger.error("Error sending rate to Kafka", e);
        }
    }
}