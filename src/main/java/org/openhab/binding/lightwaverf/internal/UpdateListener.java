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
import org.eclipse.smarthome.core.thing.Thing;
import org.openhab.binding.lightwaverf.internal.api.FeatureStatus;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class UpdateListener {
    private final static Logger logger = LoggerFactory.getLogger(Http.class);             
    public static List<Thing> thingList = new ArrayList<Thing>();
    public static List<String> channelList = new ArrayList<String>();
    private static List<FeatureStatus> featureStatus = LWBindingConstants.featureStatus;
    private static String jsonBody = "";
    private static String jsonEnd = "";
    private static String jsonMain = "";
    private static int partitionSize = 30;
    public static List<List<String>> partitions = new ArrayList<>();
    private final static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

    public static void updateListener() throws Exception {
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

    
    
}