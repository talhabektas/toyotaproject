package com.example.kafkaconsumeropensearch.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Kur verisi modeli
 */
public class RateData {
    private String rateName;
    private double bid;
    private double ask;
    private LocalDateTime timestamp;

    // Boş constructor
    public RateData() {
    }

    // Parametreli constructor
    public RateData(String rateName, double bid, double ask, LocalDateTime timestamp) {
        this.rateName = rateName;
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
    }

    /**
     * Kafka mesajından RateData nesnesi oluşturur
     * @param message Kafka mesajı (format: rateName|bid|ask|timestamp)
     * @return RateData nesnesi
     */
    public static RateData fromKafkaMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Kafka mesajı geçersiz formattta: " + message);
        }

        String rateName = parts[0];
        double bid = Double.parseDouble(parts[1]);
        double ask = Double.parseDouble(parts[2]);
        LocalDateTime timestamp = LocalDateTime.parse(parts[3]);

        return new RateData(rateName, bid, ask, timestamp);
    }

    // Getter ve Setterlar
    public String getRateName() {
        return rateName;
    }

    public void setRateName(String rateName) {
        this.rateName = rateName;
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

    /**
     * Spread hesaplar (ask - bid)
     * @return Spread değeri
     */
    public double getSpread() {
        return ask - bid;
    }

    /**
     * Mid-price hesaplar ((ask + bid) / 2)
     * @return Mid-price değeri
     */
    public double getMidPrice() {
        return (ask + bid) / 2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RateData rateData = (RateData) o;
        return Double.compare(rateData.bid, bid) == 0 &&
                Double.compare(rateData.ask, ask) == 0 &&
                Objects.equals(rateName, rateData.rateName) &&
                Objects.equals(timestamp, rateData.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rateName, bid, ask, timestamp);
    }

    @Override
    public String toString() {
        return "RateData{" +
                "rateName='" + rateName + '\'' +
                ", bid=" + bid +
                ", ask=" + ask +
                ", timestamp=" + timestamp +
                '}';
    }
}