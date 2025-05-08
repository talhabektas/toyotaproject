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
            logger.warn("USDTRY hesaplaması için bağımlı kurlar eksik");
            return null;
        }

        double bid = (pf1Rate.getBid() + pf2Rate.getBid()) / 2;
        double ask = (pf1Rate.getAsk() + pf2Rate.getAsk()) / 2;

        bid = Math.round(bid * 100000.0) / 100000.0;
        ask = Math.round(ask * 100000.0) / 100000.0;

        Rate calculatedRate = new Rate();
        calculatedRate.setRateName("USDTRY");
        calculatedRate.setBid(bid);
        calculatedRate.setAsk(ask);
        calculatedRate.setTimestamp(LocalDateTime.now());
        calculatedRate.setCalculated(true);

        logger.info("USDTRY hesaplandı: bid={}, ask={}", bid, ask);
        return calculatedRate;
    }

    private Rate calculateEURTRY(Map<String, Rate> dependencyRates) {
        Rate pf1EurUsd = dependencyRates.get("PF1_EURUSD");
        Rate pf2EurUsd = dependencyRates.get("PF2_EURUSD");
        Rate pf1UsdTry = dependencyRates.get("PF1_USDTRY");
        Rate pf2UsdTry = dependencyRates.get("PF2_USDTRY");

        if (pf1EurUsd == null || pf2EurUsd == null || pf1UsdTry == null || pf2UsdTry == null) {
            logger.warn("EURTRY hesaplaması için bağımlı kurlar eksik");
            return null;
        }

        double usdMid = ((pf1UsdTry.getBid() + pf2UsdTry.getBid()) / 2 +
                (pf1UsdTry.getAsk() + pf2UsdTry.getAsk()) / 2) / 2;

        double eurUsdBid = (pf1EurUsd.getBid() + pf2EurUsd.getBid()) / 2;
        double eurUsdAsk = (pf1EurUsd.getAsk() + pf2EurUsd.getAsk()) / 2;

        double bid = usdMid * eurUsdBid;
        double ask = usdMid * eurUsdAsk;

        // Değerleri yuvarla
        bid = Math.round(bid * 100000.0) / 100000.0;
        ask = Math.round(ask * 100000.0) / 100000.0;

        Rate calculatedRate = new Rate();
        calculatedRate.setRateName("EURTRY");
        calculatedRate.setBid(bid);
        calculatedRate.setAsk(ask);
        calculatedRate.setTimestamp(LocalDateTime.now());
        calculatedRate.setCalculated(true);

        logger.info("EURTRY hesaplandı: bid={}, ask={}", bid, ask);
        return calculatedRate;
    }

    private Rate calculateGBPTRY(Map<String, Rate> dependencyRates) {
        Rate pf1GbpUsd = dependencyRates.get("PF1_GBPUSD");
        Rate pf2GbpUsd = dependencyRates.get("PF2_GBPUSD");
        Rate pf1UsdTry = dependencyRates.get("PF1_USDTRY");
        Rate pf2UsdTry = dependencyRates.get("PF2_USDTRY");

        if (pf1GbpUsd == null || pf2GbpUsd == null || pf1UsdTry == null || pf2UsdTry == null) {
            logger.warn("GBPTRY hesaplaması için bağımlı kurlar eksik");
            return null;
        }

        double usdMid = ((pf1UsdTry.getBid() + pf2UsdTry.getBid()) / 2 +
                (pf1UsdTry.getAsk() + pf2UsdTry.getAsk()) / 2) / 2;

        double gbpUsdBid = (pf1GbpUsd.getBid() + pf2GbpUsd.getBid()) / 2;
        double gbpUsdAsk = (pf1GbpUsd.getAsk() + pf2GbpUsd.getAsk()) / 2;

        double bid = usdMid * gbpUsdBid;
        double ask = usdMid * gbpUsdAsk;

        // Değerleri yuvarla
        bid = Math.round(bid * 100000.0) / 100000.0;
        ask = Math.round(ask * 100000.0) / 100000.0;

        Rate calculatedRate = new Rate();
        calculatedRate.setRateName("GBPTRY");
        calculatedRate.setBid(bid);
        calculatedRate.setAsk(ask);
        calculatedRate.setTimestamp(LocalDateTime.now());
        calculatedRate.setCalculated(true);

        logger.info("GBPTRY hesaplandı: bid={}, ask={}", bid, ask);
        return calculatedRate;
    }
}