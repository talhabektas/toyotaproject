package com.example.demo.service;

import com.example.demo.model.RateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCPConnectionHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TCPConnectionHandler.class);

    private final Socket clientSocket;
    private final RateSimulationService simulationService;
    private final String connectionId;
    private PrintWriter out;
    private BufferedReader in;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public TCPConnectionHandler(Socket clientSocket, RateSimulationService simulationService) {
        this.clientSocket = clientSocket;
        this.simulationService = simulationService;
        this.connectionId = UUID.randomUUID().toString();
    }

    @Override
    public void run() {
        try {
            // Create input and output streams
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            logger.info("New connection established: {}", connectionId);

            String inputLine;
            // Process commands from client
            while (running.get() && (inputLine = in.readLine()) != null) {
                logger.debug("Received command: {}", inputLine);
                handleCommand(inputLine);
            }

        } catch (IOException e) {
            logger.error("Error handling client connection: {}", connectionId, e);
        } finally {
            cleanup();
        }
    }

    private void handleCommand(String command) {
        // Check command format
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

    private void handleSubscribe(String[] parts) {
        if (parts.length < 2) {
            sendErrorMessage("Invalid subscribe format. Use: subscribe|RATENAME");
            return;
        }

        String rateName = parts[1];
        boolean success = simulationService.subscribe(connectionId, rateName, this::sendRateUpdate);

        if (success) {
            out.println("Subscribed to " + rateName);
            logger.info("Client {} subscribed to rate {}", connectionId, rateName);
        } else {
            sendErrorMessage("Rate data not found for " + rateName);
        }
    }

    private void handleUnsubscribe(String[] parts) {
        if (parts.length < 2) {
            sendErrorMessage("Invalid unsubscribe format. Use: unsubscribe|RATENAME");
            return;
        }

        String rateName = parts[1];
        boolean success = simulationService.unsubscribe(connectionId, rateName);

        if (success) {
            out.println("Unsubscribed from " + rateName);
            logger.info("Client {} unsubscribed from rate {}", connectionId, rateName);
        } else {
            sendErrorMessage("Not subscribed to " + rateName);
        }
    }

    private void handleListRates() {
        StringBuilder response = new StringBuilder("Available rates:\n");
        simulationService.getAllRates().keySet().forEach(rate ->
                response.append(rate).append("\n"));
        out.println(response.toString());
        logger.debug("Sent rates list to client {}", connectionId);
    }

    private void handleQuit() {
        out.println("Goodbye!");
        running.set(false);
        logger.info("Client {} requested to quit", connectionId);
    }

    private void sendRateUpdate(RateData rateData) {
        if (out != null && !clientSocket.isClosed()) {
            out.println(rateData.toTcpProtocolString());
            logger.debug("Sent rate update to client {}: {}", connectionId, rateData.getRateName());
        }
    }

    private void sendErrorMessage(String message) {
        out.println("ERROR|" + message);
        logger.debug("Sent error message to client {}: {}", connectionId, message);
    }

    private void cleanup() {
        logger.info("Cleaning up connection: {}", connectionId);

        // Unsubscribe from all rates
        simulationService.unsubscribeAll(connectionId);

        // Close streams
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

    public void close() {
        running.set(false);
        cleanup();
    }
}