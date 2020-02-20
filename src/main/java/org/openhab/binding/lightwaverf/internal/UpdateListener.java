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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.lightwaverf.internal.api.AccessToken;
import org.openhab.binding.lightwaverf.internal.api.FeatureStatus;
import org.openhab.binding.lightwaverf.internal.api.discovery.Devices;
import org.openhab.binding.lightwaverf.internal.api.discovery.Features;
import org.openhab.binding.lightwaverf.internal.api.login.Login;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which
 * are used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */
@NonNullByDefault
public class UpdateListener {
    private final Logger logger = LoggerFactory.getLogger(UpdateListener.class);
    private List<FeatureStatus> featureStatus = new ArrayList<FeatureStatus>();
    private boolean isConnected = false;
    private String sessionKey = "";
    private Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

    public void updateListener(List<List<String>> partitions, Map<String, Long> locks) throws IOException {
        String jsonBody = "";
        String jsonEnd = "";
        String jsonMain = "";
        logger.debug("Start Partitioning Into Groups");
        for (int l = 0; l < partitions.size(); l++) {
            logger.debug("Start Partition {} of {}", (l+1),partitions.size());
            jsonBody = "{\"features\": [";
            jsonEnd = "";
            for (int m = 0; m < partitions.get(l).size(); m++) {
                if (m < (partitions.get(l).size() - 1)) {
                    jsonEnd = ",";
                } else {
                    jsonEnd = "]}";
                }
                jsonMain = "{\"featureId\": \"" + partitions.get(l).get(m).toString() + "\"}";
                jsonBody = jsonBody + jsonMain + jsonEnd;
            }
            InputStream data = new ByteArrayInputStream(jsonBody.getBytes(StandardCharsets.UTF_8));
            String response = Http.httpClient("features", data, "application/json", "");
            logger.debug("JSON Response: {}", response);
            if(response.contains("{\"message\":\"Structure not found\"}")) {
                logger.warn("Api Timed Out, decrease your group size.");
            }
            else if(response.contains("{\"message\":\"FeatureRead Failed\"}")) {
                logger.warn("Lightwaves Servers currently in error state, try and reduce your polling to see if helps");
            }
            else {
                HashMap<String, Long> featureStatuses = gson.fromJson(response,
                    new TypeToken<HashMap<String, Long>>() {}.getType());
                for (Map.Entry<String, Long> myMap : featureStatuses.entrySet()) {
                    String key = myMap.getKey().toString();
                    Long value = myMap.getValue();
                    featureStatus.stream().filter(i -> key.equals(i.getFeatureId())).forEach(u -> {
                        if(!locks.containsKey(key)) {
                            u.setValue(value);
                        } else {
                            logger.debug("feature {} not updated as lock is present", key);
                        } 
                    });
                }
            }
            logger.debug("Finish Partition {} of {}", (l+1),partitions.size());
        }
        logger.debug("Finished current poll");
    }

    public synchronized boolean isConnected() {
        return isConnected;
    }

    private synchronized void setConnected(boolean state) {
        isConnected = state;
    }

    public List<FeatureStatus> featureStatus() {
        return featureStatus;
    }
    
    public void addFeatureStatus(Devices device) {
        Integer added = 0;
        List<Features> features = new ArrayList<Features>();
            for (int c = 0; c < device.getFeatureSets().size(); c++) {
                features.addAll(device.getFeatureSets().get(c).getFeatures());
            }
            for (int d = 0; d < features.size(); d++) {
                String featureId = features.get(d).getFeatureId();
                Boolean containsFeature = featureStatus.stream().anyMatch(x -> x.getFeatureId().equals(featureId));
                if (!containsFeature) {
                    String json = "{\"featureId\": " + features.get(d).getFeatureId() + ",\"value\": -1}";
                    FeatureStatus featureStatusItem = gson.fromJson(json, FeatureStatus.class);
                    featureStatus.add(featureStatusItem);
                    added ++;
                }
                else {
                //logger.debug("Feature Status {} Already Present", features.get(d).getFeatureId());
            }
        }
        logger.debug("{} of {} features Added From Device: {} ", added,features.size(),device.getName());
        logger.debug("New featureStatus Size: {}", featureStatus.size());
    }

    public void getToken(String username, String password) throws IOException {
        logger.warn("Get new token");
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("email", username);
        jsonReq.addProperty("password", password);
        InputStream body = new ByteArrayInputStream(jsonReq.toString().getBytes(StandardCharsets.UTF_8));
        String response = Http.httpClient("login", body, "application/json", null);
        if (response.contains("Not found")) {
            logger.warn("Incorrect user credentials");
            setConnected(false);
        }
        else{
        Login login = gson.fromJson(response, Login.class);
        sessionKey = login.getTokens().getAccessToken().toString();
        AccessToken.setToken(sessionKey);
        }
    }

    public void login(String username, String password) throws IOException {
        logger.warn("Start Lightwave Login Process");
        setConnected(false);
        getToken(username,password);
        logger.debug("token: {}", sessionKey);
        setConnected(true);
        logger.warn("Connected to lightwave");
    }
}
