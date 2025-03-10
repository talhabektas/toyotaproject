package com.example.mainapp.model;

/**
 * Kur durumunu belirten enum
 */
public enum RateStatus {
    AVAILABLE,      // Kur verisi mevcut
    UNAVAILABLE,    // Kur verisi mevcut değil
    STALE,          // Kur verisi güncel değil
    ERROR           // Kur verisi alınırken hata oluştu
}