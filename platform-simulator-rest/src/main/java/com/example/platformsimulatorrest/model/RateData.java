package com.example.platformsimulatorrest.model;


import java.time.LocalDateTime;
import java.util.Objects;


public class RateData {
    private String rateName;
    private double bid;
    private double ask;
    private LocalDateTime timestamp;


    public RateData() {
    }


    public RateData(String rateName, double bid, double ask, LocalDateTime timestamp) {
        this.rateName = rateName;
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
    }

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