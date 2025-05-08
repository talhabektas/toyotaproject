package com.example.mainapp.model;


import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Kur bilgilerini taşıyan model sınıfı
 */
public class Rate implements Serializable {

    private static final long serialVersionUID = 1L;

    private String rateName;          // Kur adı (ör. PF1_USDTRY, USDTRY)
    private String platformName;      // Platform adı (ör. PF1, PF2)
    private double bid;               // Alış fiyatı
    private double ask;               // Satış fiyatı
    private LocalDateTime timestamp;  // Zaman damgası
    private boolean calculated;       // Hesaplanmış kur mu?

    /**
     * Boş constructor
     */
    public Rate() {
    }

    /**
     * Parametreli constructor
     */
    public Rate(String rateName, String platformName, double bid, double ask,
                LocalDateTime timestamp, boolean calculated) {
        this.rateName = rateName;
        this.platformName = platformName;
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
        this.calculated = calculated;
    }

    // Getter ve Setter metodları
    public String getRateName() {
        return rateName;
    }

    public void setRateName(String rateName) {
        this.rateName = rateName;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public double getBid() {
        return bid;
    }

    public void setBid(double bid) {
        this.bid = bid;
    }

    public double getAsk() {
        return ask;
    }

    public void setAsk(double ask) {
        this.ask = ask;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isCalculated() {
        return calculated;
    }

    public void setCalculated(boolean calculated) {
        this.calculated = calculated;
    }

    /**
     * Kafka formatına dönüştürür
     */
    public String toKafkaString() {
        return String.format("%s|%.2f|%.2f|%s",
                rateName, bid, ask, timestamp.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rate rate = (Rate) o;
        return Double.compare(rate.bid, bid) == 0 &&
                Double.compare(rate.ask, ask) == 0 &&
                calculated == rate.calculated &&
                Objects.equals(rateName, rate.rateName) &&
                Objects.equals(platformName, rate.platformName) &&
                Objects.equals(timestamp, rate.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rateName, platformName, bid, ask, timestamp, calculated);
    }

    @Override
    public String toString() {
        return "Rate{" +
                "rateName='" + rateName + '\'' +
                ", platformName='" + platformName + '\'' +
                ", bid=" + bid +
                ", ask=" + ask +
                ", timestamp=" + timestamp +
                ", calculated=" + calculated +
                '}';
    }
}