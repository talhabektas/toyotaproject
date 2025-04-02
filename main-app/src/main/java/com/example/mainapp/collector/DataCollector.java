package com.example.mainapp.collector;

import com.example.mainapp.coordinator.CoordinatorCallBack;
import com.example.mainapp.model.RateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Platform bağlayıcıları için temel işlevselliği sağlayan soyut sınıf
 */
public abstract class DataCollector implements PlatformConnector, Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DataCollector.class);

    protected String platformName;
    protected CoordinatorCallBack callback;
    protected Properties config;
    protected final Set<String> subscribedRates = new HashSet<>();
    protected Thread workerThread;
    protected final AtomicBoolean running = new AtomicBoolean(false);

    // Platform sağlık kontrolü için zamanlayıcı
    private ScheduledExecutorService healthCheckScheduler;
    // Sağlık kontrolü aralığı (30 saniye)
    private static final long HEALTH_CHECK_INTERVAL = 30000;
    // Platform yanıt vermeme süresi (60 saniye)
    private static final long PLATFORM_TIMEOUT = 60000;
    private long lastResponseTime;

    /**
     * Constructor
     * @param platformName Platform adı
     * @param config Platform konfigürasyonu
     */
    public DataCollector(String platformName, Properties config) {
        this.platformName = platformName;
        this.config = config;
        this.lastResponseTime = System.currentTimeMillis();
    }

    @Override
    public void setCallback(CoordinatorCallBack callback) {
        this.callback = callback;
    }

    @Override
    public String getPlatformName() {
        return platformName;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            logger.info("Starting data collector for platform: {}", platformName);
            workerThread = new Thread(this, "DataCollector-" + platformName);
            workerThread.setDaemon(true);
            workerThread.start();

            // Platform sağlık kontrolünü başlat
            scheduleHealthCheck();
        } else {
            logger.warn("Data collector for platform {} is already running", platformName);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("Stopping data collector for platform: {}", platformName);

            // Tüm abonelikleri iptal et
            Set<String> rates = new HashSet<>(subscribedRates);
            rates.forEach(rateName -> unsubscribe(platformName, rateName));

            // Bağlantıyı kapat
            disconnect(platformName, null, null);

            // Thread'i durdur
            if (workerThread != null) {
                workerThread.interrupt();
                try {
                    workerThread.join(5000);
                } catch (InterruptedException e) {
                    logger.error("Interrupted while waiting for worker thread to stop", e);
                    Thread.currentThread().interrupt();
                }
            }

            // Sağlık kontrolünü durdur
            if (healthCheckScheduler != null) {
                healthCheckScheduler.shutdownNow();
            }

            logger.info("Data collector for platform {} stopped", platformName);
        } else {
            logger.warn("Data collector for platform {} is not running", platformName);
        }
    }

    /**
     * Kur verilerine abone olduktan sonra yapılacak işlemleri gerçekleştirir
     * @param rateName Kur adı
     * @param success Abonelik başarılı mı
     */
    protected void handleSubscriptionResult(String rateName, boolean success) {
        if (success) {
            logger.info("Successfully subscribed to rate {} on platform {}", rateName, platformName);
            subscribedRates.add(rateName);
        } else {
            logger.warn("Failed to subscribe to rate {} on platform {}", rateName, platformName);
        }
    }

    /**
     * Kur verisi aboneliğini iptal ettikten sonra yapılacak işlemleri gerçekleştirir
     * @param rateName Kur adı
     * @param success İptal işlemi başarılı mı
     */
    protected void handleUnsubscriptionResult(String rateName, boolean success) {
        if (success) {
            logger.info("Successfully unsubscribed from rate {} on platform {}", rateName, platformName);
            subscribedRates.remove(rateName);
        } else {
            logger.warn("Failed to unsubscribe from rate {} on platform {}", rateName, platformName);
        }
    }

    /**
     * Platform yanıt verme zamanını günceller
     */
    protected void updateLastResponseTime() {
        this.lastResponseTime = System.currentTimeMillis();
    }

    /**
     * Platform sağlık kontrolünü zamanlar
     */
    protected void scheduleHealthCheck() {
        healthCheckScheduler = Executors.newSingleThreadScheduledExecutor();
        healthCheckScheduler.scheduleAtFixedRate(() -> {
            if (!checkPlatformHealth()) {
                logger.error("Platform {} health check failed, sending alert", platformName);
                // Koordinatöre platformun yanıt vermediğini bildir
                if (callback != null) {
                    subscribedRates.forEach(rateName ->
                            callback.onRateStatus(platformName, rateName, RateStatus.UNAVAILABLE));
                }
                // Platform bağlantısını yenilemeyi dene
                if (running.get()) {
                    logger.info("Attempting to reconnect to platform: {}", platformName);
                    disconnect(platformName, null, null);
                    connect(platformName, null, null);
                }
            }
        }, HEALTH_CHECK_INTERVAL, HEALTH_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * Platform sağlık kontrolü
     * @return Platform sağlıklı ise true
     */
    protected boolean checkPlatformHealth() {
        if (!running.get()) {
            return false;
        }

        // Son yanıt üzerinden geçen süreyi kontrol et
        long timeSinceLastResponse = System.currentTimeMillis() - lastResponseTime;
        return timeSinceLastResponse < PLATFORM_TIMEOUT;
    }
}