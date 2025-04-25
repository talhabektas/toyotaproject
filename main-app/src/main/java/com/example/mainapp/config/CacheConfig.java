package com.example.mainapp.config;

import com.example.mainapp.cache.RateCache;
import com.example.mainapp.cache.impl.InMemoryRateCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class CacheConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "cache.type", havingValue = "memory", matchIfMissing = true)
    public RateCache inMemoryRateCache() {
        return new InMemoryRateCache();
    }
}