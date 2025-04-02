package com.example.kafkaconsumer.service;

import com.example.kafkaconsumer.model.RateEntity;
import com.example.kafkaconsumer.repository.RateRepository;
import com.example.kafkaconsumer.util.RateDataParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatePersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(RatePersistenceService.class);

    private final RateRepository rateRepository;

    @Autowired
    public RatePersistenceService(RateRepository rateRepository) {
        this.rateRepository = rateRepository;
    }

    @Transactional
    public RateEntity saveRate(RateEntity rateEntity) {
        try {
            RateEntity saved = rateRepository.save(rateEntity);
            logger.debug("Saved rate: {}", saved);
            return saved;
        } catch (Exception e) {
            logger.error("Error saving rate: {}", rateEntity, e);
            throw e;
        }
    }

    @Transactional
    public boolean processMessage(String message) {
        try {
            logger.debug("Processing message: {}", message);
            RateEntity rateEntity = RateDataParser.parseMessage(message);
            if (rateEntity != null) {
                saveRate(rateEntity);
                return true;
            } else {
                logger.error("Failed to parse message: {}", message);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error processing message: {}", message, e);
            return false;
        }
    }
}