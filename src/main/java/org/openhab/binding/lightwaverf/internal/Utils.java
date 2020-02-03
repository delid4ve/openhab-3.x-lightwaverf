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

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */

import org.openhab.binding.lightwaverf.internal.LWBindingConstants;
import org.openhab.binding.lightwaverf.internal.api.*;
import org.openhab.binding.lightwaverf.internal.api.discovery.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Utils {
    private final static Logger logger = LoggerFactory.getLogger(Utils.class);
    private static String jsonBody = "";
    private static String jsonEnd = "";
    private static String jsonMain = "";
    private static List<FeatureSets> featureSets = LWBindingConstants.featureSets;
    private final static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

    public static int featureStatusIndex(String Id) {
        for (int i = 0; i < LWBindingConstants.featureStatus.size(); i++) {
            if (LWBindingConstants.featureStatus.get(i).getFeatureId().contains(Id)) {
                return i;
            }
        }
        ;
        return -1;
    }

    public int featureSetIndex(String Id) {
        for (int i = 0; i < featureSets.size(); i++) {
            if (featureSets.get(i).getFeatureSetId().contains(Id)) {
                return i;
            }
        }
        ;
        return -1;
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

 /*   public static void updates(String type, String response) {
        switch (type) {
        case "features":
            List<FeatureStatus> temp = new ArrayList<FeatureStatus>();
            String array1[] = response.split(",");
            for (int j = 0; j < array1.length; j++) {
                String b = "{\"featureId\":"
                        + array1[j].toString().replace(":", ",\"value\": ").replace("{", "").replace("}", "") + "}";
                logger.warn("String b: {}", b);
                FeatureStatus d = gson.fromJson(b, FeatureStatus.class);
                temp.add(d);
            }
            logger.warn("temp size: {}", temp.size());
            for (int m = 0; m < temp.size(); m++) {
                int index = featureStatusIndex(temp.get(m).getFeatureId().toString());
            if (index != -1) {LWBindingConstants.featureStatus.set(index, temp.get(m));}
            } 
        case "login":
        String sessionKey = null;
        Login login = gson.fromJson(response, Login.class);
            sessionKey = login.getTokens().getAccessToken().toString();
            AccessToken.setToken(sessionKey);
            //logger.debug("token: {}", sessionKey);
            createLists();
            createFeatureStatus();
            LWAccountHandler.setConnected(true);
        }
    }*/

    public static void createLists() {
        if (LWBindingConstants.structures == null || LWBindingConstants.structures.isEmpty() == true) {
            String response = Http.httpClient("structures", null, null, null);
            StructureList structureList = gson.fromJson(response, StructureList.class);
            for (int a = 0; a < structureList.getStructures().size(); a++) {
                String groupId = structureList.getStructures().get(a).toString();
                logger.warn("Group ID {}", groupId);
                String response2 = Http.httpClient("structure", null, null, groupId);
                Root structure = gson.fromJson(response2, Root.class);
                LWBindingConstants.structures.add(structure);
                logger.warn("createLists Structure size {}", LWBindingConstants.structures.size());
                logger.warn("Structure First ID {}", LWBindingConstants.structures.get(0).getGroupId().toString());
                LWBindingConstants.devices.addAll(structure.getDevices());
                logger.warn("createLists Devices size {}", LWBindingConstants.devices.size());
                logger.warn("Device First ID {}", LWBindingConstants.devices.get(0).getDeviceId().toString());
                for (int b = 0; b < LWBindingConstants.devices.size(); b++) {
                    LWBindingConstants.featureSets.addAll(LWBindingConstants.devices.get(b).getFeatureSets());
                }
                logger.warn("createLists FeatureSets size {}", LWBindingConstants.featureSets.size());
                logger.warn("featureSet First ID {}", LWBindingConstants.featureSets.get(0).getFeatureSetId().toString());
                for (int c = 0; c < LWBindingConstants.featureSets.size(); c++) {
                    LWBindingConstants.features.addAll(LWBindingConstants.featureSets.get(c).getFeatures());
                }
                logger.warn("createLists Features size {}", LWBindingConstants.features.size());
                logger.warn("feature First ID {}", LWBindingConstants.features.get(0).getFeatureId().toString());
            }
        }
    }

    public static void createFeatureStatus() {
        String a;
        if (LWBindingConstants.featureStatus == null || LWBindingConstants.featureStatus.isEmpty() == true) {
            logger.warn("Started Status Update");
            for (int j = 0; j < LWBindingConstants.features.size(); j++) {
                a = "{\"featureId\": " + LWBindingConstants.features.get(j).getFeatureId() + ",\"value\": 0}";
                //logger.warn("String {}", a);
                FeatureStatus b = gson.fromJson(a, FeatureStatus.class);
                LWBindingConstants.featureStatus.add(j, b);
            }
            logger.warn("createLists Feature Status size {}", LWBindingConstants.featureStatus.size());
            //logger.warn("feature Status First ID {}", featureStatus.get(0).getFeatureId().toString());  
        int partitionSize = 30;
        //List<List<FeatureStatus>> partitions = new ArrayList<>();
        for (int i = 0; i < LWBindingConstants.featureStatus.size(); i += partitionSize) {
            LWBindingConstants.partitions.add(LWBindingConstants.featureStatus.subList(i, Math.min(i + partitionSize, LWBindingConstants.featureStatus.size())));
        }}}
 /*           
        public void eachUpdate(int k) {
            String string1;
            String body = "{\"features\": [";
            String last = "";
            logger.warn("Started Update: {}",k);
            //for (int l = 0; l < LWBindingConstants.partitions.get(k).size(); l++) { 
            for (int l = 0; l < LWBindingConstants.featureSets.get(k).getFeatures().size(); l++) { 
                //logger.warn("feature Id: {}",partitions.get(k).get(l).getFeatureId().toString());
                //if (l < (LWBindingConstants.partitions.get(k).size() - 1)) {last = ",";} else {last = "]}";}   
                if (l < (LWBindingConstants.featureSets.get(k).getFeatures().size() - 1)) {last = ",";} else {last = "]}";}   
                string1 = "{\"featureId\": \"" + LWBindingConstants.featureSets.get(k).getFeatures().get(l).getFeatureId() + "\"}";
                //logger.warn("string: {}",string1);
                body = body + string1 + last;
            }
            //logger.warn("Body: {}",body);
            InputStream data = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));   
            String response1 = Http.httpClient("features",data,"application/json","");
            List<FeatureStatus> temp = new ArrayList<FeatureStatus>(); 
            String array1[] = response1.split(",");
                for (int j=0; j < array1.length; j++) {
                    String b = "{\"featureId\":" + array1[j].toString().replace(":",",\"value\": ").replace("{", "").replace("}", "") + "}";
                    //logger.warn("String b: {}",b);
                    FeatureStatus d = gson.fromJson(b, FeatureStatus.class);
                    temp.add(d);
                    }
                    //logger.warn("temp size: {}",temp.size());
            for (int m = 0; m < temp.size(); m++) {   
            int index = featureStatusIndex(temp.get(m).getFeatureId().toString());
            if (index != -1) {LWBindingConstants.featureStatus.set(index, temp.get(m));}
            } 
            logger.warn("Finished Update: {}",k);       
    }*/



}
