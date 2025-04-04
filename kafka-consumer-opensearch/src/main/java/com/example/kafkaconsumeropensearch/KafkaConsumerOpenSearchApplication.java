package com.example.kafkaconsumeropensearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.kafkaconsumeropensearch"})
public class KafkaConsumerOpenSearchApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaConsumerOpenSearchApplication.class, args);
	}
}
