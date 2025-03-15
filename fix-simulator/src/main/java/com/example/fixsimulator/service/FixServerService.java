package com.example.fixsimulator.service;

import com.example.fixsimulator.config.FixSimulatorConfig;
import com.example.fixsimulator.fix.FixApplication;
import com.example.fixsimulator.model.RateData;
import com.example.fixsimulator.util.RandomRateGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import quickfix.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FIX protokolü sunucusu ve kur simülasyonu servisi
 */
@Service
public class FixServerService {

    private static final Logger logger = LogManager.getLogger(FixServerService.class);

    @Value("${quickfix.config:config/quickfix.cfg}")
    private String quickfixConfigFile;

    private final FixSimulatorConfig simulatorConfig;
    private final FixApplication fixApplication;
    private final RandomRateGenerator rateGenerator;
    private final Map<String, RateData> rateDataMap = new ConcurrentHashMap<>();

    private Acceptor acceptor;
    private AtomicInteger updateCount = new AtomicInteger(0);
    private AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Constructor
     */
    @Autowired
    public FixServerService(FixSimulatorConfig simulatorConfig, FixApplication fixApplication) {
        this.simulatorConfig = simulatorConfig;
        this.fixApplication = fixApplication;
        this.rateGenerator = new RandomRateGenerator(
                simulatorConfig.getMinRateChange(),
                simulatorConfig.getMaxRateChange());
    }

    /**
     * Servis başlatıldığında çalışır
     */
    @PostConstruct
    public void init() {
        initializeRates();
        initializeFixAcceptor();
        start();
    }

    /**
     * Servis durdurulduğunda çalışır
     */
    @PreDestroy
    public void shutdown() {
        stop();
    }

    /**
     * Kur verilerini başlangıç değerleriyle doldurur
     */
    private void initializeRates() {
        simulatorConfig.getInitialRates().forEach((rateName, rateConfig) -> {
            RateData rateData = new RateData(
                    rateName,
                    rateConfig.getInitialBid(),
                    rateConfig.getInitialAsk(),
                    LocalDateTime.now()
            );
            rateDataMap.put(rateName, rateData);
            logger.info("Initialized rate: {}", rateData);
        });

        // FixApplication'a kur verilerini set et
        fixApplication.setRateData(rateDataMap);
    }

    /**
     * FIX acceptor'u başlatır
     */
    private void initializeFixAcceptor() {
        try {
            SessionSettings settings = new SessionSettings(quickfixConfigFile);
            MessageStoreFactory storeFactory = new FileStoreFactory(settings);
            LogFactory logFactory = new SLF4JLogFactory(settings);
            MessageFactory messageFactory = new DefaultMessageFactory();

            acceptor = new SocketAcceptor(
                    fixApplication,
                    storeFactory,
                    settings,
                    logFactory,
                    messageFactory);

            logger.info("FIX acceptor initialized with config: {}", quickfixConfigFile);
        } catch (ConfigError e) {
            logger.error("Error initializing FIX acceptor", e);
            throw new RuntimeException("Failed to initialize FIX acceptor", e);
        }
    }

    /**
     * Servisi başlatır
     */
    public synchronized void start() {
        if (running.compareAndSet(false, true)) {
            try {
                acceptor.start();
                logger.info("FIX server started, waiting for connections");
            } catch (RuntimeError | ConfigError e) {
                logger.error("Failed to start FIX acceptor", e);
                running.set(false);
                throw new RuntimeException("Failed to start FIX server", e);
            }
        } else {
            logger.warn("FIX server is already running");
        }
    }

    /**
     * Servisi durdurur
     */
    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            try {
                acceptor.stop();
                logger.info("FIX server stopped");
            } catch (RuntimeError e) {
                logger.error("Error stopping FIX acceptor", e);
            }
        } else {
            logger.warn("FIX server is not running");
        }
    }

    /**
     * Kur verilerini düzenli aralıklarla günceller
     */
    @Scheduled(fixedDelayString = "${simulation.updateIntervalMs:5000}")
    public void updateRates() {
        if (!running.get()) {
            return;
        }

        int currentCount = updateCount.incrementAndGet();
        int maxUpdates = simulatorConfig.getMaxUpdates();

        if (maxUpdates > 0 && currentCount > maxUpdates) {
            logger.info("Reached maximum update count ({}), stopping simulation", maxUpdates);
            stop();
            return;
        }

        rateDataMap.forEach((rateName, oldRateData) -> {
            RateData newRateData = rateGenerator.generateNextRate(oldRateData);
            rateDataMap.put(rateName, newRateData);

            // Bağlı olan tüm oturumlara güncel kur verilerini gönder
            fixApplication.publishRateUpdate(newRateData);

            logger.debug("Updated rate: {}", newRateData);
        });
    }

    /**
     * Tüm kur verilerini döndürür
     * @return Kur verileri haritası
     */
    public Map<String, RateData> getAllRates() {
        return new HashMap<>(rateDataMap);
    }

    /**
     * Belirli bir kur verisini döndürür
     * @param rateName Kur adı
     * @return Kur verisi veya bulunamazsa null
     */
    public RateData getRateData(String rateName) {
        return rateDataMap.get(rateName);
    }
}