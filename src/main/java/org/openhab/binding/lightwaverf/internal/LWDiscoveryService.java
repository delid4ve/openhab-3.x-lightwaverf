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

import static org.openhab.binding.lightwaverf.internal.LWBindingConstants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.openhab.binding.lightwaverf.internal.LWBindingConstants;
import org.openhab.binding.lightwaverf.internal.api.AccessToken;
import org.openhab.binding.lightwaverf.internal.api.discovery.*;
import org.openhab.binding.lightwaverf.internal.handler.*;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LWDiscoveryService extends AbstractDiscoveryService implements DiscoveryService, ThingHandlerService {

    private final Logger logger = LoggerFactory.getLogger(LWDiscoveryService.class);

    private static final int DISCOVER_TIMEOUT_SECONDS = 10;

    private LWAccountHandler accountHandler;
    private ScheduledFuture<?> scanTask;
    ThingTypeUID thingTypeUid = null;
    ThingUID bridgeUID;
    String label;
    private String sId;
    private String sName;
    private String dId;
    private String sdId;
    private String dType;
    private String dProductCode;
    private String array1[];
    private List<StructureList> structureList = new ArrayList<StructureList>();
    private List<Root> structures = new ArrayList<Root>();
    private final static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

    public LWDiscoveryService() {
        super(LWBindingConstants.DISCOVERABLE_THING_TYPE_UIDS, DISCOVER_TIMEOUT_SECONDS, true);
    }

    @Override
    protected void activate(Map<String, @Nullable Object> configProperties) {
        // logger.debug("Activate Background Discovery");
        super.activate(configProperties);
    }

    @Override
    public void deactivate() {
        // logger.debug("Deactivate Background discovery");
        super.deactivate();
    }

    @Override
    @Modified
    protected void modified(Map<String, @Nullable Object> configProperties) {
        super.modified(configProperties);
    }

    @Override
    public void startBackgroundDiscovery() {
        logger.debug("Start Background Discovery");
        try {
            discover();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void startScan() {
        // logger.debug("Start Scan");
        if (this.scanTask != null) {
            scanTask.cancel(true);
        }
        this.scanTask = scheduler.schedule(() -> {
            try {
                discover();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }, 0, TimeUnit.SECONDS);
    }

    @Override
    protected void stopScan() {
        // logger.debug("Stop Scan");
        super.stopScan();

        if (this.scanTask != null) {
            this.scanTask.cancel(true);
            this.scanTask = null;
        }
    }

    public Properties getHeader() {
        Properties headers = new Properties();
        headers.put("Authorization", "Bearer " + AccessToken.getToken());
        return headers;
    }

    private void discover() throws Exception {
        logger.debug("Start Discovery");
            bridgeUID = accountHandler.getThing().getUID();
            if (structures == null || structures.isEmpty() == true) {
                String response = Http.httpClient("structures", null, null, null);
                logger.debug("Structure Response: {}", response);
                StructureList structureResponse = gson.fromJson(response, StructureList.class);
                structureList.add(structureResponse);
                for (int sR = 0; sR < structureList.size(); sR++) {
                    for (int sL = 0; sL < structureList.get(sR).getStructures().size(); sL++) {
                    String response2 = Http.httpClient("structure", null, null, structureList.get(sR).getStructures().get(sL).toString());
                    logger.debug("Structure List Response: {}", response2);
                    Root structure = gson.fromJson(response2, Root.class);
                    structures.add(structure);
                    logger.debug("Amount Of Structures: {}", structures.size());
                    }
                }
            }
                    for (int s = 0; s < structures.size(); s++) {
                        sId = structures.get(s).getGroupId();
                        sName = structures.get(s).getName();
                        logger.debug("Structure Id {} with name {} Found", sId,sName);
                            for (int d = 0; d < structures.get(s).getDevices().size(); d++) {
                            dType = structures.get(s).getDevices().get(d).getCat().toString();
                            dId = structures.get(s).getDevices().get(d).getDeviceId().toString();
                            array1 = dId.split("-");
                            sdId = array1[1].toString();
                        logger.debug("Device Simplified Id: {}", sdId);
                            dProductCode = structures.get(s).getDevices().get(d).getProductCode().toString();
                            logger.debug("Product Code discovered: {}", "'" + dProductCode + "'");
                            switch (dProductCode) {
                                case "L2":
                                thingTypeUid = THING_TYPE_LIGHTWAVE_HUB;
                                break;
                                case "LW600" :
                                thingTypeUid = THING_TYPE_EMONITOR_GEN1;
                                break;
                                case "LW320" : case "LW380" :
                                thingTypeUid = THING_TYPE_SSOCKET_GEN1;
                                break;
                                case "L42" :
                                thingTypeUid = THING_TYPE_DSOCKET_GEN2;
                                break;
                                case "L21" : case "L21MK2" :
                                thingTypeUid = THING_TYPE_SDIMMER_GEN2;
                                break;
                                case "L22" : case "L22MK2" :
                                thingTypeUid = THING_TYPE_DDIMMER_GEN2;
                                break;
                                case "L23" : case "L23MK2" :
                                thingTypeUid = THING_TYPE_TDIMMER_GEN2;
                                break;
                                case "L24" : case "L24MK2" :
                                thingTypeUid = THING_TYPE_QDIMMER_GEN2;
                                break;
                                case "LW921" :
                                thingTypeUid = THING_TYPE_THERMOSTAT;
                                break;
                                default : 
                                thingTypeUid = THING_TYPE_UNKNOWNDEVICE;
                            }
                            logger.debug("Used thingTypeUID: {}", thingTypeUid.toString());
                            ThingUID deviceThing = new ThingUID(thingTypeUid, accountHandler.getThing().getUID(),sdId);
                                    Map<String, Object> dProperties = new HashMap<>();
                                    dProperties.put("Device ID", dId);
                                    dProperties.put("sdId", sdId);
                                    dProperties.put("Connected To Structure ID", sId);
                                    dProperties.put("Connected To Structure Name", sName);
                                    dProperties.put("Name", structures.get(s).getDevices().get(d).getName().toString());
                                    dProperties.put("Device", structures.get(s).getDevices().get(d).getDevice().toString());
                                    dProperties.put("Type", dType);
                                    dProperties.put("Description", structures.get(s).getDevices().get(d).getDesc().toString());
                                    dProperties.put("Product", structures.get(s).getDevices().get(d).getProduct().toString());
                                    dProperties.put("Product Code", structures.get(s).getDevices().get(d).getProductCode().toString());
                                    dProperties.put("Category", structures.get(s).getDevices().get(d).getCat().toString());
                                    dProperties.put("Generation", structures.get(s).getDevices().get(d).getGen().toString());
                                    dProperties.put("Channels", structures.get(s).getDevices().get(d).getFeatureSets().size());
                                    label = createLabelDevice(structures.get(s).getDevices().get(d));
                                    thingDiscovered(DiscoveryResultBuilder.create(deviceThing).withLabel(label)
                                        .withProperties(dProperties)
                                        .withRepresentationProperty(sdId.toString())
                                        .withBridge(bridgeUID)
                                        .build());
                            }
                    }
                
            
        }
    
    public String createLabelDevice(Devices deviceList) {
        StringBuilder sb = new StringBuilder();
        sb.append(deviceList.getName());
        return sb.toString();
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof LWAccountHandler) {
            accountHandler = (LWAccountHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return accountHandler;
    }

    


}
