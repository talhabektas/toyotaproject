package com.example.fixsimulator.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Objects;

/**
 * Kur verisini temsil eden sınıf
 */
public class RateData {
    private String rateName;
    private double bid;
    private double ask;
    private LocalDateTime timestamp;

    /**
     * Boş constructor
     */
    public RateData() {
    }

    /**
     * Parametreli constructor
     */
    public RateData(String rateName, double bid, double ask, LocalDateTime timestamp) {
        this.rateName = rateName;
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
    }

    // Getter ve Setter metodları
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
     * LocalDateTime'ı Date'e çevirir (FIX mesajları için)
     * @return Date nesnesi
     */
    public Date getTimestampAsDate() {
        return Date.from(timestamp.toInstant(ZoneOffset.UTC));
    }

    /**
     * Rate verisini FIX formatına uygun string olarak döndürür
     * @return FIX formatındaki string
     */
    public String toFixString() {
        return String.format("%s:%.5f/%.5f", rateName, bid, ask);
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