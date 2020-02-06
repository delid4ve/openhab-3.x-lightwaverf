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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.lightwaverf.internal.Http;
import org.openhab.binding.lightwaverf.internal.UpdateListener;
import org.openhab.binding.lightwaverf.internal.Utils;
import org.openhab.binding.lightwaverf.internal.api.login.*;
import org.openhab.binding.lightwaverf.internal.api.*;
import org.openhab.binding.lightwaverf.internal.config.AccountConfig;
import org.openhab.binding.lightwaverf.internal.LWDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.FieldNamingPolicy;
import java.io.ByteArrayInputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class LWAccountHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(LWAccountHandler.class);
    private final static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
    private ScheduledFuture<?> connectionCheckTask;
    private ScheduledFuture<?> refreshTask;
    private AccountConfig config;
    private static boolean isConnected = false;
    public static boolean listsCreated = false;
    private ScheduledFuture<?> listTask;
    private String sessionKey;
    public LWAccountHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Lightwave account handler.");
        config = getConfigAs(AccountConfig.class);
        startConnectionCheck();
        if (listTask == null || listTask.isCancelled()) {
            listTask = scheduler.schedule(this::startRefresh, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void dispose() {
        // logger.debug("Running dispose()");
        stopConnectionCheck();
        config = null;
        if (refreshTask != null) {
            refreshTask.cancel(true);
            listTask.cancel(true);
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(LWDiscoveryService.class);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

    }

    public ThingUID getID() {
        return getThing().getUID();
    }

    public synchronized static boolean isListsCreated() {
        return listsCreated;
    }

    public synchronized static void setListsCreated(boolean state) {
        listsCreated = state;
    }
    public synchronized boolean isConnected() {
        return isConnected;
    }

    public synchronized static void setConnected(boolean state) {
        isConnected = state;
    }

    public void login(String username, String password) throws Exception {
        logger.warn("Start Login Process");
        setConnected(false);
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("email", username);
        jsonReq.addProperty("password", password);
        InputStream body = new ByteArrayInputStream(jsonReq.toString().getBytes(StandardCharsets.UTF_8));
        // InputStream body = Utils.createRequestBody(jsonReq.toString());
        String response = Http.httpClient("login", body, "application/json", null);
        logger.warn("Returned Login Http Response {}", response);
        if (response.contains("Not found")) {
            logger.warn("Lightwave Rf Servers Currently Down");
            updateStatus(ThingStatus.OFFLINE);
        }
        Login login = gson.fromJson(response, Login.class);
        logger.warn("Parsed Login response");
        sessionKey = login.getTokens().getAccessToken().toString();
        AccessToken.setToken(sessionKey);
        logger.warn("token: {}", sessionKey);
        Utils.createLists();
        Utils.createFeatureStatus();
        setListsCreated(true);
        setConnected(true);
        logger.warn("Connected");
    }

    private void startRefresh() {
        int refresh = Integer.valueOf(this.thing.getConfiguration().get("pollingInterval").toString());
        if (UpdateListener.channelList.size() == 0) {
            logger.warn("Channel List For Updating Is Empty");
        }
        if (refreshTask == null || refreshTask.isCancelled()) {
            refreshTask = scheduler.scheduleWithFixedDelay(this::updateStateAndChannels, 0, refresh, TimeUnit.SECONDS);
        }
    }

    private void updateStateAndChannels() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    UpdateListener.updateListener();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
    }
}).start();
    }

    private void connect() throws Exception {
        logger.debug("Initializing connection to Lightwave");
        updateStatus(ThingStatus.OFFLINE);
            login(config.username, config.password);
            updateStatus(ThingStatus.ONLINE);
    }

    private void startConnectionCheck() {
        if (connectionCheckTask == null || connectionCheckTask.isCancelled()) {
            logger.debug("Start periodic connection check");
            Runnable runnable = () -> {
                logger.debug("Checking Lightwave connection");
                if (isConnected()) {
                    logger.debug("Connection to Lightwave established");
                } else {
                        try {
                        connect();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                }
            };
            connectionCheckTask = scheduler.scheduleWithFixedDelay(runnable, 0, 60, TimeUnit.SECONDS);
        } else {
             logger.debug("Connection check task already running");
        }
    }

    private void stopConnectionCheck() {
        if (connectionCheckTask != null) {
            logger.debug("Stop periodic connection check");
            connectionCheckTask.cancel(true);
            connectionCheckTask = null;
        }
    }

    

}
