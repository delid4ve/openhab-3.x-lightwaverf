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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.openhab.binding.lightwaverf.internal.dto.api.LightwaverfSmartDevices;
import org.openhab.binding.lightwaverf.internal.dto.api.LightwaverfSmartLogin;
import org.openhab.binding.lightwaverf.internal.dto.api.LightwaverfSmartRoot;
import org.openhab.binding.lightwaverf.internal.dto.api.LightwaverfSmartStructureList;
import org.openhab.binding.lightwaverf.internal.listeners.LightwaverfSmartListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which
 * are used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */
@NonNullByDefault
public class LightwaverfSmartApi {
    private final Logger logger = LoggerFactory.getLogger(LightwaverfSmartApi.class);

    private final Gson gson;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String loginUrl;
    private final LightwaverfSmartListener listener;

    private String token = "";
    private String username = "";
    private String password = "";

    public LightwaverfSmartApi(HttpClient httpClient, Gson gson, LightwaverfSmartListener listener) {
        this.gson = gson;
        this.httpClient = httpClient;
        this.listener = listener;
        this.baseUrl = "https://publicapi.lightwaverf.com/v1/";
        this.loginUrl = "https://auth.lightwaverf.com/v2/lightwaverf/autouserlogin/lwapps";
    }

    public void start(String username, String password) {
        this.username = username;
        this.password = password;
        login();
    }

    public List<LightwaverfSmartDevices> getDevices() {
        List<LightwaverfSmartDevices> devices = new ArrayList<LightwaverfSmartDevices>();
        LightwaverfSmartStructureList structureList = new LightwaverfSmartStructureList();
        structureList = getStructureList();
        for (int a = 0; a < structureList.getStructures().size(); a++) {
            String structureId = structureList.getStructures().get(a).toString();
            LightwaverfSmartRoot structure = getStructure(structureId);
            devices.addAll(structure.getDevices());
        }
        return devices;
    }

    public @Nullable LightwaverfSmartStructureList getStructureList() {
        String url = baseUrl + "structures/";
        LightwaverfSmartStructureList structureList = new LightwaverfSmartStructureList();
        ContentResponse response;
        try {
            response = httpClient.newRequest(url).header("Authorization", "Bearer " + token).method("GET").send();
            logger.debug("StructureList Response:{}", response.getContentAsString());
            error(response.getContentAsString());
            structureList = gson.fromJson(response.getContentAsString(), LightwaverfSmartStructureList.class);
            return structureList;
        } catch (Exception e) {
            logger.warn("Api Couldnt discover structures:{}", e.getMessage());
            return null;
        }
    }

    public @Nullable LightwaverfSmartRoot getStructure(String structureId) {
        String url = baseUrl + "structure/" + structureId;
        ContentResponse response;
        try {
            response = httpClient.newRequest(url).header("Authorization", "Bearer " + token).method("GET").send();
            logger.debug("Structure Response:{}", response.getContentAsString());
            error(response.getContentAsString());
            LightwaverfSmartRoot root = gson.fromJson(response.getContentAsString(), LightwaverfSmartRoot.class);
            if (root != null) {
                return root;
            }
            return null;
        } catch (Exception e) {
            logger.warn("Api Couldnt discover structure:{}", e.getMessage());
            return null;
        }
    }

    public @Nullable String login() {
        LightwaverfSmartLogin login = new LightwaverfSmartLogin();
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("email", this.username);
        jsonReq.addProperty("password", this.password);
        ContentResponse response;
        try {
            response = httpClient.newRequest(loginUrl).header("x-lwrf-appid", "ios-01")
                    // .header("accept", "application/json").header("Content-Type", "application/json")
                    .method("POST").content(new StringContentProvider(gson.toJson(jsonReq)), "application/json").send();
            error(response.getContentAsString());
            login = gson.fromJson(response.getContentAsString(), LightwaverfSmartLogin.class);
            token = login.getTokens().getAccessToken();
            listener.tokenUpdated(token);
            logger.debug("LightwaveRF - New Refresh Token Aquired: {}", token);
            return response.getContentAsString();
        } catch (Exception e) {
            logger.debug("LightwaveRF - Error Logging into API:{}", e.getMessage());
            return null;
        }
    }

    public String getToken() {
        return token;
    }

    public void error(String response) {
        if (response.contains("Not found")) {
            logger.warn("LightwaveRF - API returned 'Not Found' - User Credentials maybe incorrect");
        } else if (response.contains("Discovery Failed")) {
            logger.warn("LightwaveRF - Discovery failed, API maybe offline");
        }
    }
}
