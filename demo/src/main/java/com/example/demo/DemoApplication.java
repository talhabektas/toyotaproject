package com.example.demo;


import com.example.demo.config.SimulatorConfig;
import com.example.demo.service.RateSimulationService;
import com.example.demo.service.TCPConnectionHandler;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * TCP Platform Simülatörü Ana Uygulama Sınıfı
 */
public class DemoApplication{
    private static final Logger logger = LogManager.getLogger(DemoApplication.class);

    private final SimulatorConfig config;
    private final RateSimulationService simulationService;
    private ServerSocket serverSocket;
    private ExecutorService connectionPool;
    private boolean running = false;

    /**
     * Constructor
     * @param config Simülatör konfigürasyonu
     */
    public DemoApplication(SimulatorConfig config) {
        this.config = config;
        this.simulationService = new RateSimulationService(config);
    }

    /**
     * Uygulamayı başlatır
     */
    public void start() {
        if (running) {
            logger.warn("Application is already running");
            return;
        }

        try {
            // Simülasyon servisini başlat
            simulationService.start();

            // TCP sunucusunu başlat
            serverSocket = new ServerSocket(config.getPort());
            connectionPool = Executors.newFixedThreadPool(config.getThreadPoolSize());

            running = true;
            logger.info("TCP Platform Simulator started on port {}", config.getPort());

            // Bağlantıları dinle
            acceptConnections();

        } catch (IOException e) {
            logger.error("Failed to start TCP server", e);
            stop();
        }
    }

    /**
     * İstemci bağlantılarını kabul eder
     */
    private void acceptConnections() {
        new Thread(() -> {
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("New client connected: {}", clientSocket.getRemoteSocketAddress());

                    // Yeni bağlantıyı işlemek için bir thread başlat
                    TCPConnectionHandler handler = new TCPConnectionHandler(clientSocket, simulationService);
                    connectionPool.execute(handler);

                } catch (IOException e) {
                    if (running) {
                        logger.error("Error accepting client connection", e);
                    }
                }
            }
        }, "TCPConnectionAcceptor").start();
    }

    /**
     * Uygulamayı durdurur
     */
    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        logger.info("Stopping TCP Platform Simulator...");

        // Simülasyon servisini durdur
        simulationService.stop();

        // Server soketi kapat
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                logger.info("Server socket closed");
            } catch (IOException e) {
                logger.error("Error closing server socket", e);
            }
        }

        // Thread havuzunu kapat
        if (connectionPool != null) {
            connectionPool.shutdown();
            try {
                if (!connectionPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    connectionPool.shutdownNow();
                }
                logger.info("Connection pool shutdown completed");
            } catch (InterruptedException e) {
                connectionPool.shutdownNow();
                Thread.currentThread().interrupt();
                logger.error("Connection pool shutdown interrupted", e);
            }
        }

        logger.info("TCP Platform Simulator stopped");
    }

    /**
     * Ana metod
     * @param args Komut satırı argümanları
     */
    public static void main(String[] args) {
        // Komut satırı argümanlarını işle
        CommandLineParser parser = new DefaultParser();
        Options options = getCommandLineOptions();

        try {
            CommandLine cmd = parser.parse(options, args);

            // Yapılandırma dosyası yollarını al
            String propertiesFile = cmd.getOptionValue("config", "config/application.properties");
            String ratesFile = cmd.getOptionValue("rates", "config/rates-config.json");

            // Dosyaların varlığını kontrol et
            checkFileExists(propertiesFile);
            checkFileExists(ratesFile);

            // Konfigürasyonu yükle
            SimulatorConfig config = new SimulatorConfig();
            config.loadFromProperties(propertiesFile);
            config.loadRatesFromJson(ratesFile);

            // Uygulamayı başlat
            DemoApplication app = new DemoApplication(config);
            app.start();

            // Kapatma kancası (shutdown hook) ekle
            Runtime.getRuntime().addShutdownHook(new Thread(app::stop));

        } catch (ParseException e) {
            logger.error("Failed to parse command line arguments", e);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("tcp-simulator", options);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Application startup failed", e);
            System.exit(1);
        }
    }

    /**
     * Komut satırı seçeneklerini tanımlar
     * @return Komut satırı seçenekleri
     */
    private static Options getCommandLineOptions() {
        Options options = new Options();

        Option configOption = Option.builder("c")
                .longOpt("config")
                .argName("file")
                .hasArg()
                .desc("Properties dosyasının yolu (varsayılan: config/application.properties)")
                .build();

        Option ratesOption = Option.builder("r")
                .longOpt("rates")
                .argName("file")
                .hasArg()
                .desc("Kurlar JSON dosyasının yolu (varsayılan: config/rates-config.json)")
                .build();

        options.addOption(configOption);
        options.addOption(ratesOption);

        return options;
    }

    /**
     * Dosyanın var olup olmadığını kontrol eder
     * @param filePath Dosya yolu
     */
    private static void checkFileExists(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            logger.error("File does not exist: {}", filePath);
            throw new RuntimeException("Required file not found: " + filePath);
        }
    }
}
