package com.example.kafkaconsumeropensearch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class AppConfig {
    // JacksonConfig sınıfı artık ObjectMapper'ı sağlıyor
}