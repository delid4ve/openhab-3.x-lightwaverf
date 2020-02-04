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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    private LWAccountHandler  accountHandler;
    private ScheduledFuture<?> scanTask;
    ThingTypeUID thingTypeUid = null;
    ThingUID bridgeUID;
    String label;

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
            discover();
    }

    @Override
    protected void startScan() {
        // logger.debug("Start Scan");
        if (this.scanTask != null) {
            scanTask.cancel(true);
        }
        this.scanTask = scheduler.schedule(() -> {
            discover();
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

    private void discover() {
        if (LWBindingConstants.structures == null) {
            logger.debug("Structures didnt return any data");
        } else {
        for (int a = 0; a < LWBindingConstants.structures.size(); a++) {
                    String groupId = structures.get(a).getGroupId();
                    String name = structures.get(a).getName();
                    bridgeUID = accountHandler.getThing().getUID();
                    thingTypeUid = THING_TYPE_LIGHTWAVE_STRUCTURE;
                    ThingUID structureThing = new ThingUID(thingTypeUid, accountHandler.getThing().getUID(),
                    structures.get(a).getGroupId().toString().replace('-', '_').replace('+', '_'));
                    Map<String, Object> structureProperties = new HashMap<>();
                    structureProperties.put("structureId", groupId);
                    label = createLabelStructure(structures.get(a));
                    logger.debug("Found structure: {} : {}", label, structureProperties);
                    thingDiscovered(DiscoveryResultBuilder.create(structureThing).withLabel(label)
                        .withProperties(structureProperties)
                        .withRepresentationProperty(groupId)
                        .withBridge(bridgeUID).build());
                                List<Devices> devices = structures.get(a).getDevices();
                                logger.debug("Device Size: {}", devices.size());
                                devices.forEach(device -> {
                                    logger.debug("Device Id: {}", device.getDeviceId().toString());
                                    logger.debug("Device Name(): {}", device.getName().toString());
                                    String type = device.getCat().toString();
                                    if (type.contains("Heating")) {
                                        thingTypeUid = THING_TYPE_LIGHTWAVE_THERMOSTAT;
                                    }
                                    else if (type.contains("Hub")) {
                                        thingTypeUid = THING_TYPE_LIGHTWAVE_HUB;
                                    }
                                    else {
                                        thingTypeUid = THING_TYPE_LIGHTWAVE_DEVICE;
                                    }
                                    logger.debug("Creating Thing");
                                    ThingUID deviceThing = new ThingUID(thingTypeUid, accountHandler.getThing().getUID(),
                                    device.getDeviceId().toString().replace('-', '_').replace('+', '_'));
                                    logger.debug("Thing Created");
                                    Map<String, Object> deviceProperties = new HashMap<>();
                                    deviceProperties.put("deviceId", device.getDeviceId().toString());
                                    deviceProperties.put("Connected To Structure ID", groupId);
                                    deviceProperties.put("Connected To Structure Name", name);
                                    deviceProperties.put("Name", device.getName().toString());
                                    deviceProperties.put("Device", device.getDevice().toString());
                                    deviceProperties.put("Type", device.getType().toString());
                                    deviceProperties.put("Description", device.getDesc().toString());
                                    deviceProperties.put("Product", device.getProduct().toString());
                                    deviceProperties.put("Product Code", device.getProductCode().toString());
                                    deviceProperties.put("Category", device.getCat().toString());
                                    deviceProperties.put("Generation", device.getGen().toString());
                                    label = createLabelDevice(device);
                                    logger.debug("Found device: {} : {}", label, deviceProperties);
                                    thingDiscovered(DiscoveryResultBuilder.create(deviceThing).withLabel(label)
                                        .withProperties(deviceProperties)
                                        .withRepresentationProperty(device.getDeviceId().toString())
                                        .withBridge(bridgeUID).build());
                                   //line changed from Devices.getFeatureSets
                                        List<FeatureSets> featureSets = LWBindingConstants.featureSets;
                                logger.debug("Number Of Feature Sets Discovered: {}", featureSets.size());
                                featureSets.forEach(featureSet -> {
                                    
                                    
                                    logger.debug("FeatureSet Id: {}", featureSet.getFeatureSetId().toString());
                                    logger.debug("FeatureSet Name(): {}", featureSet.getName().toString());
                                    if (type.contains("Lighting") || type.contains("Power")) {
                                    if (type.contains("Lighting")) {
                                        thingTypeUid = THING_TYPE_LIGHTWAVE_DIMMER;
                                    }
                                    else if (type.contains("Power")) {
                                        thingTypeUid = THING_TYPE_LIGHTWAVE_SOCKET;
                                    }
                                    ThingUID featureSetThing = new ThingUID(thingTypeUid,
                                            accountHandler.getThing().getUID(), featureSet.getFeatureSetId()
                                                    .toString().replace('-', '_').replace('+', '_'));
                                    Map<String, Object> featureSetProperties = new HashMap<>();
                                    featureSetProperties.put("featureSetId", featureSet.getFeatureSetId().toString());
                                    featureSetProperties.put("Connected To Structure ID", groupId);
                                    featureSetProperties.put("Connected To Structure Name", name);
                                    featureSetProperties.put("Connected To Device ID", device.getDeviceId().toString());
                                    featureSetProperties.put("Connected To Device Name", device.getName().toString());
                                    label = createLabelFeatureSet(featureSet);
                                    logger.debug("Found featureset: {} : {}", label, featureSetProperties);
                                    thingDiscovered(DiscoveryResultBuilder.create(featureSetThing).withLabel(label)
                                            .withProperties(featureSetProperties)
                                            .withRepresentationProperty(featureSet.getFeatureSetId().toString())
                                            .withBridge(bridgeUID).build());
                                }
                                else {}
                            });
                                });
                       
                    }
                }
            }
    

    


    public String createLabelDevice(Devices deviceList) {
        StringBuilder sb = new StringBuilder();
        if (deviceList.getDevice().contains("thermostat")||
            deviceList.getDevice().contains("Link")) {
            sb.append("");
        } else {
            sb.append("Master - ");
        }
        sb.append(deviceList.getName());
        return sb.toString();
    }

    private String createLabelStructure(Root structureList) {
        StringBuilder sb = new StringBuilder();
        sb.append("Structure - ");
        sb.append(structureList.getName());
        return sb.toString();
    }

    private String createLabelFeatureSet(FeatureSets featureSetList) {
        StringBuilder sb = new StringBuilder();
        sb.append(featureSetList.getName());
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
