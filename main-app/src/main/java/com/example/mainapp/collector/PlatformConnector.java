package com.example.mainapp.collector;

import com.example.mainapp.coordinator.CoordinatorCallBack;

/**
 * Platform bağlantılarını yönetmek için ortak arayüz
 */
public interface PlatformConnector {

    /**
     * Bağlantıyı gerçekleştirmek için kullanılan metod
     * @param platformName Platform adı
     * @param userid Kullanıcı adı
     * @param password Şifre
     * @return Bağlantı başarılı ise true
     */
    boolean connect(String platformName, String userid, String password);

    /**
     * Bağlantıyı kesmek için kullanılan metod
     * @param platformName Platform adı
     * @param userid Kullanıcı adı
     * @param password Şifre
     * @return İşlem başarılı ise true
     */
    boolean disconnect(String platformName, String userid, String password);

    /**
     * Bir kura abone olmak için çağrılacak metod
     * @param platformName Platform adı
     * @param rateName Kur adı
     * @return İşlem başarılı ise true
     */
    boolean subscribe(String platformName, String rateName);

    /**
     * Bir kur aboneliğini sonlandırmak için çağrılacak metod
     * @param platformName Platform adı
     * @param rateName Kur adı
     * @return İşlem başarılı ise true
     */
    boolean unsubscribe(String platformName, String rateName);

    /**
     * Koordinatör callback'ini ayarlar
     * @param callback Koordinatör callback'i
     */
    void setCallback(CoordinatorCallBack callback);

    /**
     * Platform adını döndürür
     * @return Platform adı
     */
    String getPlatformName();

    /**
     * Bağlayıcıyı başlatır
     */
    void start();

    /**
     * Bağlayıcıyı durdurur
     */
    void stop();
}