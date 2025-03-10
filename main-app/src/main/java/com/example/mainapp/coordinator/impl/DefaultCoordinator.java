package com.example.mainapp.coordinator.impl;


import com.example.mainapp.cache.RateCache;
import com.example.mainapp.calculator.RateCalculator;
import com.example.mainapp.collector.PlatformConnector;
import com.example.mainapp.coordinator.Coordinator;
import com.example.mainapp.model.Rate;
import com.example.mainapp.model.RateFields;
import com.example.mainapp.model.RateStatus;
import com.example.mainapp.services.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Coordinator arayüzünün varsayılan uygulaması
 */
@Component
public class DefaultCoordinator implements Coordinator {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCoordinator.class);

    private final Map<String, PlatformConnector> connectors = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> calculatedRateDependencies = new ConcurrentHashMap<>();

    private final RateCache rateCache;
    private final RateCalculator rateCalculator;
    private final KafkaProducerService kafkaProducerService;

    /**
     * Constructor
     * @param rateCache Kur önbelleği
     * @param rateCalculator Kur hesaplayıcı
     * @param kafkaProducerService Kafka üretici servisi
     */
    @Autowired
    public DefaultCoordinator(RateCache rateCache, RateCalculator rateCalculator,
                              KafkaProducerService kafkaProducerService) {
        this.rateCache = rateCache;
        this.rateCalculator = rateCalculator;
        this.kafkaProducerService = kafkaProducerService;

        initializeCalculatedRateDependencies();
    }

    /**
     * Hesaplanan kurlar için bağımlılıkları başlatır
     */
    private void initializeCalculatedRateDependencies() {
        // USDTRY için PF1_USDTRY ve PF2_USDTRY bağımlılıkları
        Set<String> usdtryDeps = new HashSet<>();
        usdtryDeps.add("PF1_USDTRY");
        usdtryDeps.add("PF2_USDTRY");
        calculatedRateDependencies.put("USDTRY", usdtryDeps);

        // EURTRY için PF1_EURUSD, PF2_EURUSD, PF1_USDTRY ve PF2_USDTRY bağımlılıkları
        Set<String> eurtryDeps = new HashSet<>();
        eurtryDeps.add("PF1_EURUSD");
        eurtryDeps.add("PF2_EURUSD");
        eurtryDeps.add("PF1_USDTRY");
        eurtryDeps.add("PF2_USDTRY");
        calculatedRateDependencies.put("EURTRY", eurtryDeps);

        // GBPTRY için PF1_GBPUSD, PF2_GBPUSD, PF1_USDTRY ve PF2_USDTRY bağımlılıkları
        Set<String> gbptryDeps = new HashSet<>();
        gbptryDeps.add("PF1_GBPUSD");
        gbptryDeps.add("PF2_GBPUSD");
        gbptryDeps.add("PF1_USDTRY");
        gbptryDeps.add("PF2_USDTRY");
        calculatedRateDependencies.put("GBPTRY", gbptryDeps);
    }

    @Override
    public void start() {
        logger.info("Starting coordinator");

        // Tüm bağlayıcıları başlat
        connectors.values().forEach(PlatformConnector::start);
    }

    @Override
    public void stop() {
        logger.info("Stopping coordinator");

        // Tüm bağlayıcıları durdur
        connectors.values().forEach(PlatformConnector::stop);
    }

    @Override
    public void addConnector(PlatformConnector connector) {
        String platformName = connector.getPlatformName();
        logger.info("Adding connector for platform: {}", platformName);

        connector.setCallback(this);
        connectors.put(platformName, connector);
    }

    @Override
    public boolean removeConnector(String platformName) {
        logger.info("Removing connector for platform: {}", platformName);

        PlatformConnector connector = connectors.remove(platformName);
        if (connector != null) {
            connector.stop();
            return true;
        }
        return false;
    }

    @Override
    public List<PlatformConnector> getConnectors() {
        return new ArrayList<>(connectors.values());
    }

    @Override
    public Set<String> subscribeRate(String rateName) {
        logger.info("Subscribing to rate {} on all platforms", rateName);

        Set<String> successfulPlatforms = new HashSet<>();

        for (Map.Entry<String, PlatformConnector> entry : connectors.entrySet()) {
            String platformName = entry.getKey();
            PlatformConnector connector = entry.getValue();

            if (connector.subscribe(platformName, rateName)) {
                successfulPlatforms.add(platformName);
            }
        }

        return successfulPlatforms;
    }

    @Override
    public boolean subscribeRate(String platformName, String rateName) {
        logger.info("Subscribing to rate {} on platform {}", rateName, platformName);

        PlatformConnector connector = connectors.get(platformName);
        if (connector != null) {
            return connector.subscribe(platformName, rateName);
        }

        logger.warn("Platform {} not found", platformName);
        return false;
    }

    @Override
    public Set<String> unsubscribeRate(String rateName) {
        logger.info("Unsubscribing from rate {} on all platforms", rateName);

        Set<String> successfulPlatforms = new HashSet<>();

        for (Map.Entry<String, PlatformConnector> entry : connectors.entrySet()) {
            String platformName = entry.getKey();
            PlatformConnector connector = entry.getValue();

            if (connector.unsubscribe(platformName, rateName)) {
                successfulPlatforms.add(platformName);
            }
        }

        return successfulPlatforms;
    }

    @Override
    public boolean unsubscribeRate(String platformName, String rateName) {
        logger.info("Unsubscribing from rate {} on platform {}", rateName, platformName);

        PlatformConnector connector = connectors.get(platformName);
        if (connector != null) {
            return connector.unsubscribe(platformName, rateName);
        }

        logger.warn("Platform {} not found", platformName);
        return false;
    }

    @Override
    public Rate getRate(String rateName) {
        return rateCache.getRate(rateName);
    }

    @Override
    public Rate getRate(String platformName, String rateName) {
        return rateCache.getRate(platformName, rateName);
    }

    @Override
    public Set<String> getAllRateNames() {
        return rateCache.getAllRateNames();
    }

    @Override
    public Set<String> getAllPlatformNames() {
        return connectors.keySet();
    }

    @Override
    public boolean calculateRate(String targetRateName) {
        logger.info("Calculating rate: {}", targetRateName);

        Set<String> dependencies = calculatedRateDependencies.get(targetRateName);
        if (dependencies == null) {
            logger.warn("No dependencies defined for calculated rate: {}", targetRateName);
            return false;
        }

        // Tüm bağımlılıkların mevcut olup olmadığını kontrol et
        Map<String, Rate> dependencyRates = new HashMap<>();
        for (String depRateName : dependencies) {
            Rate depRate = rateCache.getRate(depRateName);
            if (depRate == null) {
                logger.warn("Dependency rate {} not available for calculating {}", depRateName, targetRateName);
                return false;
            }
            dependencyRates.put(depRateName, depRate);
        }

        try {
            // Kuru hesapla
            Rate calculatedRate = rateCalculator.calculate(targetRateName, dependencyRates);
            if (calculatedRate != null) {
                // Hesaplanan kuru önbelleğe ve Kafka'ya gönder
                rateCache.putRate(calculatedRate);
                kafkaProducerService.sendRate(calculatedRate);

                logger.info("Successfully calculated rate: {}", calculatedRate);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error calculating rate {}", targetRateName, e);
        }

        return false;
    }

    // CoordinatorCallback uygulamaları

    @Override
    public void onConnect(String platformName, boolean status) {
        logger.info("Platform {} connection status: {}", platformName, status);
    }

    @Override
    public void onDisConnect(String platformName, boolean status) {
        logger.info("Platform {} disconnection status: {}", platformName, status);
    }

    @Override
    public void onRateAvailable(String platformName, String rateName, Rate rate) {
        logger.debug("Rate {} available from platform {}", rateName, platformName);

        // Kuru önbelleğe ekle
        rateCache.putRate(rate);

        // Kafka'ya gönder
        kafkaProducerService.sendRate(rate);

        // Bu kur herhangi bir hesaplanan kurun bağımlılığı mı kontrol et
        checkAndCalculateDependentRates(rateName);
    }

    @Override
    public void onRateUpdate(String platformName, String rateName, RateFields rateFields) {
        logger.debug("Rate {} updated from platform {}", rateName, platformName);

        // Önbellekten mevcut kuru al
        Rate existingRate = rateCache.getRate(platformName, rateName);
        if (existingRate != null) {
            // Güncellenmiş kur oluştur
            Rate updatedRate = new Rate(
                    existingRate.getRateName(),
                    existingRate.getPlatformName(),
                    rateFields.getBid(),
                    rateFields.getAsk(),
                    rateFields.getTimestamp(),
                    existingRate.isCalculated()
            );

            // Önbelleği güncelle
            rateCache.putRate(updatedRate);

            // Kafka'ya gönder
            kafkaProducerService.sendRate(updatedRate);

            // Bu kur herhangi bir hesaplanan kurun bağımlılığı mı kontrol et
            checkAndCalculateDependentRates(rateName);
        } else {
            logger.warn("Received update for unknown rate {} from platform {}", rateName, platformName);
        }
    }

    @Override
    public void onRateStatus(String platformName, String rateName, RateStatus rateStatus) {
        logger.info("Rate {} status from platform {}: {}", rateName, platformName, rateStatus);

        if (rateStatus == RateStatus.UNAVAILABLE || rateStatus == RateStatus.ERROR) {
            // Kuru önbellekten kaldır
            rateCache.removeRate(platformName, rateName);
        }
    }

    /**
     * Bir kur güncellendiyse ve bu kur hesaplanan kurların bağımlılığı ise gerekli hesaplamaları yapar
     * @param updatedRateName Güncellenen kur adı
     */
    private void checkAndCalculateDependentRates(String updatedRateName) {
        for (Map.Entry<String, Set<String>> entry : calculatedRateDependencies.entrySet()) {
            String calculatedRateName = entry.getKey();
            Set<String> dependencies = entry.getValue();

            if (dependencies.contains(updatedRateName)) {
                logger.debug("Rate {} is a dependency for calculated rate {}, triggering calculation",
                        updatedRateName, calculatedRateName);
                calculateRate(calculatedRateName);
            }
        }
    }
}