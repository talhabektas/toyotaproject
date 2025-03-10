package com.example.mainapp.calculator.factory;


import com.example.mainapp.calculator.impl.GroovyRateCalculator;
import com.example.mainapp.calculator.impl.JavaRateCalculator;
import com.example.mainapp.calculator.impl.JavaScriptRateCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating rate calculators based on formula type
 */
@Component
public class CalculatorFactory {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorFactory.class);

    private final Map<String, Object> calculators = new HashMap<>();

    /**
     * Get a calculator for the specified formula type
     * @param formulaType Formula type (java, javascript, groovy)
     * @return Calculator instance or null if type is unsupported
     */
    public Object getCalculator(String formulaType) {
        // Check if calculator already exists
        Object calculator = calculators.get(formulaType.toLowerCase());
        if (calculator != null) {
            return calculator;
        }

        // Create new calculator based on type
        try {
            switch (formulaType.toLowerCase()) {
                case "java":
                    calculator = new JavaRateCalculator();
                    break;

                case "javascript":
                    calculator = new JavaScriptRateCalculator();
                    break;

                case "groovy":
                    calculator = new GroovyRateCalculator();
                    break;

                default:
                    logger.error("Unsupported formula type: {}", formulaType);
                    return null;
            }

            // Cache calculator
            calculators.put(formulaType.toLowerCase(), calculator);
            return calculator;

        } catch (Exception e) {
            logger.error("Error creating calculator for type: {}", formulaType, e);
            return null;
        }
    }
}
