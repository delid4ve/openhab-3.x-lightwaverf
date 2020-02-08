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

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openhab.binding.lightwaverf.internal.api.AccessToken;
import org.openhab.binding.lightwaverf.internal.api.FeatureStatus;
import org.openhab.binding.lightwaverf.internal.api.discovery.Devices;
import org.openhab.binding.lightwaverf.internal.api.discovery.FeatureSets;
import org.openhab.binding.lightwaverf.internal.api.discovery.Features;
import org.openhab.binding.lightwaverf.internal.api.discovery.Root;
import org.openhab.binding.lightwaverf.internal.api.discovery.StructureList;
import org.openhab.binding.lightwaverf.internal.api.login.Login;
import org.openhab.binding.lightwaverf.internal.handler.DeviceHandler;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class UpdateListener {
    private final static Logger logger = LoggerFactory.getLogger(Http.class);             
    private static List<Root> structures = new ArrayList<Root>();
    private static List<Devices> devices = new ArrayList<Devices>();
    private static List<FeatureSets> featureSets = new ArrayList<FeatureSets>();
    private static List<Features> features = new ArrayList<Features>();
    private static List<FeatureStatus> featureStatus = new ArrayList<FeatureStatus>();
    private static List<String> channelList = new ArrayList<String>();
    private static List<String> cLinked = new ArrayList<String>();
    private static List<List<String>> partitions = new ArrayList<>();
    private static String jsonBody = "";
    private static String jsonEnd = "";
    private static String jsonMain = "";
    private static int partitionSize;
    private boolean isConnected = false;
    private String sessionKey;
    private final static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

    public void updateListener() throws Exception {
        partitionSize = DeviceHandler.partitionSize;
        jsonBody = "";
        jsonEnd = "";
        jsonMain = "";
        partitions.clear();
        for (int i = 0; i < channelList.size(); i += partitionSize) {
            partitions.add(channelList.subList(i, Math.min(i + partitionSize, channelList.size())));
        }
        for (int l = 0; l < partitions.size(); l++) {
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
            logger.debug("response:{}", response);
            HashMap<String, Integer> featureStatuses = gson.fromJson(response,
                    new TypeToken<HashMap<String, Integer>>() {
                    }.getType());
            for (Map.Entry<String, Integer> myMap : featureStatuses.entrySet()) {
                String key = myMap.getKey().toString();
                int value = myMap.getValue();
                featureStatus.stream().filter(i -> key.equals(i.getFeatureId())).forEach(u -> u.setValue(value));
            }
        }
    }

    public synchronized boolean isConnected() {
        return isConnected;
    }

    private synchronized void setConnected(boolean state) {
        isConnected = state;
    }

    public synchronized List<Devices> devices() {
        return devices;
    }

    public synchronized List<FeatureSets> featureSets() {
        return featureSets;
    }

    public synchronized List<String> channelList() {
        return channelList;
    }

    public synchronized List<FeatureStatus> featureStatus() {
        return featureStatus;
    }

    public synchronized List<String> cLinked() {
        return cLinked;
    }

    public synchronized void addcLinked(String link) {
        cLinked.add(link);
    }

    public void login(String username, String password) throws Exception {
        logger.warn("Start Login Process");
        setConnected(false);
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("email", username);
        jsonReq.addProperty("password", password);
        InputStream body = new ByteArrayInputStream(jsonReq.toString().getBytes(StandardCharsets.UTF_8));
        String response = Http.httpClient("login", body, "application/json", null);
        logger.warn("Returned Login Http Response {}", response);
        if (response.contains("Not found")) {
            logger.warn("Lightwave Rf Servers Currently Down");
            // updateStatus(ThingStatus.OFFLINE);
        }
        Login login = gson.fromJson(response, Login.class);
        logger.warn("Parsed Login response");
        sessionKey = login.getTokens().getAccessToken().toString();
        AccessToken.setToken(sessionKey);
        logger.warn("token: {}", sessionKey);
        createLists();
        createFeatureStatus();
        setConnected(true);
        logger.warn("Connected");
    }

    private void createLists() throws Exception {
        structures.clear();
        devices.clear();
        featureSets.clear();
        features.clear();
        logger.debug("Started List Generation");
        String response = Http.httpClient("structures", null, null, null);
        StructureList structureList = gson.fromJson(response, StructureList.class);
        for (int a = 0; a < structureList.getStructures().size(); a++) {
            String groupId = structureList.getStructures().get(a).toString();
            String response2 = Http.httpClient("structure", null, null, groupId);
            Root structure = gson.fromJson(response2, Root.class);
            structures.add(structure);
            devices.addAll(structure.getDevices());
            for (int b = 0; b < devices.size(); b++) {
                featureSets.addAll(devices.get(b).getFeatureSets());
            }
            for (int c = 0; c < featureSets.size(); c++) {
                features.addAll(featureSets.get(c).getFeatures());
                }
                logger.warn("createLists Features size {}", features.size());
            }
        }

        private void createFeatureStatus() {
            String a;
            featureStatus.clear();
            
                logger.warn("Started Status Update");
                for (int j = 0; j < features.size(); j++) {
                    a = "{\"featureId\": " + features.get(j).getFeatureId() + ",\"value\": 0}";
                    // logger.warn("String {}", a);
                    FeatureStatus b = gson.fromJson(a, FeatureStatus.class);
                    featureStatus.add(j, b);
                }
                logger.warn("createLists Feature Status size {}", featureStatus.size());
        }
        public static InputStream createRequestBody(String featureSetId) {
            for (int i = 0; i < featureSets.size(); i++) {
                if (featureSets.get(i).getFeatureSetId().contains(featureSetId)) {
                    jsonBody = "{\"features\": [";
                    jsonEnd = "";
                    for (int l = 0; l < featureSets.get(i).getFeatures().size(); l++) {
                        if (l < (featureSets.get(i).getFeatures().size() - 1)) {
                            jsonEnd = ",";
                        } else {
                            jsonEnd = "]}";
                        }
                        jsonMain = "{\"featureId\": \"" + featureSets.get(i).getFeatures().get(l).getFeatureId() + "\"}";
                        jsonBody = jsonBody + jsonMain + jsonEnd;
                    }
                }
            }
            InputStream data = new ByteArrayInputStream(jsonBody.getBytes(StandardCharsets.UTF_8));
            return data;
        }
}