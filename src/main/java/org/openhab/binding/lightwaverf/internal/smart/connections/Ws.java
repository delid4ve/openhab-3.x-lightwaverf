package org.openhab.binding.lightwaverf.internal.smart.connections;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.smarthome.io.net.http.WebSocketFactory;
import org.openhab.binding.lightwaverf.internal.smart.dto.websocket.InitialStatus;
import org.openhab.binding.lightwaverf.internal.smart.listeners.WsEventListener;
import org.openhab.binding.lightwaverf.internal.smart.queues.MessageSender;
import org.openhab.binding.lightwaverf.internal.smart.queues.WsMessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

@WebSocket
public class Ws {

    private final Logger logger = LoggerFactory.getLogger(Ws.class);
    private final WebSocketClient webSocketClient;
    private final Gson gson;
    private Session session;
    private final WsEventListener listener;
    private final String uuid = UUID.randomUUID().toString();
    private final String url = "wss://v1-linkplus-app.lightwaverf.com";
    private int transactionId = 1;
    private final String deviceUuid = UUID.randomUUID().toString();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private final MessageSender sender;

    public Ws(WebSocketFactory webSocketFactory, WsEventListener listener, Gson gson) {
        this.webSocketClient = webSocketFactory.createWebSocketClient("LightwaveWebSocket");
        this.webSocketClient.setMaxIdleTimeout(86400000);
        this.listener = listener;
        this.gson = gson;
        sender = new MessageSender(this, gson);
    }

    public synchronized void start() {
        try {
            webSocketClient.start();
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            URI uri = new URI(url);
            webSocketClient.setAsyncWriteTimeout(10000);
            webSocketClient.setMaxTextMessageBufferSize(10000);
            webSocketClient.connect(this, uri, request);
            executor.scheduleWithFixedDelay(sender, 0, 100, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
        }
    }

    // public void addOkListener(MessageSender sender) {
    // okListeners.put(sender, sender);
    // }

    // public void removeOkListener(MessageSender sender) {
    // okListeners.remove(sender);
    // }

    public void stop() {
        sender.stopRunning();
        executor.shutdown();
        try {
            webSocketClient.stop();
        } catch (Exception e) {
            logger.debug("Error while closing connection", e);
        }
        webSocketClient.destroy();
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    /*
     * public void getStatus(String id, String classType, String readType) {
     * WsRead readStatus = new WsRead();
     * readStatus.setClassType(classType);
     * readStatus.setId(id);
     * readStatus.setTransactionId(transactionId);
     * readStatus.setUuid(uuid);
     * readStatus.setWriteType(readType);
     * logger.debug("LightwaveRF - Get Status Message: {}", readStatus.getString());
     * sendText(readStatus.getString());
     * }
     */

    /*
     * public void setStatus(String id, String classType, String value, String writeType) {
     * WsWrite writeStatus = new WsWrite();
     * writeStatus.setClassType(classType);
     * writeStatus.setId(id);
     * writeStatus.setTransactionId(transactionId);
     * writeStatus.setUuid(uuid);
     * writeStatus.setValue(value);
     * writeStatus.setWriteType(writeType);
     * logger.debug("LightwaveRF - Sending nee value to websocket, enable trace logging for full message");
     * logger.trace("LightwaveRF - Write Value Message: {}", writeStatus.getString());
     * sendText(writeStatus.getString());
     * }
     */

    /*
     * public void login(String token) {
     * logger.debug("LightwaveRF - Logging in to WebSocket");
     * WsAuthenticate authenticate = new WsAuthenticate();
     * authenticate.setTransactionId(transactionId);
     * authenticate.setToken(token);
     * authenticate.setDeviceUuid(deviceUuid);
     * authenticate.setUuid(uuid);
     * logger.debug(
     * "LightwaveRF - Sending Authentication message to websocket, enable trace logging for full message");
     * logger.trace("LightwaveRF - Authenticate Websocket Message: {}", authenticate.getString());
     * sendText(authenticate.getString());
     * }
     */

    public void login(String token) {
        InitialStatus initialStatus = new InitialStatus();
        initialStatus.setVersion(1);
        initialStatus.setClass_("user");
        initialStatus.setOperation("authenticate");
        initialStatus.setDirection("request");
        List<InitialStatus.Item> items = new ArrayList<>();
        InitialStatus.Item item = initialStatus.new Item();
        InitialStatus.Item.Payload payload = item.new Payload();
        payload.setToken(token);
        payload.setClientDeviceId(deviceUuid);
        item.setItemId("0");
        item.setPayload(payload);
        items.add(item);
        initialStatus.setItems(items);
        initialStatus.setSenderId(uuid);
        initialStatus.setTransactionId(transactionId);
        transactionId++;
        send(gson.toJson(initialStatus));

        // getStatuses(initialStatus);
    }

    public void getInitalStatus(InitialStatus initialStatus) {
        initialStatus.setSenderId(uuid);
        initialStatus.setTransactionId(transactionId);
        transactionId++;
        send(gson.toJson(initialStatus));
    }

    public synchronized void sendCommand(InitialStatus initialStatus) {
        initialStatus.setSenderId(uuid);
        initialStatus.setTransactionId(transactionId);
        transactionId++;
        WsMessageQueue queue = new WsMessageQueue();
        queue.setInitialStatus(initialStatus);
        queue.setTransactionid(Integer.parseInt(initialStatus.getItems().get(0).getItemId()));
        sender.sendCommand(queue);
    }

    public synchronized void send(String message) {
        logger.debug("Message sent: {}", message);
        session.getRemote().sendString(message, new WriteCallback() {
            @Override
            public void writeSuccess() {
                logger.debug("LightwaveRF - Websocket message sending completed: {}", message);
            }

            @Override
            public void writeFailed(Throwable x) {
                logger.warn("LightwaveRF - Websocket message sending failed: {} with reason: {}", message, x);
            }
        });
    }

    @OnWebSocketConnect
    public void onConnect(Session wssession) {
        session = wssession;
        logger.debug("LightwaveRF - WebSocket Socket {} successfully connected to {}", this,
                session.getRemoteAddress().getAddress());
        listener.updateConnectionState(true);
        logger.debug("LightwaveRF - WebSocket connection open");
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        InitialStatus rm = new InitialStatus();
        rm = gson.fromJson(message, InitialStatus.class);
        if (rm.getDirection().equals("response")) {
            logger.debug("LightwaveRF - Response received for sent Command, TransactionID: {}",
                    rm.getItems().get(0).getItemId());
            sender.onMessage(rm.getItems().get(0).getItemId());
        } else {
            logger.debug("LightwaveRF - Notification received, Sending to Device: {}", message);
            listener.onMessage(message);
        }
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        logger.warn("LightwaveRF - Closing a WebSocket due to {}", reason);
        listener.updateConnectionState(false);
        logger.warn("LightwaveRF - WebSocket connection closed");
    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        onClose(0, cause.getMessage());
    }
}
