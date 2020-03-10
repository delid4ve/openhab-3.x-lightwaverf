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
package org.openhab.binding.lightwaverf.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lightwaverf.internal.api.AccessToken;
import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.openhab.binding.lightwaverf.internal.api.discovery.Devices;
import org.openhab.binding.lightwaverf.internal.api.discovery.Root;
import org.openhab.binding.lightwaverf.internal.api.discovery.StructureList;
import org.openhab.binding.lightwaverf.internal.api.login.Login;
import org.openhab.binding.lightwaverf.internal.Http;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which
 * are used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */
@NonNullByDefault
public class Http {
    private final Logger logger = LoggerFactory.getLogger(Http.class);
    private Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

    private Properties getHeader(@Nullable String type) {
        Properties headers = new Properties();
        switch (type) {
        case "login":
            headers.put("x-lwrf-appid", "ios-01");
            break;
        case "structures":
        case "structure":
        case "feature":
        case "features":
            headers.put("Authorization", "Bearer " + AccessToken.getToken());
            break;
        }
        return headers;
    }

    private String url(@Nullable String type, @Nullable String groupId) {
        String url;
        switch (type) {
        case "login":
            url = "https://auth.lightwaverf.com/v2/lightwaverf/autouserlogin/lwapps";
            break;
        case "structures":
            url = "https://publicapi.lightwaverf.com/v1/structures/";
            break;
        case "structure":
            url = "https://publicapi.lightwaverf.com/v1/structure/" + groupId;
            break;
        case "feature":
            url = "https://publicapi.lightwaverf.com/v1/feature/" + groupId;
            break;
        case "features":
            url = "https://publicapi.lightwaverf.com/v1/features/read";
            break;
        default:
            url = "";
        }
        return url;
    }

    public String method(@Nullable String type) {
        String method;
        switch (type) {
        case "login":
        case "feature":
        case "features":
            method = "POST";
            break;
        case "structures":
        case "structure":
            method = "GET";
            break;
        default:
            method = "GET";
        }
        return method;
    }

    public String httpClient(@Nullable String type, @Nullable InputStream data, @Nullable String other,
            @Nullable String groupId) {
        String response = "";
        try {
            response = HttpUtil.executeUrl(method(type), url(type, groupId), getHeader(type), data, other, 100000);
        } catch (IOException e) {
            if (e.getMessage().toString().contains("java.lang.InterruptedException")) {
                logger.debug("Http request was interrupted: {}",e.getMessage());
            } else {
                logger.debug("Http Util threw an error: {}",e.getMessage());
            }
        }
        return response;
    }
    
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

    private StructureList getStructureList() {
        String response = httpClient("structures", null, null, null);
        StructureList structureList = gson.fromJson(response, StructureList.class);
        return structureList;
    }

    private Root getStructure(String structureId) {
        String response = httpClient("structure", null, null, structureId);
        Root structure = gson.fromJson(response, Root.class);
        return structure;
    }

    public void getToken(String username, String password)  {
        logger.warn("Get new token");
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("email", username);
        jsonReq.addProperty("password", password);
        InputStream body = new ByteArrayInputStream(jsonReq.toString().getBytes(StandardCharsets.UTF_8));
        String response = httpClient("login", body, "application/json", null);
        if (response.contains("Not found")) {
            logger.warn("Incorrect user credentials");
        }
        else{
        Login login = gson.fromJson(response, Login.class);
        String sessionKey = login.getTokens().getAccessToken().toString();
        AccessToken.setToken(sessionKey);
        }
    }
}
