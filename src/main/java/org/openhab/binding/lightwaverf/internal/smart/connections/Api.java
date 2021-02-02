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
package org.openhab.binding.lightwaverf.internal.smart.connections;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.smarthome.io.net.http.HttpClientFactory;
import org.openhab.binding.lightwaverf.internal.smart.dto.api.Devices;
import org.openhab.binding.lightwaverf.internal.smart.dto.api.Login;
import org.openhab.binding.lightwaverf.internal.smart.dto.api.Root;
import org.openhab.binding.lightwaverf.internal.smart.dto.api.StructureList;
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
public class Api {
    // private String token = "";
    private final Logger logger = LoggerFactory.getLogger(Api.class);

    private String token = "";
    private final Gson gson;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String loginUrl;

    public Api(HttpClientFactory httpClientFactory, Gson gson) {
        this.gson = gson;
        this.httpClient = httpClientFactory.createHttpClient("sonoffApi");
        this.baseUrl = "https://publicapi.lightwaverf.com/v1/";
        this.loginUrl = "https://auth.lightwaverf.com/v2/lightwaverf/autouserlogin/lwapps";
    }

    public void start() {
        try {
            httpClient.start();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            httpClient.stop();
            httpClient.destroy();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*
     * private Properties getHeader(@Nullable String type) {
     * Properties headers = new Properties();
     * switch (type) {
     * case "login":
     * headers.put("x-lwrf-appid", "ios-01");
     * break;
     * case "feature":
     * case "features":
     * headers.put("Authorization", "Bearer " + token);
     * break;
     * }
     * return headers;
     * }
     * 
     * private String url(@Nullable String type, @Nullable String groupId) {
     * String url;
     * switch (type) {
     * 
     * case "feature":
     * url = "https://publicapi.lightwaverf.com/v1/feature/" + groupId;
     * break;
     * case "features":
     * url = "https://publicapi.lightwaverf.com/v1/features/read";
     * break;
     * default:
     * url = "";
     * }
     * return url;
     * }
     * 
     * public String method(@Nullable String type) {
     * String method;
     * case "feature":
     * case "features":
     * method = "POST";
     * break;
     * }
     * return method;
     * }
     */

    public List<Devices> getDevices() {
        List<Devices> devices = new ArrayList<Devices>();
        StructureList structureList = new StructureList();
        structureList = getStructureList();
        for (int a = 0; a < structureList.getStructures().size(); a++) {
            String structureId = structureList.getStructures().get(a).toString();
            Root structure = getStructure(structureId);
            devices.addAll(structure.getDevices());
        }
        return devices;
    }

    public Devices getDevice(String sdId) {
        List<Devices> devices = new ArrayList<Devices>();
        Devices device = new Devices();
        StructureList structureList = new StructureList();
        structureList = getStructureList();
        for (int a = 0; a < structureList.getStructures().size(); a++) {
            String structureId = structureList.getStructures().get(a).toString();
            Root structure = getStructure(structureId);
            devices.addAll(structure.getDevices());
        }
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getDeviceId().contains("-" + sdId + "-")) {
                device = devices.get(i);
            }
        }
        return device;
    }

    public @Nullable StructureList getStructureList() {
        String url = baseUrl + "structures/";
        StructureList structureList = new StructureList();
        ContentResponse response;
        try {
            response = httpClient.newRequest(url).header("Authorization", "Bearer " + token).method("GET").send();
            logger.debug("LightwaveRF - StructureList Response:{}", response.getContentAsString());
            error(response.getContentAsString());
            structureList = gson.fromJson(response.getContentAsString(), StructureList.class);
            return structureList;
        } catch (Exception e) {
            logger.warn("Api Couldnt discover structures:{}", e);
            return null;
        }
    }

    public @Nullable Root getStructure(String structureId) {
        String url = baseUrl + "structure/" + structureId;
        Root root = new Root();
        ContentResponse response;
        try {
            response = httpClient.newRequest(url).header("Authorization", "Bearer " + token).method("GET").send();
            logger.debug("LightwaveRF - StructureList Response:{}", response.getContentAsString());
            error(response.getContentAsString());
            root = gson.fromJson(response.getContentAsString(), Root.class);
            return root;
        } catch (Exception e) {
            logger.warn("Api Couldnt discover structure:{}", e);
            return null;
        }
    }

    public @Nullable String login(String username, String password) {
        Login login = new Login();
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("email", username);
        jsonReq.addProperty("password", password);
        ContentResponse response;
        try {
            response = httpClient.newRequest(loginUrl).header("x-lwrf-appid", "ios-01")
                    // .header("accept", "application/json").header("Content-Type", "application/json")
                    .method("POST").content(new StringContentProvider(gson.toJson(jsonReq)), "application/json").send();
            error(response.getContentAsString());
            login = gson.fromJson(response.getContentAsString(), Login.class);
            token = login.getTokens().getAccessToken();
            logger.debug("LightwaveRF - New Refresh Token Aquired: {}", token);
            return response.getContentAsString();
        } catch (Exception e) {
            logger.debug("LightwaveRF - Error Logging into API:{}", e);
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
