package com.example.mainapp.calculator.impl;



import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Groovy dilinde kur hesaplama uygulaması
 */
public class GroovyRateCalculator {

    private static final Logger logger = LoggerFactory.getLogger(GroovyRateCalculator.class);

    // Derlenmiş scriptleri önbellekle
    private final Map<String, groovy.lang.Script> compiledScripts = new ConcurrentHashMap<>();

    /**
     * Groovy formülünü çalıştırır
     * @param context Hesaplama bağlamı
     * @return Bid ve ask içeren sonuç haritası
     */
    public Map<String, Object> executeFormula(Map<String, Object> context) {
        try {
            String targetRateName = (String) context.get("targetRateName");
            String formula = (String) context.get("formula");
            Object dependencies = context.get("dependencies");

            // Derlenmiş script var mı kontrol et ya da yenisini derle
            groovy.lang.Script script = compiledScripts.get(targetRateName);
            if (script == null) {
                // Formülü çağıran wrapper script oluştur
                String wrapperScript = formula + "\n" +
                        "def execute() {\n" +
                        "    return calculate(dependencies)\n" +
                        "}\n" +
                        "return execute()";

                // Scripti derle
                GroovyShell shell = new GroovyShell();
                script = shell.parse(wrapperScript);

                // Derlenmiş scripti önbelleğe al
                compiledScripts.put(targetRateName, script);
            }

            // Değişkenler için binding oluştur
            Binding binding = new Binding();
            binding.setVariable("dependencies", dependencies);
            script.setBinding(binding);

            // Scripti çalıştır
            Object result = script.run();

            // Sonucu Map'e dönüştür
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                return resultMap;
            } else {
                logger.error("Groovy calculation did not return expected format");
                return null;
            }

        } catch (Exception e) {
            logger.error("Error executing Groovy formula", e);
            return null;
        }
    }
}