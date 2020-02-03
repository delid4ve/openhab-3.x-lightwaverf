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

import java.io.IOException;
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
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.lightwaverf.internal.Http;
import org.openhab.binding.lightwaverf.internal.Utils;
import org.openhab.binding.lightwaverf.internal.api.login.*;
import org.openhab.binding.lightwaverf.internal.api.*;
import org.openhab.binding.lightwaverf.internal.config.AccountConfig;
import org.openhab.binding.lightwaverf.internal.discovery.LWDiscoveryService;
import org.openhab.binding.lightwaverf.internal.exceptions.*;
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
    private AccountConfig config;
    private boolean loginCredentialError;
    private static boolean isConnected = false;

    public LWAccountHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Lightwave account handler.");
        config = getConfigAs(AccountConfig.class);
        loginCredentialError = false;
        startConnectionCheck();
        
    }

    @Override
    public void dispose() {
        // logger.debug("Running dispose()");
        stopConnectionCheck();
        config = null;
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

    public synchronized boolean isConnected() {
        return isConnected;
    }

    public synchronized static void setConnected(boolean state) {
        isConnected = state;
    }

    public void login(String username, String password) throws LightwaveCommException, LightwaveLoginException, IOException {
        setConnected(false);
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("email", username);
        jsonReq.addProperty("password", password);
        InputStream body = new ByteArrayInputStream(jsonReq.toString().getBytes(StandardCharsets.UTF_8));
        //InputStream body = Utils.createRequestBody(jsonReq.toString());
        String response = Http.httpClient("login", body, "application/json", null);
        String sessionKey = null;
        Login login = gson.fromJson(response, Login.class);
            sessionKey = login.getTokens().getAccessToken().toString();
            AccessToken.setToken(sessionKey);
            //logger.debug("token: {}", sessionKey);
            Utils.createLists();
            Utils.createFeatureStatus();
            LWAccountHandler.setConnected(true);
    }

    private void connect() throws LightwaveCommException, LightwaveLoginException, IOException {
        if (loginCredentialError) {
            throw new LightwaveLoginException("Connection to Lightwave can't be opened because of wrong credentials");
        }
        logger.debug("Initializing connection to Lightwave");
        updateStatus(ThingStatus.OFFLINE);
        try {
            login(config.username, config.password);
            //connection.login(config.username, config.password);
            updateStatus(ThingStatus.ONLINE);

        } catch (LightwaveLoginException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            loginCredentialError = true;
            throw e;
        } catch (LightwaveCommException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            throw e;
        }
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
                    } catch (LightwaveCommException | LightwaveLoginException | IOException e) {
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
