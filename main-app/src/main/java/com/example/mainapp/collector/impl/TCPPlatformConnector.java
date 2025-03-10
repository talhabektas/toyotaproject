package com.example.mainapp.collector.impl;


import com.example.mainapp.collector.DataCollector;
import com.example.mainapp.model.Rate;
import com.example.mainapp.model.RateFields;
import com.example.mainapp.model.RateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TCP platform connection manager class
 */
public class TCPPlatformConnector extends DataCollector {

    private static final Logger logger = LoggerFactory.getLogger(TCPPlatformConnector.class);

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final BlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();

    private static final Pattern RATE_PATTERN = Pattern.compile("([^|]+)\\|22:number:([^|]+)\\|25:number:([^|]+)\\|5:timestamp:(.+)");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Constructor
     * @param platformName Platform name
     * @param config Platform configuration
     */
    public TCPPlatformConnector(String platformName, Properties config) {
        super(platformName, config);
    }

    @Override
    public boolean connect(String platformName, String userid, String password) {
        if (this.socket != null && this.socket.isConnected()) {
            logger.warn("Already connected to platform: {}", platformName);
            return false;
        }

        try {
            String host = config.getProperty("tcp.host", "localhost");
            int port = Integer.parseInt(config.getProperty("tcp.port", "8081"));

            logger.info("Connecting to TCP server {}:{} for platform {}", host, port, platformName);

            this.socket = new Socket(host, port);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            logger.info("Connected to TCP server for platform {}", platformName);

            if (callback != null) {
                callback.onConnect(platformName, true);
            }

            return true;
        } catch (IOException e) {
            logger.error("Failed to connect to platform: {}", platformName, e);

            if (callback != null) {
                callback.onConnect(platformName, false);
            }

            return false;
        }
    }

    @Override
    public boolean disconnect(String platformName, String userid, String password) {
        if (this.socket == null || this.socket.isClosed()) {
            logger.warn("Not connected to platform: {}", platformName);
            return false;
        }

        try {
            // Close connection
            out.close();
            in.close();
            socket.close();

            socket = null;
            out = null;
            in = null;

            logger.info("Disconnected from platform: {}", platformName);

            if (callback != null) {
                callback.onDisConnect(platformName, true);
            }

            return true;
        } catch (IOException e) {
            logger.error("Error disconnecting from platform: {}", platformName, e);

            if (callback != null) {
                callback.onDisConnect(platformName, false);
            }

            return false;
        }
    }

    @Override
    public boolean subscribe(String platformName, String rateName) {
        if (out == null || socket == null || !socket.isConnected()) {
            logger.error("Cannot subscribe - not connected to platform: {}", platformName);
            return false;
        }

        try {
            commandQueue.put("subscribe|" + rateName);
            return true;
        } catch (InterruptedException e) {
            logger.error("Interrupted while adding subscribe command to queue", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean unsubscribe(String platformName, String rateName) {
        if (out == null || socket == null || !socket.isConnected()) {
            logger.error("Cannot unsubscribe - not connected to platform: {}", platformName);
            return false;
        }

        try {
            commandQueue.put("unsubscribe|" + rateName);
            return true;
        } catch (InterruptedException e) {
            logger.error("Interrupted while adding unsubscribe command to queue", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void run() {
        connect(platformName, null, null);

        try {
            // Command sending thread
            Thread commandThread = new Thread(() -> {
                try {
                    while (running.get() && !Thread.currentThread().isInterrupted()) {
                        String command = commandQueue.poll(1, TimeUnit.SECONDS);
                        if (command != null && out != null) {
                            logger.debug("Sending command: {}", command);
                            out.println(command);
                        }
                    }
                } catch (InterruptedException e) {
                    logger.info("Command thread interrupted");
                    Thread.currentThread().interrupt();
                }
            }, "TCPCommandSender-" + platformName);

            commandThread.setDaemon(true);
            commandThread.start();

            // Response reading
            String responseLine;
            while (running.get() && !Thread.currentThread().isInterrupted() &&
                    in != null && (responseLine = in.readLine()) != null) {

                logger.debug("Received: {}", responseLine);

                if (responseLine.startsWith("ERROR|")) {
                    logger.error("Error from platform {}: {}", platformName, responseLine.substring(6));
                    continue;
                }

                if (responseLine.startsWith("Subscribed to ")) {
                    String rateName = responseLine.substring("Subscribed to ".length());
                    handleSubscriptionResult(rateName, true);
                    continue;
                }

                if (responseLine.startsWith("Unsubscribed from ")) {
                    String rateName = responseLine.substring("Unsubscribed from ".length());
                    handleUnsubscriptionResult(rateName, true);
                    continue;
                }

                // Rate data
                Matcher matcher = RATE_PATTERN.matcher(responseLine);
                if (matcher.matches()) {
                    String rateName = matcher.group(1);
                    double bid = Double.parseDouble(matcher.group(2));
                    double ask = Double.parseDouble(matcher.group(3));
                    LocalDateTime timestamp = LocalDateTime.parse(matcher.group(4), TIMESTAMP_FORMATTER);

                    // Notify coordinator
                    if (callback != null) {
                        if (!subscribedRates.contains(rateName)) {
                            // First data
                            Rate rate = new Rate(rateName, platformName, bid, ask, timestamp, false);
                            callback.onRateAvailable(platformName, rateName, rate);

                            logger.debug("Rate available - {}: {}", rateName, rate);
                        } else {
                            // Update
                            RateFields rateFields = new RateFields(bid, ask, timestamp);
                            callback.onRateUpdate(platformName, rateName, rateFields);

                            logger.debug("Rate update - {}: {}", rateName, rateFields);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error reading from TCP socket for platform: {}", platformName, e);

            if (callback != null) {
                callback.onDisConnect(platformName, false);

                // Notify UNAVAILABLE status for all rates
                subscribedRates.forEach(rateName ->
                        callback.onRateStatus(platformName, rateName, RateStatus.UNAVAILABLE));
            }
        } finally {
            disconnect(platformName, null, null);
        }
    }
}