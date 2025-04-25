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
 * Service for FIX protocol server and rate simulation
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
     * Runs when the service is initialized
     */
    @PostConstruct
    public void init() {
        initializeRates();
        initializeFixAcceptor();
        start();
    }

    /**
     * Runs when the service is being shut down
     */
    @PreDestroy
    public void shutdown() {
        stop();
    }

    /**
     * Populates the rate data with initial values
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

        // Set the rate data for the FixApplication
        fixApplication.setRateData(rateDataMap);
    }

    /**
     * Initializes the FIX acceptor
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
     * Starts the service
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
     * Stops the service
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
     * Updates rate data at regular intervals
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

            // Send updated rate data to all connected sessions
            fixApplication.publishRateUpdate(newRateData);

            logger.debug("Updated rate: {}", newRateData);
        });
    }

    /**
     * Returns all rate data
     * @return A map of rate data
     */
    public Map<String, RateData> getAllRates() {
        return new HashMap<>(rateDataMap);
    }

    /**
     * Returns specific rate data
     * @param rateName The rate name
     * @return The rate data, or null if not found
     */
    public RateData getRateData(String rateName) {
        return rateDataMap.get(rateName);
    }
}