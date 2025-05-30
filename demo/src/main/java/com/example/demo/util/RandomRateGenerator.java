package com.example.demo.util;


import com.example.demo.model.RateData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Gerçekçi kur değişimleri simüle eden yardımcı sınıf
 */
public class RandomRateGenerator {
    private static final Logger logger = LogManager.getLogger(RandomRateGenerator.class);

    private final double minRateChange;
    private final double maxRateChange;
    private final Random random;

    /**
     * Constructor
     * @param minRateChange Minimum değişim oranı (ör. -0.02 = -%2)
     * @param maxRateChange Maximum değişim oranı (ör. 0.02 = %2)
     */
    public RandomRateGenerator(double minRateChange, double maxRateChange) {
        this.minRateChange = minRateChange;
        this.maxRateChange = maxRateChange;
        this.random = new Random();
    }

    /**
     * Mevcut kur verisinden yeni bir kur verisi oluşturur
     * @param currentRate Mevcut kur verisi
     * @return Güncellenmiş kur verisi
     */
    public RateData generateNextRate(RateData currentRate) {
        if (currentRate == null) {
            throw new IllegalArgumentException("Current rate cannot be null");
        }

        // Random rates
        double bidChangeRate = getRandomChangeRate();
        double askChangeRate = getRandomChangeRate();

        // Update about their current values
        double newBid = applyChangeRate(currentRate.getBid(), bidChangeRate);
        double newAsk = applyChangeRate(currentRate.getAsk(), askChangeRate);

        // Alış her zaman satıştan düşük olmalı
        if (newBid >= newAsk) {
            newAsk = newBid * 1.01; // Alış ve satış arasında min %1 fark olsun
        }

        // New rate data
        return new RateData(
                currentRate.getRateName(),
                newBid,
                newAsk,
                LocalDateTime.now()
        );
    }

    /**
     * Minimum ve maksimum değişim oranları arasında rastgele bir oran üretir
     * @return Rastgele değişim oranı
     */
    private double getRandomChangeRate() {
        return minRateChange + (maxRateChange - minRateChange) * random.nextDouble();
    }

    /**
     * Değere değişim oranını uygular
     * @param value Değer
     * @param changeRate Değişim oranı
     * @return Değişim uygulanmış değer
     */
    private double applyChangeRate(double value, double changeRate) {
        return value * (1 + changeRate);
    }
}