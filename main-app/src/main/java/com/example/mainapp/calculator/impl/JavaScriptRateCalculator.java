package com.example.mainapp.calculator.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

/**
 * JavaScript dilinde kur hesaplama uygulaması
 */
public class JavaScriptRateCalculator {

    private static final Logger logger = LoggerFactory.getLogger(JavaScriptRateCalculator.class);

    private final ScriptEngine engine;
    private final ObjectMapper objectMapper;

    /**
     * Constructor
     */
    public JavaScriptRateCalculator() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("JavaScript");
        this.objectMapper = new ObjectMapper();

        if (engine == null) {
            logger.error("JavaScript engine not available");
            throw new RuntimeException("JavaScript engine not available");
        }
    }

    /**
     * JavaScript formülünü çalıştırır
     * @param context Hesaplama bağlamı
     * @return Bid ve ask içeren sonuç haritası
     */
    public Map<String, Object> executeFormula(Map<String, Object> context) {
        try {
            String formula = (String) context.get("formula");
            Object dependenciesObj = context.get("dependencies");

            // Bağımlılıkları JavaScript'e uygun bir formata dönüştür
            String dependenciesJson = objectMapper.writeValueAsString(dependenciesObj);

            // Scripti oluştur
            String script = formula + "\n" +
                    "var dependencies = " + dependenciesJson + ";\n" +
                    "var result = calculate(dependencies);";

            // Scripti çalıştır
            engine.eval(script);

            // Sonucu al
            Object resultObj = engine.get("result");

            // Sonucu Map'e dönüştür
            if (resultObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) resultObj;
                return result;
            } else {
                logger.error("JavaScript calculation did not return expected format");
                return null;
            }

        } catch (Exception e) {
            logger.error("Error executing JavaScript formula", e);
            return null;
        }
    }
}