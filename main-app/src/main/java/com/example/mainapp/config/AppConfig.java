package com.example.mainapp.config;

import com.example.mainapp.calculator.RateCalculator;
import com.example.mainapp.calculator.impl.JavaRateCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Application configuration
 */
@Configuration
public class AppConfig {

    /**
     * Configure object mapper with Java 8 time module
     * @return Configured object mapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    /**
     * Configure default rate calculator
     * @return Default rate calculator implementation
     */
    @Bean
    @Primary
    public RateCalculator rateCalculator() {
        return new JavaRateCalculator();
    }
}