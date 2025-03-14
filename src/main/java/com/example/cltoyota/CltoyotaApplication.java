package com.example.cltoyota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableKafka
public class CltoyotaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CltoyotaApplication.class, args);
    }

}
