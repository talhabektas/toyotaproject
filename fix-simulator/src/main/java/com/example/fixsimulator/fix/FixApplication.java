package com.example.fixsimulator.fix;

import com.example.fixsimulator.model.RateData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.MarketDataSnapshotFullRefresh;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FixApplication implements Application {

    private static final Logger logger = LogManager.getLogger(FixApplication.class);

    private Map<String, RateData> rateDataMap = new ConcurrentHashMap<>();
    private final Map<SessionID, Map<String, Boolean>> sessionSubscriptions = new ConcurrentHashMap<>();

    public FixApplication() {
        logger.info("FixApplication başlatıldı");
    }

    public void setRateData(Map<String, RateData> rateDataMap) {
        this.rateDataMap = rateDataMap;
    }

    public void publishRateUpdate(RateData rateData) {
        sessionSubscriptions.forEach((sessionId, subscriptions) -> {
            if (subscriptions.containsKey(rateData.getRateName())) {
                try {
                    Session session = Session.lookupSession(sessionId);
                    if (session != null && session.isLoggedOn()) {
                        Message message = createMarketDataMessage(rateData);
                        session.send(message);
                        logger.debug("Sent rate update for {} to session {}", rateData.getRateName(), sessionId);
                    }
                } catch (Exception e) {
                    logger.error("Error sending rate update to session {}", sessionId, e);
                }
            }
        });
    }

    private Message createMarketDataMessage(RateData rateData) throws FieldNotFound {
        MarketDataSnapshotFullRefresh message = new MarketDataSnapshotFullRefresh();

        message.set(new Symbol(rateData.getRateName()));
        message.set(new MDReqID(rateData.getRateName()));

        MarketDataSnapshotFullRefresh.NoMDEntries bidGroup = new MarketDataSnapshotFullRefresh.NoMDEntries();
        bidGroup.set(new MDEntryType(MDEntryType.BID));
        bidGroup.set(new MDEntryPx(rateData.getBid()));
        bidGroup.set(new MDEntryTime(convertToLocalTime(rateData.getTimestampAsDate())));
        bidGroup.set(new MDEntryDate(convertToLocalDate(rateData.getTimestampAsDate())));

        MarketDataSnapshotFullRefresh.NoMDEntries askGroup = new MarketDataSnapshotFullRefresh.NoMDEntries();
        askGroup.set(new MDEntryType(MDEntryType.OFFER));
        askGroup.set(new MDEntryPx(rateData.getAsk()));
        askGroup.set(new MDEntryTime(convertToLocalTime(rateData.getTimestampAsDate())));
        askGroup.set(new MDEntryDate(convertToLocalDate(rateData.getTimestampAsDate())));

        message.addGroup(bidGroup);
        message.addGroup(askGroup);

        return message;
    }

    private LocalDate convertToLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalTime convertToLocalTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
    }

    @Override
    public void onCreate(SessionID sessionId) {
        logger.info("Session oluşturuldu: {}", sessionId);
        sessionSubscriptions.put(sessionId, new ConcurrentHashMap<>());
    }

    @Override
    public void onLogon(SessionID sessionId) {
        logger.info("Session oturum açtı: {}", sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        logger.info("Session oturumu kapattı: {}", sessionId);
        sessionSubscriptions.remove(sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        logger.debug("Admin mesajı gönderiliyor: {}", message);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        logger.debug("Admin mesajı alındı: {}", message);
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        logger.debug("Uygulama mesajı gönderiliyor: {} to {}", message, sessionId);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        logger.debug("Uygulama mesajı alındı: {} from {}", message, sessionId);

        try {
            String msgType = message.getHeader().getString(MsgType.FIELD);

            if (msgType.equals(MsgType.MARKET_DATA_REQUEST)) {
                handleMarketDataRequest((MarketDataRequest) message, sessionId);
            }
        } catch (Exception e) {
            logger.error("Mesaj işlenirken hata oluştu: {}", message, e);
        }
    }

    private void handleMarketDataRequest(MarketDataRequest request, SessionID sessionId) throws FieldNotFound {
        String mdReqID = request.getMDReqID().getValue();
        int subscriptionType = request.getSubscriptionRequestType().getValue();

        // Fix for the error: directly use the integer constants instead of the undefined fields
        if (subscriptionType == 1) { // Use 1 instead of SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES
            int symbolCount = request.getNoRelatedSym().getValue();
            for (int i = 1; i <= symbolCount; i++) {
                MarketDataRequest.NoRelatedSym symbolGroup = new MarketDataRequest.NoRelatedSym();
                request.getGroup(i, symbolGroup);

                String symbol = symbolGroup.getSymbol().getValue();

                Map<String, Boolean> subscriptions = sessionSubscriptions.get(sessionId);
                if (subscriptions != null) {
                    subscriptions.put(symbol, Boolean.TRUE);
                    logger.info("Session {} subscribed to symbol {}", sessionId, symbol);

                    RateData rateData = rateDataMap.get(symbol);
                    if (rateData != null) {
                        try {
                            Message snapshot = createMarketDataMessage(rateData);
                            Session.sendToTarget(snapshot, sessionId);
                            logger.debug("Sent initial snapshot for {} to session {}", symbol, sessionId);
                        } catch (Exception e) {
                            logger.error("Error sending initial snapshot for {} to session {}", symbol, sessionId, e);
                        }
                    } else {
                        logger.warn("No data available for requested symbol: {}", symbol);
                    }
                }
            }
        } else if (subscriptionType == 2) { // Use 2 instead of SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST
            int symbolCount = request.getNoRelatedSym().getValue();
            for (int i = 1; i <= symbolCount; i++) {
                MarketDataRequest.NoRelatedSym symbolGroup = new MarketDataRequest.NoRelatedSym();
                request.getGroup(i, symbolGroup);

                String symbol = symbolGroup.getSymbol().getValue();

                Map<String, Boolean> subscriptions = sessionSubscriptions.get(sessionId);
                if (subscriptions != null) {
                    subscriptions.remove(symbol);
                    logger.info("Session {} unsubscribed from symbol {}", sessionId, symbol);
                }
            }
        }
    }
}