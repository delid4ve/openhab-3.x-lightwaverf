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
package org.openhab.binding.lightwaverf.internal.smart.handler;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.net.http.HttpClientFactory;
import org.eclipse.smarthome.io.net.http.WebSocketFactory;
import org.openhab.binding.lightwaverf.internal.LWDiscoveryService;
import org.openhab.binding.lightwaverf.internal.smart.config.AccountConfig;
import org.openhab.binding.lightwaverf.internal.smart.connections.Api;
import org.openhab.binding.lightwaverf.internal.smart.connections.Ws;
import org.openhab.binding.lightwaverf.internal.smart.dto.api.Devices;
import org.openhab.binding.lightwaverf.internal.smart.dto.websocket.InitialStatus;
import org.openhab.binding.lightwaverf.internal.smart.listeners.DeviceStateListener;
import org.openhab.binding.lightwaverf.internal.smart.listeners.WsEventListener;
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
public class LWAccountHandler extends BaseBridgeHandler implements WsEventListener {
    private final Logger logger = LoggerFactory.getLogger(LWAccountHandler.class);
    private final WebSocketFactory webSocketFactory;
    private final HttpClientFactory httpClientFactory;
    private @Nullable Api api;
    private final Gson gson;
    protected @Nullable Ws ws;
    private @Nullable ScheduledFuture<?> connectionCheckerFuture;
    private @Nullable ScheduledFuture<?> tokenTask;
    private @Nullable ScheduledFuture<?> connectionTask;
    private Map<String, Devices> devices = new HashMap<>();
    private Map<String, DeviceStateListener> stateListener = new HashMap<>();
    private Boolean wsOnline = false;
    private @Nullable AccountConfig config;

    public LWAccountHandler(Bridge thing, WebSocketFactory webSocketFactory, HttpClientFactory httpClientFactory,
            Gson gson) {
        super(thing);
        this.webSocketFactory = webSocketFactory;
        this.httpClientFactory = httpClientFactory;
        this.gson = gson;
    }

    @Override
    public void initialize() {
        config = this.getConfigAs(AccountConfig.class);
        tokenTask = scheduler.scheduleWithFixedDelay(this::refreshLogin, 24, 24, TimeUnit.HOURS);
        try {
            api = new Api(httpClientFactory, gson);
            api.start();
            String response = api.login(config.username, config.password);
            logger.debug("LightwaveRF - Login: {}", response);
            List<Devices> deviceList = api.getDevices();
            for (int i = 0; i < deviceList.size(); i++) {
                devices.put((deviceList.get(i).getDeviceId().split("-"))[1].toString(), deviceList.get(i));
            }
            updateProperties();
            ws = new Ws(webSocketFactory, this, gson);
            ws.start();
            Runnable connectionChecker = () -> {
                if (!wsOnline) {
                    api.login(config.username, config.password);
                    ws.start();
                    ws.login(api.getToken());
                }
            };
            connectionCheckerFuture = scheduler.scheduleWithFixedDelay(connectionChecker, 1, 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
        }
    }

    public @Nullable Devices getDevice(String sdid) {
        return devices.get(sdid);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    public Integer getElecCost() {
        return config.electricityCost;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(LWDiscoveryService.class);
    }

    @Override
    public void thingUpdated(Thing thing) {
        this.thing = thing;
        dispose();
        initialize();
    }

    @Override
    public void dispose() {
        logger.debug("LightwaveRF - Running dispose()");
        if (connectionCheckerFuture != null) {
            connectionCheckerFuture.cancel(true);
            connectionCheckerFuture = null;
        }
        if (tokenTask != null) {
            tokenTask.cancel(true);
            tokenTask = null;
        }
        if (ws != null) {
            ws.stop();
            ws = null;
        }
        wsOnline = false;
        if (api != null) {
            api.stop();
            api = null;
        }
    }

    @Override
    public void updateConnectionState(boolean connected) {
        logger.debug("LightwaveRF - Connection Status:{}", connected);
        wsOnline = connected;
        if (connected) {
            ws.login(api.getToken());
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    public Boolean isConnected() {
        return wsOnline;
    }

    public @Nullable Ws getConnection() {
        return ws;
    }

    public @Nullable Api getApi() {
        return api;
    }

    private void refreshLogin() {
        api.login(config.username, config.password);
        ws.login(api.getToken());
    }

    private void updateProperties() {
        Map<String, String> properties = editProperties();
        properties.clear();
        devices.forEach((k, v) -> {
            properties.put("Connected Device: " + v.getName() + "", "Simple DeviceId (sdId): " + k + ", Product: "
                    + v.getDesc() + ", Gen: " + v.getGen() + ", Channels: " + v.getFeatureSets().size() + "\r\n");
        });
        updateProperties(properties);
    }

    public void registerStateListener(String deviceId, DeviceStateListener listener) {
        stateListener.put(deviceId, listener);
    }

    public void unregisterStateListener(String deviceId) {
        stateListener.remove(deviceId);
    }

    public void onMessage(String message) {
        InitialStatus rm = new InitialStatus();
        rm = gson.fromJson(message, InitialStatus.class);
        if (!rm.getOperation().equals("authenticate")) {
            DeviceStateListener listener = stateListener
                    .get(rm.getItems().get(0).getPayload().getDeviceId().toString());
            if (listener != null) {
                listener.stateUpdate(rm);
            } else {
                logger.debug("LightwaveRF - Listener for device was null: {}", message);
            }
        }
    }
}
