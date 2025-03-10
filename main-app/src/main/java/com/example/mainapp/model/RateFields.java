package com.example.mainapp.model;


import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Kur alanlarını içeren model sınıfı (güncelleme işlemleri için)
 */
public class RateFields implements Serializable {

    private static final long serialVersionUID = 1L;

    private double bid;               // Alış fiyatı
    private double ask;               // Satış fiyatı
    private LocalDateTime timestamp;  // Zaman damgası

    /**
     * Boş constructor
     */
    public RateFields() {
    }

    /**
     * Parametreli constructor
     */
    public RateFields(double bid, double ask, LocalDateTime timestamp) {
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
    }

    /**
     * Rate nesnesinden RateFields oluşturur
     */
    public static RateFields fromRate(Rate rate) {
        return new RateFields(rate.getBid(), rate.getAsk(), rate.getTimestamp());
    }

    // Getter ve Setter metodları
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RateFields that = (RateFields) o;
        return Double.compare(that.bid, bid) == 0 &&
                Double.compare(that.ask, ask) == 0 &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bid, ask, timestamp);
    }

    @Override
    public String toString() {
        return "RateFields{" +
                "bid=" + bid +
                ", ask=" + ask +
                ", timestamp=" + timestamp +
                '}';
    }
}