package com.example.mainapp.services;

import com.example.mainapp.model.Rate;

/**
 * Kafka producer service interface
 */
public interface KafkaProducerService {

    /**
     * Send a rate to Kafka
     * @param rate Rate to send
     */
    void sendRate(Rate rate);
}