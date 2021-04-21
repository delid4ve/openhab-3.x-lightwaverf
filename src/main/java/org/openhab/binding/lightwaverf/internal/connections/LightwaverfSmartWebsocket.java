/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.lightwaverf.internal.connections;

import java.net.URI;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.openhab.binding.lightwaverf.internal.listeners.LightwaverfSmartListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which
 * are used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */
@NonNullByDefault
@WebSocket
public class LightwaverfSmartWebsocket {

    private final Logger logger = LoggerFactory.getLogger(LightwaverfSmartWebsocket.class);
    private final WebSocketClient webSocketClient;
    private final LightwaverfSmartListener listener;
    private final String url = "wss://v1-linkplus-app.lightwaverf.com";

    private Boolean connected = false;
    private Boolean closing = false;

    private @Nullable Session session;

    public LightwaverfSmartWebsocket(WebSocketClient webSocketClient, LightwaverfSmartListener listener) {
        this.webSocketClient = webSocketClient;
        this.listener = listener;
    }

    public Boolean getConnected() {
        return this.connected;
    }

    public void setConnected(Boolean connected) {
        this.connected = connected;
    }

    public synchronized void start() {
        try {
            closing = false;
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            URI uri = new URI(url);

            webSocketClient.setConnectTimeout(1000);

            webSocketClient.setAsyncWriteTimeout(5000);
            // make sure our message buffer is large enough
            webSocketClient.setMaxTextMessageBufferSize(1024 * 1024);
            // if the websocket doesnt receive any data then it disconnects
            webSocketClient.setMaxIdleTimeout(1500000000);
            webSocketClient.connect(this, uri, request);
        } catch (Exception e) {
        }
    }

    public void stop() {
        closing = true;
        logger.debug("Stopping websocket client");
        Session session = this.session;
        if (session != null) {
            session.close();
            this.session = null;
        }
    }

    public void sendPing() {
        sendMessage("{}");
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        this.connected = true;
        listener.websocketConnected(true);
        logger.debug("LightwaveRF - WebSocket Socket successfully connected to {}",
                session.getRemoteAddress().getAddress());
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        listener.onMessage(message);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        logger.warn("LightwaveRF - Closing a WebSocket due to {}", closing ? "binding shutting down" : reason);
        session.close();
        this.session = null;
        this.connected = false;
        listener.websocketConnected(false);
    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        String reason = cause.getMessage();
        logger.error("Websocket Error, Please Enable Trace Logging to see the full error");
        StackTraceElement[] array = cause.getStackTrace();
        for (StackTraceElement traceElement : array) {
            logger.trace("{}", traceElement);
        }
        if (reason != null) {
            onClose(0, reason);
        } else {
            onClose(0, "");
        }
    }

    public void sendMessage(String message) {
        if (connected) {
            logger.debug("Sending message: {}", message);
            Session session = this.session;
            if (session != null) {
                session.getRemote().sendString(message, new WriteCallback() {
                    @Override
                    public void writeSuccess() {
                        logger.debug("LightwaveRF - Websocket message sending completed: {}", message);
                    }

                    @Override
                    public void writeFailed(@Nullable Throwable e) {
                        logger.warn("LightwaveRF - Websocket message sending failed: {} with reason: {}", message,
                                e.getMessage());
                    }
                });
            }
        } else {
            logger.debug("Websocket is unable to send the message as its disconnected {}", message);
        }
    }
}
