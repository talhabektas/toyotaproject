package com.example.platformsimulatorrest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the REST platform simulator
 */
@SpringBootApplication
@EnableScheduling
public class PlatformSimulatorRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformSimulatorRestApplication.class, args);
    }

}