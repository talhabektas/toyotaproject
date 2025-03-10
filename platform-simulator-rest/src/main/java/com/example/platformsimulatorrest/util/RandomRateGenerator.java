package com.example.platformsimulatorrest.util;



import com.example.platformsimulatorrest.model.RateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Gerçekçi kur değişimleri simüle eden yardımcı sınıf
 */
public class RandomRateGenerator {
    private static final Logger logger = LoggerFactory.getLogger(RandomRateGenerator.class);

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

        // Rastgele değişim oranları oluştur
        double bidChangeRate = getRandomChangeRate();
        double askChangeRate = getRandomChangeRate();

        // Mevcut değerleri değişim oranına göre güncelle
        double newBid = applyChangeRate(currentRate.getBid(), bidChangeRate);
        double newAsk = applyChangeRate(currentRate.getAsk(), askChangeRate);

        // Alış her zaman satıştan düşük olmalı
        if (newBid >= newAsk) {
            newAsk = newBid * 1.01; // Alış ve satış arasında min %1 fark olsun
        }

        // Yeni kur verisini oluştur
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