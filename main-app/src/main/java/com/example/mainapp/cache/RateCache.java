package com.example.mainapp.cache;


import com.example.mainapp.model.Rate;

import java.util.Set;

/**
 * Kur önbelleği için arayüz
 */
public interface RateCache {

    /**
     * Bir kuru önbelleğe ekler
     * @param rate Eklenecek kur
     */
    void putRate(Rate rate);

    /**
     * Bir kuru adına göre döndürür
     * @param rateName Kur adı
     * @return Kur nesnesi veya bulunamazsa null
     */
    Rate getRate(String rateName);

    /**
     * Bir kuru platform ve kur adına göre döndürür
     * @param platformName Platform adı
     * @param rateName Kur adı
     * @return Kur nesnesi veya bulunamazsa null
     */
    Rate getRate(String platformName, String rateName);

    /**
     * Bir kuru önbellekten kaldırır
     * @param rateName Kur adı
     * @return Kur kaldırıldıysa true
     */
    boolean removeRate(String rateName);

    /**
     * Bir kuru platform ve kur adına göre önbellekten kaldırır
     * @param platformName Platform adı
     * @param rateName Kur adı
     * @return Kur kaldırıldıysa true
     */
    boolean removeRate(String platformName, String rateName);

    /**
     * Önbellekteki tüm kur adlarını döndürür
     * @return Kur adları kümesi
     */
    Set<String> getAllRateNames();

    /**
     * Önbelleği temizler
     */
    void clearCache();
}