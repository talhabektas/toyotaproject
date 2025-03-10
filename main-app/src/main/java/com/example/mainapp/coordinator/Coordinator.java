package com.example.mainapp.coordinator;


import com.example.mainapp.collector.PlatformConnector;
import com.example.mainapp.model.Rate;

import java.util.List;
import java.util.Set;

/**
 * Koordinatörün temel işlevselliğini tanımlayan arayüz
 */
public interface Coordinator extends CoordinatorCallBack {

    /**
     * Koordinatörü başlatır
     */
    void start();

    /**
     * Koordinatörü durdurur
     */
    void stop();

    /**
     * Platform bağlayıcı ekler
     * @param connector Platform bağlayıcı
     */
    void addConnector(PlatformConnector connector);

    /**
     * Platform bağlayıcıyı kaldırır
     * @param platformName Platform adı
     * @return İşlem başarılı ise true
     */
    boolean removeConnector(String platformName);

    /**
     * Tüm platform bağlayıcıları döndürür
     * @return Platform bağlayıcı listesi
     */
    List<PlatformConnector> getConnectors();

    /**
     * Tüm platformlarda bir kura abone olur
     * @param rateName Kur adı
     * @return Aboneliğin başarılı olduğu platform adları kümesi
     */
    Set<String> subscribeRate(String rateName);

    /**
     * Belirli bir platformda bir kura abone olur
     * @param platformName Platform adı
     * @param rateName Kur adı
     * @return Abonelik başarılı ise true
     */
    boolean subscribeRate(String platformName, String rateName);

    /**
     * Tüm platformlarda bir kurun aboneliğini iptal eder
     * @param rateName Kur adı
     * @return Aboneliğin iptal edildiği platform adları kümesi
     */
    Set<String> unsubscribeRate(String rateName);

    /**
     * Belirli bir platformda bir kurun aboneliğini iptal eder
     * @param platformName Platform adı
     * @param rateName Kur adı
     * @return İptal işlemi başarılı ise true
     */
    boolean unsubscribeRate(String platformName, String rateName);

    /**
     * Önbellekten bir kur verisini döndürür
     * @param rateName Kur adı
     * @return Kur verisi veya bulunamazsa null
     */
    Rate getRate(String rateName);

    /**
     * Belirli bir platformdan bir kur verisini döndürür
     * @param platformName Platform adı
     * @param rateName Kur adı
     * @return Kur verisi veya bulunamazsa null
     */
    Rate getRate(String platformName, String rateName);

    /**
     * Tüm mevcut kur adlarını döndürür
     * @return Kur adları kümesi
     */
    Set<String> getAllRateNames();

    /**
     * Tüm mevcut platform adlarını döndürür
     * @return Platform adları kümesi
     */
    Set<String> getAllPlatformNames();

    /**
     * Bir kur hesaplar
     * @param targetRateName Hedef kur adı
     * @return Hesaplama başarılı ise true
     */
    boolean calculateRate(String targetRateName);
}