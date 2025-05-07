package com.example.mainapp.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration
 */
@Configuration
public class KafkaConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topic.rates}")
    private String ratesTopic;

    @Value("${kafka.topic.rates.partitions:3}")
    private int ratesTopicPartitions;

    @Value("${kafka.topic.rates.replication-factor:1}")
    private short ratesTopicReplicationFactor;

    /**
     * Configure Kafka admin client
     * @return Kafka admin
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * Configure rates topic
     * @return New topic configuration
     */
    @Bean

    public NewTopic ratesTopic() {
        try {
            return new NewTopic(ratesTopic, ratesTopicPartitions, ratesTopicReplicationFactor);
        } catch (Exception e) {
            // Topic muhtemelen zaten var, log'a kaydet ve devam et
            logger.warn("Topic oluşturulamadı {}, zaten var olabilir: {}", ratesTopic, e.getMessage());
            return null;
        }
    }

    /**
     * Configure Kafka producer factory
     * @return Producer factory
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "1"); // "all" yerine "1" kullanarak performansı artırabilirsiniz
        configProps.put(ProducerConfig.RETRIES_CONFIG, 10);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 500);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB buffer
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 60000); // 60 saniye bekle

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Configure Kafka template
     * @return Kafka template
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
