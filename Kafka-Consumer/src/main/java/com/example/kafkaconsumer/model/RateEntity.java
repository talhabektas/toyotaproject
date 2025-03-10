package com.example.kafkaconsumer.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity class for storing rate data in the database
 */
@Entity
@Table(name = "tbl_rates")
public class RateEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate_name", nullable = false, length = 20)
    private String rateName;

    @Column(name = "bid", nullable = false)
    private double bid;

    @Column(name = "ask", nullable = false)
    private double ask;

    @Column(name = "rate_update_time", nullable = false)
    private LocalDateTime rateUpdateTime;

    @Column(name = "db_update_time", nullable = false)
    private LocalDateTime dbUpdateTime;

    /**
     * Default constructor
     */
    public RateEntity() {
    }

    /**
     * Parameterized constructor
     */
    public RateEntity(String rateName, double bid, double ask,
                      LocalDateTime rateUpdateTime, LocalDateTime dbUpdateTime) {
        this.rateName = rateName;
        this.bid = bid;
        this.ask = ask;
        this.rateUpdateTime = rateUpdateTime;
        this.dbUpdateTime = dbUpdateTime;
    }

    /**
     * Create entity from Kafka message
     * @param message Kafka message in format "rateName|bid|ask|timestamp"
     * @return RateEntity instance
     */
    public static RateEntity fromKafkaMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid Kafka message format: " + message);
        }

        String rateName = parts[0];
        double bid = Double.parseDouble(parts[1]);
        double ask = Double.parseDouble(parts[2]);
        LocalDateTime rateUpdateTime = LocalDateTime.parse(parts[3]);
        LocalDateTime dbUpdateTime = LocalDateTime.now();

        return new RateEntity(rateName, bid, ask, rateUpdateTime, dbUpdateTime);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getRateUpdateTime() {
        return rateUpdateTime;
    }

    public void setRateUpdateTime(LocalDateTime rateUpdateTime) {
        this.rateUpdateTime = rateUpdateTime;
    }

    public LocalDateTime getDbUpdateTime() {
        return dbUpdateTime;
    }

    public void setDbUpdateTime(LocalDateTime dbUpdateTime) {
        this.dbUpdateTime = dbUpdateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RateEntity that = (RateEntity) o;
        return Double.compare(that.bid, bid) == 0 &&
                Double.compare(that.ask, ask) == 0 &&
                Objects.equals(id, that.id) &&
                Objects.equals(rateName, that.rateName) &&
                Objects.equals(rateUpdateTime, that.rateUpdateTime) &&
                Objects.equals(dbUpdateTime, that.dbUpdateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, rateName, bid, ask, rateUpdateTime, dbUpdateTime);
    }

    @Override
    public String toString() {
        return "RateEntity{" +
                "id=" + id +
                ", rateName='" + rateName + '\'' +
                ", bid=" + bid +
                ", ask=" + ask +
                ", rateUpdateTime=" + rateUpdateTime +
                ", dbUpdateTime=" + dbUpdateTime +
                '}';
    }
}