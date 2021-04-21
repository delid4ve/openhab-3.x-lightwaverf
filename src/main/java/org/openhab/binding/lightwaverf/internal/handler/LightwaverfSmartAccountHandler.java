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
package org.openhab.binding.lightwaverf.internal.handler;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.openhab.binding.lightwaverf.internal.LightwaverfSmartCommandManager;
import org.openhab.binding.lightwaverf.internal.config.LightwaverfSmartAccountConfig;
import org.openhab.binding.lightwaverf.internal.connections.LightwaverfSmartApi;
import org.openhab.binding.lightwaverf.internal.connections.LightwaverfSmartWebsocket;
import org.openhab.binding.lightwaverf.internal.discovery.LWDiscoveryService;
import org.openhab.binding.lightwaverf.internal.dto.LightwaverfSmartRequest;
import org.openhab.binding.lightwaverf.internal.dto.api.LightwaverfSmartDevices;
import org.openhab.binding.lightwaverf.internal.listeners.LightwaverfSmartDeviceListener;
import org.openhab.binding.lightwaverf.internal.listeners.LightwaverfSmartListener;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which
 * are used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */
@NonNullByDefault
public class LightwaverfSmartAccountHandler extends BaseBridgeHandler implements LightwaverfSmartListener {
    private final Logger logger = LoggerFactory.getLogger(LightwaverfSmartAccountHandler.class);
    private final LightwaverfSmartApi api;
    private final LightwaverfSmartWebsocket webSocket;
    private final LightwaverfSmartCommandManager commandManager;

    private @Nullable ScheduledFuture<?> connectionTask;
    private @Nullable ScheduledFuture<?> tokenTask;
    private @Nullable ScheduledFuture<?> queueTask;

    private Map<String, LightwaverfSmartDevices> devices = new HashMap<String, LightwaverfSmartDevices>();

    private Boolean wsOnline = false;
    private Double electricityCost = 0.0;

    public LightwaverfSmartAccountHandler(Bridge thing, WebSocketClient webSocketClient, HttpClient httpClient,
            Gson gson) {
        super(thing);
        this.commandManager = new LightwaverfSmartCommandManager(this, gson);
        this.webSocket = new LightwaverfSmartWebsocket(webSocketClient, this);
        this.api = new LightwaverfSmartApi(httpClient, gson, this);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(LWDiscoveryService.class);
    }

    @Override
    public void initialize() {
        LightwaverfSmartAccountConfig config = this.getConfigAs(LightwaverfSmartAccountConfig.class);
        // String username = (String) getConfig().get("username");
        // String password = (String) getConfig().get("password");
        // Integer retries = (Integer) getConfig().get("retries");
        // Integer electricityCost = (Integer) getConfig().get("electricityCost");
        // Integer delay = (Integer) getConfig().get("delay");

        String username = config.username;
        String password = config.password;
        Integer retries = config.retries;
        Integer electricityCost = config.electricityCost;
        Integer delay = config.delay;
        Integer timeout = config.timeout;
        this.electricityCost = ((double) electricityCost) / 100;
        api.start(username, password);
        commandManager.start(retries, timeout);
        List<LightwaverfSmartDevices> deviceList = api.getDevices();
        for (int i = 0; i < deviceList.size(); i++) {
            LightwaverfSmartDevices device = deviceList.get(i);
            String deviceid = device.getDeviceId();
            // Add to list for device handlers to initialise
            devices.put(deviceid, deviceList.get(i));
        }
        queueTask = scheduler.scheduleWithFixedDelay(commandManager, 0, delay, TimeUnit.MILLISECONDS);
        webSocket.start();
        // Create other tasks
        logger.debug("Creating scheduled tasks");
        Runnable connectionCheck = () -> {
            if (!wsOnline) {
                reConnect();
            } else {
                // commandManager.sendLoginCommand();
                // webSocket.sendPing();
            }
        };
        connectionTask = scheduler.scheduleWithFixedDelay(connectionCheck, 60, 60, TimeUnit.SECONDS);
        Runnable refreshTokens = () -> {
            if (wsOnline) {
                refreshTokens();
            }
        };
        tokenTask = scheduler.scheduleWithFixedDelay(refreshTokens, 24, 24, TimeUnit.HOURS);
        updateProperties();
    }

    @Override
    public void dispose() {
        logger.debug("LightwaveRF - Running dispose()");
        ScheduledFuture<?> connectionTask = this.connectionTask;
        if (connectionTask != null) {
            connectionTask.cancel(true);
            this.connectionTask = null;
        }
        ScheduledFuture<?> tokenTask = this.tokenTask;
        if (tokenTask != null) {
            tokenTask.cancel(true);
            this.tokenTask = null;
        }
        ScheduledFuture<?> queueTask = this.queueTask;
        if (queueTask != null) {
            Boolean complete = queueTask.cancel(false);
            if (complete.equals(true)) {
                this.queueTask = null;
            } else {
                logger.debug("Unable to nullify queuetask");
            }
        }
        commandManager.setRunning(false);
        webSocket.stop();
        commandManager.stop();
        wsOnline = false;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    private void updateProperties() {
        Map<String, String> properties = editProperties();
        properties.clear();
        devices.forEach((k, v) -> {
            properties.put("Connected Device: " + v.getName() + "", "Deviceid: " + k + ", Product: " + v.getDesc()
                    + ", Gen: " + v.getGen() + ", Channels: " + v.getFeatureSets().size() + "\r\n");
        });
        updateProperties(properties);
    }

    public void addFeature(String featureid, String deviceid) {
        commandManager.addFeature(featureid, deviceid);
    }

    public void addDeviceListener(String deviceId, LightwaverfSmartDeviceListener listener) {
        commandManager.addDeviceListener(deviceId, listener);
    }

    public void removeDeviceListener(String deviceId) {
        commandManager.removeDeviceListener(deviceId);
    }

    public void sendDeviceCommand(LightwaverfSmartRequest command) {
        commandManager.queueCommand(command);
    }

    public Boolean isConnected() {
        return wsOnline;
    }

    // Required for device initialisation
    public @Nullable LightwaverfSmartDevices getDevice(String deviceid) {
        return devices.get(deviceid);
    }

    // Required for discovery
    public List<LightwaverfSmartDevices> getDevices() {
        return api.getDevices();
    }

    private void reConnect() {
        api.login();
        webSocket.start();
    }

    private void refreshTokens() {
        api.login();
        commandManager.sendLoginCommand();
    }

    public Double getElectricityCost() {
        return this.electricityCost;
    }

    @Override
    public void onMessage(String message) {
        commandManager.onMessage(message);
    }

    @Override
    public void websocketConnected(Boolean connected) {
        commandManager.websocketConnected(connected);
        if (!connected) {
            updateStatus(ThingStatus.OFFLINE);
            this.wsOnline = false;
        }
    }

    @Override
    public void tokenUpdated(String token) {
        commandManager.tokenUpdated(token);
    }

    @Override
    public void sendMessage(String message) {
        webSocket.sendMessage(message);
    }

    @Override
    public void websocketLoggedIn() {
        this.wsOnline = true;
        webSocket.setConnected(true);
        updateStatus(ThingStatus.ONLINE);
    }
}
