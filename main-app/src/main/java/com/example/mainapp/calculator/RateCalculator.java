package com.example.mainapp.calculator;

import com.example.mainapp.model.Rate;

import java.util.Map;

/**
 * Kur hesaplama için arayüz
 */
public interface RateCalculator {

    /**
     * Bağımlılıklara göre bir kur hesaplar
     * @param targetRateName Hedef kur adı
     * @param dependencyRates Bağımlılık adı -> Kur nesnesi eşlemesi
     * @return Hesaplanan kur veya hesaplanamazsa null
     */
    Rate calculate(String targetRateName, Map<String, Rate> dependencyRates);

    /**
     * Bir kur için hesaplama formülü kaydeder
     * @param rateName Kur adı
     * @param formula Kaydedilecek formül
     * @param formulaType Formül dil tipi (java, javascript, groovy)
     * @return Kayıt başarılı ise true
     */
    boolean registerFormula(String rateName, String formula, String formulaType);

    /**
     * Bir kur için hesaplama formülünü kaldırır
     * @param rateName Kur adı
     * @return Kaldırma başarılı ise true
     */
    boolean unregisterFormula(String rateName);
}