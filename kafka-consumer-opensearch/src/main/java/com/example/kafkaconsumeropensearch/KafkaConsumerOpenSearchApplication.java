package com.example.kafkaconsumeropensearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableKafka
@EnableRetry
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class KafkaConsumerOpenSearchApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaConsumerOpenSearchApplication.class, args);
	}
}