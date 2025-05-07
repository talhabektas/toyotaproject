package com.example.platformsimulatorrest.controller;

import com.example.platformsimulatorrest.model.RateData;
import com.example.platformsimulatorrest.service.RateSimulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for rate data API endpoints
 */
@RestController
@RequestMapping("/api/rates")
public class RateController {
    private static final Logger logger = LoggerFactory.getLogger(RateController.class);

    private final RateSimulationService rateSimulationService;

    @Autowired
    public RateController(RateSimulationService rateSimulationService) {
        this.rateSimulationService = rateSimulationService;
    }

    /**
     * Get all available rates
     * @return Map of rate name to rate data
     */
    @GetMapping
    public ResponseEntity<Map<String, RateData>> getAllRates() {
        logger.info("Request received for all rates");
        Map<String, RateData> rates = rateSimulationService.getAllRates();
        return ResponseEntity.ok(rates);
    }

    /**
     * Get rate data for a specific rate
     * @param rateName Rate name
     * @return Rate data or 404 if not found
     */
    @GetMapping("/{rateName}")
    public ResponseEntity<RateData> getRate(@PathVariable String rateName) {
        logger.info("Request received for rate: {}", rateName);

        RateData rateData = rateSimulationService.getRateData(rateName);
        if (rateData == null) {
            logger.warn("Rate not found: {}", rateName);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(rateData);
    }

    /**
     * Control simulation (start/stop)
     * @param action Action to perform (start/stop)
     * @return Status message
     */
    @PostMapping("/simulation/{action}")
    public ResponseEntity<String> controlSimulation(@PathVariable String action) {
        logger.info("Simulation control action: {}", action);

        switch (action.toLowerCase()) {
            case "start":
                rateSimulationService.start();
                return ResponseEntity.ok("Simulation started");
            case "stop":
                rateSimulationService.stop();
                return ResponseEntity.ok("Simulation stopped");
            default:
                logger.warn("Invalid simulation action: {}", action);
                return ResponseEntity.badRequest().body("Invalid action. Use 'start' or 'stop'");
        }
    }
}