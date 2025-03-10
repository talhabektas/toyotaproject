package com.example.demo.service;


import com.example.demo.model.RateData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP bağlantılarını işleyen sınıf
 */
public class TCPConnectionHandler implements Runnable {
    private static final Logger logger = LogManager.getLogger(TCPConnectionHandler.class);

    private final Socket clientSocket;
    private final RateSimulationService simulationService;
    private final String connectionId;
    private PrintWriter out;
    private BufferedReader in;
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * Constructor
     * @param clientSocket İstemci soketi
     * @param simulationService Simülasyon servisi
     */
    public TCPConnectionHandler(Socket clientSocket, RateSimulationService simulationService) {
        this.clientSocket = clientSocket;
        this.simulationService = simulationService;
        this.connectionId = UUID.randomUUID().toString();
    }

    @Override
    public void run() {
        try {
            // Input ve output stream'leri oluştur
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            logger.info("New connection established: {}", connectionId);

            String inputLine;
            // İstemciden gelen komutları işle
            while (running.get() && (inputLine = in.readLine()) != null) {
                handleCommand(inputLine);
            }

        } catch (IOException e) {
            logger.error("Error handling client connection: {}", connectionId, e);
        } finally {
            cleanup();
        }
    }

    /**
     * İstemci komutlarını işler
     * @param command İstemciden gelen komut
     */
    private void handleCommand(String command) {
        logger.debug("Received command: {}", command);

        // Komut formatını kontrol et
        String[] parts = command.split("\\|");
        if (parts.length < 1) {
            sendErrorMessage("Invalid request format");
            return;
        }

        String action = parts[0].toLowerCase();

        switch (action) {
            case "subscribe":
                handleSubscribe(parts);
                break;
            case "unsubscribe":
                handleUnsubscribe(parts);
                break;
            case "list":
                handleListRates();
                break;
            case "quit":
            case "exit":
                handleQuit();
                break;
            default:
                sendErrorMessage("Invalid request format");
                break;
        }
    }

    /**
     * Kur'a abone olma komutunu işler
     * @param parts Komut parçaları
     */
    private void handleSubscribe(String[] parts) {
        if (parts.length < 2) {
            sendErrorMessage("Invalid subscribe format. Use: subscribe|RATENAME");
            return;
        }

        String rateName = parts[1];
        boolean success = simulationService.subscribe(connectionId, rateName, this::sendRateUpdate);

        if (success) {
            out.println("Subscribed to " + rateName);
        } else {
            sendErrorMessage("Rate data not found for " + rateName);
        }
    }

    /**
     * Kur aboneliğini iptal etme komutunu işler
     * @param parts Komut parçaları
     */
    private void handleUnsubscribe(String[] parts) {
        if (parts.length < 2) {
            sendErrorMessage("Invalid unsubscribe format. Use: unsubscribe|RATENAME");
            return;
        }

        String rateName = parts[1];
        boolean success = simulationService.unsubscribe(connectionId, rateName);

        if (success) {
            out.println("Unsubscribed from " + rateName);
        } else {
            sendErrorMessage("Not subscribed to " + rateName);
        }
    }

    /**
     * Mevcut kurları listeleme komutunu işler
     */
    private void handleListRates() {
        StringBuilder response = new StringBuilder("Available rates:\n");
        simulationService.getAllRates().keySet().forEach(rate ->
                response.append(rate).append("\n"));
        out.println(response.toString());
    }

    /**
     * Çıkış komutunu işler
     */
    private void handleQuit() {
        out.println("Goodbye!");
        running.set(false);
    }

    /**
     * Kur güncellemelerini istemciye gönderir
     * @param rateData Güncel kur verisi
     */
    private void sendRateUpdate(RateData rateData) {
        if (out != null && !clientSocket.isClosed()) {
            out.println(rateData.toTcpProtocolString());
        }
    }

    /**
     * Hata mesajı gönderir
     * @param message Hata mesajı
     */
    private void sendErrorMessage(String message) {
        out.println("ERROR|" + message);
    }

    /**
     * Kaynakları temizler
     */
    private void cleanup() {
        logger.info("Cleaning up connection: {}", connectionId);

        // Tüm abonelikleri iptal et
        simulationService.unsubscribeAll(connectionId);

        // Stream'leri kapat
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error during cleanup for connection: {}", connectionId, e);
        }
    }

    /**
     * Bağlantıyı kapatmak için kullanılır
     */
    public void close() {
        running.set(false);
        cleanup();
    }
}