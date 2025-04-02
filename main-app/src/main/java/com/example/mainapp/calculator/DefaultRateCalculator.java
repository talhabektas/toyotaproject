package com.example.mainapp.calculator;

import com.example.mainapp.calculator.factory.CalculatorFactory;
import com.example.mainapp.calculator.impl.GroovyRateCalculator;
import com.example.mainapp.calculator.impl.JavaRateCalculator;
import com.example.mainapp.calculator.impl.JavaScriptRateCalculator;
import com.example.mainapp.model.Rate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RateCalculator arayüzünün varsayılan uygulaması
 */
@Component
@DependsOn("calculatorFactory")
public class DefaultRateCalculator implements RateCalculator {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRateCalculator.class);

    private final CalculatorFactory calculatorFactory;

    // Kur adı -> formül tipi ve formül eşlemesi
    private final Map<String, FormulaInfo> formulas = new ConcurrentHashMap<>();

    /**
     * Constructor
     * @param calculatorFactory Hesaplayıcı fabrikası
     */
    @Autowired
    public DefaultRateCalculator(CalculatorFactory calculatorFactory) {
        this.calculatorFactory = calculatorFactory;

        // Varsayılan formülleri kaydet
        registerDefaultFormulas();
    }

    /**
     * Varsayılan hesaplama formüllerini kaydeder
     */
    private void registerDefaultFormulas() {
        // USDTRY hesaplama formülü (PF1_USDTRY ve PF2_USDTRY ortalaması)
        String usdtryFormula = "function calculate(dependencies) {\n" +
                "    var pf1 = dependencies['PF1_USDTRY'];\n" +
                "    var pf2 = dependencies['PF2_USDTRY'];\n" +
                "    \n" +
                "    var bid = (pf1.bid + pf2.bid) / 2;\n" +
                "    var ask = (pf1.ask + pf2.ask) / 2;\n" +
                "    \n" +
                "    return {\n" +
                "        bid: bid,\n" +
                "        ask: ask\n" +
                "    };\n" +
                "}";

        registerFormula("USDTRY", usdtryFormula, "javascript");

        // EURTRY hesaplama formülü
        String eurtryFormula = "function calculate(dependencies) {\n" +
                "    var pf1_eurusd = dependencies['PF1_EURUSD'];\n" +
                "    var pf2_eurusd = dependencies['PF2_EURUSD'];\n" +
                "    var pf1_usdtry = dependencies['PF1_USDTRY'];\n" +
                "    var pf2_usdtry = dependencies['PF2_USDTRY'];\n" +
                "    \n" +
                "    // USD orta fiyatını hesapla\n" +
                "    var usdmid = (((pf1_usdtry.bid + pf2_usdtry.bid) / 2) + ((pf1_usdtry.ask + pf2_usdtry.ask) / 2)) / 2;\n" +
                "    \n" +
                "    // EUR/USD ortalamasını hesapla\n" +
                "    var eurusd_bid_avg = (pf1_eurusd.bid + pf2_eurusd.bid) / 2;\n" +
                "    var eurusd_ask_avg = (pf1_eurusd.ask + pf2_eurusd.ask) / 2;\n" +
                "    \n" +
                "    // EUR/TRY hesapla\n" +
                "    var bid = usdmid * eurusd_bid_avg;\n" +
                "    var ask = usdmid * eurusd_ask_avg;\n" +
                "    \n" +
                "    return {\n" +
                "        bid: bid,\n" +
                "        ask: ask\n" +
                "    };\n" +
                "}";

        registerFormula("EURTRY", eurtryFormula, "javascript");

        // GBPTRY hesaplama formülü
        String gbptryFormula = "function calculate(dependencies) {\n" +
                "    var pf1_gbpusd = dependencies['PF1_GBPUSD'];\n" +
                "    var pf2_gbpusd = dependencies['PF2_GBPUSD'];\n" +
                "    var pf1_usdtry = dependencies['PF1_USDTRY'];\n" +
                "    var pf2_usdtry = dependencies['PF2_USDTRY'];\n" +
                "    \n" +
                "    // USD orta fiyatını hesapla\n" +
                "    var usdmid = (((pf1_usdtry.bid + pf2_usdtry.bid) / 2) + ((pf1_usdtry.ask + pf2_usdtry.ask) / 2)) / 2;\n" +
                "    \n" +
                "    // GBP/USD ortalamasını hesapla\n" +
                "    var gbpusd_bid_avg = (pf1_gbpusd.bid + pf2_gbpusd.bid) / 2;\n" +
                "    var gbpusd_ask_avg = (pf1_gbpusd.ask + pf2_gbpusd.ask) / 2;\n" +
                "    \n" +
                "    // GBP/TRY hesapla\n" +
                "    var bid = usdmid * gbpusd_bid_avg;\n" +
                "    var ask = usdmid * gbpusd_ask_avg;\n" +
                "    \n" +
                "    return {\n" +
                "        bid: bid,\n" +
                "        ask: ask\n" +
                "    };\n" +
                "}";

        registerFormula("GBPTRY", gbptryFormula, "javascript");
    }

    @Override
    public Rate calculate(String targetRateName, Map<String, Rate> dependencyRates) {
        logger.debug("Calculating rate {} with {} dependencies", targetRateName, dependencyRates.size());

        // Formül bilgisini al
        FormulaInfo formulaInfo = formulas.get(targetRateName);
        if (formulaInfo == null) {
            logger.error("No formula registered for rate: {}", targetRateName);
            return null;
        }

        try {
            // Formül tipi için hesaplayıcı al
            Object calculator = calculatorFactory.getCalculator(formulaInfo.formulaType);
            if (calculator == null) {
                logger.error("No calculator found for formula type: {}", formulaInfo.formulaType);
                return null;
            }

            // Hesaplama bağlamı oluştur
            Map<String, Object> context = new HashMap<>();
            context.put("targetRateName", targetRateName);
            context.put("dependencies", dependencyRates);
            context.put("formula", formulaInfo.formula);

            // Formül tipine göre hesapla
            Map<String, Object> result;

            switch (formulaInfo.formulaType.toLowerCase()) {
                case "java":
                    result = ((JavaRateCalculator) calculator).executeFormula(context);
                    break;

                case "javascript":
                    result = ((JavaScriptRateCalculator) calculator).executeFormula(context);
                    break;

                case "groovy":
                    result = ((GroovyRateCalculator) calculator).executeFormula(context);
                    break;

                default:
                    logger.error("Unsupported formula type: {}", formulaInfo.formulaType);
                    return null;
            }

            if (result != null) {
                // Sonuçtan bid ve ask değerlerini çıkar
                double bid = ((Number) result.get("bid")).doubleValue();
                double ask = ((Number) result.get("ask")).doubleValue();

                // Hesaplanan kur oluştur
                Rate calculatedRate = new Rate(
                        targetRateName,
                        "CALCULATED",
                        bid,
                        ask,
                        LocalDateTime.now(),
                        true
                );

                logger.debug("Calculated rate {}: {}", targetRateName, calculatedRate);
                return calculatedRate;
            } else {
                logger.error("Calculation returned null result for rate: {}", targetRateName);
                return null;
            }

        } catch (Exception e) {
            logger.error("Error calculating rate {}", targetRateName, e);
            return null;
        }
    }

    @Override
    public boolean registerFormula(String rateName, String formula, String formulaType) {
        if (rateName == null || formula == null || formulaType == null) {
            logger.error("Invalid arguments for formula registration");
            return false;
        }

        // Formül tipi için hesaplayıcı var mı kontrol et
        Object calculator = calculatorFactory.getCalculator(formulaType);
        if (calculator == null) {
            logger.error("No calculator found for formula type: {}", formulaType);
            return false;
        }

        // Formülü kaydet
        formulas.put(rateName, new FormulaInfo(formula, formulaType));
        logger.info("Registered {} formula for rate {}", formulaType, rateName);

        return true;
    }

    @Override
    public boolean unregisterFormula(String rateName) {
        FormulaInfo removed = formulas.remove(rateName);

        if (removed != null) {
            logger.info("Unregistered formula for rate {}", rateName);
            return true;
        }

        logger.warn("No formula found to unregister for rate {}", rateName);
        return false;
    }

    /**
     * Formül bilgisini saklamak için iç sınıf
     */
    private static class FormulaInfo {
        private final String formula;
        private final String formulaType;

        public FormulaInfo(String formula, String formulaType) {
            this.formula = formula;
            this.formulaType = formulaType;
        }
    }
}