package com.example.kafkaconsumer.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.example.kafkaconsumer.repository")
@EntityScan(basePackages = "com.example.kafkaconsumer.model")
public class DatabaseConfig {
    // Configuration is handled through Spring Boot auto-configuration and properties
}