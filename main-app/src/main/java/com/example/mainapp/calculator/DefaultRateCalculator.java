package com.example.mainapp.calculator;

import com.example.mainapp.calculator.RateCalculator;
import com.example.mainapp.model.Rate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of RateCalculator for cross rates
 */
@Component
public class DefaultRateCalculator implements RateCalculator {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRateCalculator.class);

    @Override
    public Rate calculate(String targetRateName, Map<String, Rate> dependencyRates) {
        logger.debug("Calculating rate for: {} with dependencies: {}", targetRateName, dependencyRates.keySet());

        try {
            // Implement cross rate calculations based on targetRateName
            if ("USDTRY".equals(targetRateName)) {
                return calculateUSDTRY(dependencyRates);
            } else if ("EURTRY".equals(targetRateName)) {
                return calculateEURTRY(dependencyRates);
            } else if ("GBPTRY".equals(targetRateName)) {
                return calculateGBPTRY(dependencyRates);
            } else {
                logger.warn("No calculation formula for rate: {}", targetRateName);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error calculating rate {}: {}", targetRateName, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean registerFormula(String rateName, String formula, String formulaType) {
        // This implementation uses hardcoded formulas
        logger.info("Formula registration not supported in DefaultRateCalculator");
        return false;
    }

    @Override
    public boolean unregisterFormula(String rateName) {
        // This implementation uses hardcoded formulas
        logger.info("Formula unregistration not supported in DefaultRateCalculator");
        return false;
    }

    private Rate calculateUSDTRY(Map<String, Rate> dependencyRates) {
        Rate pf1Rate = dependencyRates.get("PF1_USDTRY");
        Rate pf2Rate = dependencyRates.get("PF2_USDTRY");

        if (pf1Rate == null || pf2Rate == null) {
            logger.warn("Missing dependency rates for USDTRY calculation");
            return null;
        }

        // Simple average of platform rates
        double bid = (pf1Rate.getBid() + pf2Rate.getBid()) / 2;
        double ask = (pf1Rate.getAsk() + pf2Rate.getAsk()) / 2;

        Rate calculatedRate = new Rate();
        calculatedRate.setRateName("USDTRY");
        calculatedRate.setBid(bid);
        calculatedRate.setAsk(ask);
        calculatedRate.setTimestamp(LocalDateTime.now());
        calculatedRate.setCalculated(true);

        return calculatedRate;
    }

    private Rate calculateEURTRY(Map<String, Rate> dependencyRates) {
        Rate eurusd1 = dependencyRates.get("PF1_EURUSD");
        Rate eurusd2 = dependencyRates.get("PF2_EURUSD");
        Rate usdtry1 = dependencyRates.get("PF1_USDTRY");
        Rate usdtry2 = dependencyRates.get("PF2_USDTRY");

        if (eurusd1 == null || eurusd2 == null || usdtry1 == null || usdtry2 == null) {
            logger.warn("Missing dependency rates for EURTRY calculation");
            return null;
        }

        // Calculate EURTRY as EURUSD * USDTRY
        double eurusdBid = (eurusd1.getBid() + eurusd2.getBid()) / 2;
        double eurusdAsk = (eurusd1.getAsk() + eurusd2.getAsk()) / 2;
        double usdtryBid = (usdtry1.getBid() + usdtry2.getBid()) / 2;
        double usdtryAsk = (usdtry1.getAsk() + usdtry2.getAsk()) / 2;

        double bid = eurusdBid * usdtryBid;
        double ask = eurusdAsk * usdtryAsk;

        Rate calculatedRate = new Rate();
        calculatedRate.setRateName("EURTRY");
        calculatedRate.setBid(bid);
        calculatedRate.setAsk(ask);
        calculatedRate.setTimestamp(LocalDateTime.now());
        calculatedRate.setCalculated(true);

        return calculatedRate;
    }

    private Rate calculateGBPTRY(Map<String, Rate> dependencyRates) {
        Rate gbpusd1 = dependencyRates.get("PF1_GBPUSD");
        Rate gbpusd2 = dependencyRates.get("PF2_GBPUSD");
        Rate usdtry1 = dependencyRates.get("PF1_USDTRY");
        Rate usdtry2 = dependencyRates.get("PF2_USDTRY");

        if (gbpusd1 == null || gbpusd2 == null || usdtry1 == null || usdtry2 == null) {
            logger.warn("Missing dependency rates for GBPTRY calculation");
            return null;
        }

        // Calculate GBPTRY as GBPUSD * USDTRY
        double gbpusdBid = (gbpusd1.getBid() + gbpusd2.getBid()) / 2;
        double gbpusdAsk = (gbpusd1.getAsk() + gbpusd2.getAsk()) / 2;
        double usdtryBid = (usdtry1.getBid() + usdtry2.getBid()) / 2;
        double usdtryAsk = (usdtry1.getAsk() + usdtry2.getAsk()) / 2;

        double bid = gbpusdBid * usdtryBid;
        double ask = gbpusdAsk * usdtryAsk;

        Rate calculatedRate = new Rate();
        calculatedRate.setRateName("GBPTRY");
        calculatedRate.setBid(bid);
        calculatedRate.setAsk(ask);
        calculatedRate.setTimestamp(LocalDateTime.now());
        calculatedRate.setCalculated(true);

        return calculatedRate;
    }
}