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
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
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

    private String host;
    private int port;
    private int retryCount;
    private long retryIntervalMs;
    private int connectTimeout;

    private static final Pattern RATE_PATTERN = Pattern.compile("([^|]+)\\|22:number:([^|]+)\\|25:number:([^|]+)\\|5:timestamp:(.+)");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Constructor
     * @param platformName Platform name
     * @param config Platform configuration
     */
    public TCPPlatformConnector(String platformName, Properties config) {
        super(platformName, config);

        this.host = config.getProperty("tcp.host", "tcp-simulator");
        this.port = Integer.parseInt(config.getProperty("tcp.port", "8081"));
        this.retryCount = Integer.parseInt(config.getProperty("connection.retryCount", "10"));
        this.retryIntervalMs = Long.parseLong(config.getProperty("connection.retryIntervalMs", "15000"));
        this.connectTimeout = Integer.parseInt(config.getProperty("connection.timeoutMs", "30000"));

        logger.info("TCPPlatformConnector initialized for {} with host={}, port={}",
                platformName, host, port);
    }

    @Override
    public boolean connect(String platformName, String userid, String password) {
        if (this.socket != null && this.socket.isConnected() && !this.socket.isClosed()) {
            logger.info("Already connected to platform: {}", platformName);
            return true;
        }

        int attempts = 0;
        boolean connected = false;

        while (attempts < retryCount && !connected) {
            attempts++;
            try {
                logger.info("Connecting to TCP server {}:{} for platform {} (attempt {}/{})",
                        host, port, platformName, attempts, retryCount);

                this.socket = new Socket(host, port);
                this.socket.setSoTimeout(connectTimeout); // Read timeout
                this.out = new PrintWriter(socket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                connected = true;
                logger.info("Successfully connected to TCP server for platform {}", platformName);

                if (callback != null) {
                    callback.onConnect(platformName, true);
                }

                // Connection successful, reset last response time
                updateLastResponseTime();

                return true;
            } catch (ConnectException e) {
                logger.error("Connection refused to TCP server {}:{} for platform {} (attempt {}/{})",
                        host, port, platformName, attempts, retryCount);

                if (attempts >= retryCount) {
                    logger.error("Failed to connect to platform after {} attempts: {}", retryCount, platformName);

                    if (callback != null) {
                        callback.onConnect(platformName, false);
                    }

                    return false;
                }

                try {
                    logger.info("Waiting {} ms before retry...", retryIntervalMs);
                    Thread.sleep(retryIntervalMs);
                } catch (InterruptedException ie) {
                    logger.error("Interrupted while waiting to retry connection", ie);
                    Thread.currentThread().interrupt();
                    return false;
                }
            } catch (IOException e) {
                logger.error("Error connecting to platform: {} - {}", platformName, e.getMessage(), e);

                if (callback != null) {
                    callback.onConnect(platformName, false);
                }

                return false;
            }
        }

        // This should not be reached if the loop exits normally
        return connected;
    }

    @Override
    public boolean disconnect(String platformName, String userid, String password) {
        if (this.socket == null || this.socket.isClosed()) {
            logger.warn("Not connected to platform: {}", platformName);
            return true;
        }

        try {
            // Close connection
            if (out != null) {
                out.close();
                out = null;
            }

            if (in != null) {
                in.close();
                in = null;
            }

            if (socket != null) {
                socket.close();
                socket = null;
            }

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
        if (!isConnected()) {
            logger.error("Cannot subscribe - not connected to platform: {}", platformName);

            // Bağlantıyı otomatik olarak yeniden kurmayı dene
            boolean reconnected = connect(platformName, null, null);
            if (!reconnected) {
                return false;
            }
        }

        try {
            commandQueue.put("subscribe|" + rateName);
            logger.info("Added subscribe command to queue for rate: {}", rateName);
            return true;
        } catch (InterruptedException e) {
            logger.error("Interrupted while adding subscribe command to queue", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean unsubscribe(String platformName, String rateName) {
        if (!isConnected()) {
            logger.error("Cannot unsubscribe - not connected to platform: {}", platformName);
            return false;
        }

        try {
            commandQueue.put("unsubscribe|" + rateName);
            logger.info("Added unsubscribe command to queue for rate: {}", rateName);
            return true;
        } catch (InterruptedException e) {
            logger.error("Interrupted while adding unsubscribe command to queue", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected() && out != null && in != null;
    }

    @Override
    public void run() {
        connect(platformName, null, null);

        if (!isConnected()) {
            logger.error("Could not establish initial connection to platform: {}", platformName);
            // Don't exit yet - keep retrying
        }

        try {
            // Command sending thread
            Thread commandThread = new Thread(() -> {
                try {
                    while (running.get() && !Thread.currentThread().isInterrupted()) {
                        String command = commandQueue.poll(1, TimeUnit.SECONDS);
                        if (command != null) {
                            if (isConnected()) {
                                logger.debug("Sending command: {}", command);
                                out.println(command);

                                // Update last response time for sending commands too
                                updateLastResponseTime();
                            } else {
                                logger.warn("Cannot send command as connection is closed: {}", command);

                                // Bağlantı kopmuş, yeniden kurmayı dene
                                boolean reconnected = connect(platformName, null, null);
                                if (reconnected) {
                                    // Yeniden bağlantı kuruldu, komutu tekrar kuyruğa ekle
                                    commandQueue.put(command);
                                } else {
                                    // Yeniden bağlantı kurulamadı, abonelik durumlarını güncelle
                                    if (command.startsWith("subscribe|")) {
                                        String rateName = command.substring("subscribe|".length());
                                        handleSubscriptionResult(rateName, false);

                                        if (callback != null) {
                                            callback.onRateStatus(platformName, rateName, RateStatus.UNAVAILABLE);
                                        }
                                    }
                                }
                            }
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
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    if (!isConnected()) {
                        // Bağlantı kopmuş, yeniden kurmayı dene
                        boolean reconnected = connect(platformName, null, null);
                        if (!reconnected) {
                            // Yeniden bağlantı kurulamadı, biraz bekle ve tekrar dene
                            Thread.sleep(retryIntervalMs);
                            continue;
                        }
                    }

                    String responseLine = in.readLine();
                    if (responseLine == null) {
                        logger.warn("TCP connection closed by server");
                        disconnect(platformName, null, null);

                        // Try to reconnect
                        boolean reconnected = connect(platformName, null, null);
                        if (!reconnected) {
                            // Yeniden bağlantı kurulamadı, biraz bekle ve tekrar dene
                            Thread.sleep(retryIntervalMs);
                        }

                        continue;
                    }

                    // Update last response time
                    updateLastResponseTime();

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
                } catch (SocketTimeoutException e) {
                    logger.debug("Socket read timeout - this is normal for heartbeat checking");
                    // Update last response time for timeouts too
                    updateLastResponseTime();
                } catch (IOException e) {
                    logger.error("Error reading from TCP socket for platform: {}", platformName, e);

                    if (callback != null) {
                        callback.onDisConnect(platformName, false);

                        // Notify UNAVAILABLE status for all rates
                        subscribedRates.forEach(rateName ->
                                callback.onRateStatus(platformName, rateName, RateStatus.UNAVAILABLE));
                    }

                    // Try to reconnect
                    disconnect(platformName, null, null);
                    boolean reconnected = connect(platformName, null, null);
                    if (!reconnected) {
                        // Wait before retrying
                        try {
                            Thread.sleep(retryIntervalMs);
                        } catch (InterruptedException ie) {
                            logger.info("Interrupted while waiting to reconnect");
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        // Resubscribe to all rates
                        for (String rateName : subscribedRates) {
                            subscribe(platformName, rateName);
                        }
                    }
                } catch (InterruptedException e) {
                    logger.info("TCP platform connector thread interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            disconnect(platformName, null, null);
        }
    }
}