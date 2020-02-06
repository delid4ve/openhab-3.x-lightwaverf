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

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.awt.Color;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.joda.time.DateTime;
import org.openhab.binding.lightwaverf.internal.Http;
import org.openhab.binding.lightwaverf.internal.LWBindingConstants;
import org.openhab.binding.lightwaverf.internal.UpdateListener;
import org.openhab.binding.lightwaverf.internal.api.discovery.Devices;
import org.openhab.binding.lightwaverf.internal.api.discovery.FeatureSets;
import org.openhab.binding.lightwaverf.internal.api.discovery.Features;
import org.openhab.binding.lightwaverf.internal.config.DeviceConfig;
import org.openhab.binding.lightwaverf.internal.LWDiscoveryService;
import org.eclipse.smarthome.core.library.types.*;
import org.openhab.binding.lightwaverf.internal.api.FeatureStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import com.google.gson.JsonObject;
import java.util.concurrent.TimeUnit;

public class DeviceHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(DeviceHandler.class);
    private DeviceConfig config;
    String value;
    //private ChannelGroupTypeProvider groups;
    private LWAccountHandler accountHandler;
    private ScheduledFuture<?> refreshTask;
    private static List<FeatureSets> featureSets = LWBindingConstants.featureSets;
    private static List<FeatureStatus> featureStatus = LWBindingConstants.featureStatus;
    private static List<Features> features = LWBindingConstants.features;
    private String deviceId = this.thing.getProperties().get("Device ID").toString();
    private static List<Devices> devices = LWBindingConstants.devices;
    private Devices device = null;
    private Features feature = null;    
    private FeatureStatus status = null;
    private String featureSetId = null;
    private int i;
    public static int partitionSize;
    private String groupType;
    private String noChannels = this.thing.getProperties().get("Channels");
    public int pollingInterval;
    private String channelId;
    private String channelTypeId;
    private int channelValue;
    private FeatureSets featureSet = null;
    List<String> channelList = UpdateListener.channelList;
    private ScheduledFuture<?> listTask;
    int refresh;

    public DeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing {} handler.", getThing().getThingTypeUID());
        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge Not set");
            return;
        }
        config = getConfigAs(DeviceConfig.class);
        logger.debug("Device Config: {}", config);
        initializeBridge(bridge.getHandler(), bridge.getStatus());
        pollingInterval = Integer.valueOf(this.getBridge().getConfiguration().get("pollingInterval").toString());
        partitionSize = Integer.valueOf(this.getBridge().getConfiguration().get("pollingGroupSize").toString());
        refreshTask = scheduler.scheduleWithFixedDelay(this::updateChannels, 11, pollingInterval, TimeUnit.SECONDS);
        
    }

    @Override
    public void dispose() {
        logger.debug("Running dispose()");
        if (refreshTask != null) {
            refreshTask.cancel(true);
            refreshTask = null;
        }
        if (listTask != null) {
            listTask.cancel(true);
            listTask = null;
        }
        accountHandler = null;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(LWDiscoveryService.class);
    }

    public ThingUID getID() {
        return getThing().getUID();
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("bridgeStatusChanged {} for thing {}", bridgeStatusInfo, getThing().getUID());
        Bridge bridge = getBridge();
        if (bridge != null) {
            initializeBridge(bridge.getHandler(), bridgeStatusInfo.getStatus());
        }
    }

    private void initializeBridge(ThingHandler thingHandler, ThingStatus bridgeStatus) {
        logger.debug("initializeBridge {} for thing {}", bridgeStatus, getThing().getUID());

        if (thingHandler != null && bridgeStatus != null) {
            accountHandler = (LWAccountHandler) thingHandler;
            if (bridgeStatus == ThingStatus.ONLINE) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void channelUnlinked(final ChannelUID channelUID) {
        Objects.requireNonNull(channelUID, "channelUID cannot be null");
        String ch = channelUID.getIdWithoutGroup().toString();
        int group =  (Integer.parseInt(channelUID.getGroupId()) -1);
        feature = getFeature(group,ch);
        if(channelList.contains(feature.getFeatureId()) == true) {
            channelList.remove(feature.getFeatureId());
        }
        super.channelUnlinked(channelUID);
    }

    @Override
    public void channelLinked(final ChannelUID channelUID) {
        Objects.requireNonNull(channelUID, "channelUID cannot be null");
        String ch = channelUID.getIdWithoutGroup().toString();
        int group =  (Integer.parseInt(channelUID.getGroupId()) -1);
        feature = getFeature(group,ch);
        if(channelList.contains(feature.getFeatureId()) == false) {
            channelList.add(feature.getFeatureId());
        }
        super.channelLinked(channelUID);
    }

    private Features getFeature(int featureSetNo, String channelId) { 
        device = devices.stream().filter(i -> deviceId.equals(i.getDeviceId())).findFirst().orElse(devices.get(0));
        featureSetId = device.getFeatureSets().get(featureSetNo).getFeatureSetId().toString();
        featureSet = featureSets.stream()
        .filter(i -> featureSetId.equals(i.getFeatureSetId())).findFirst().orElse(featureSets.get(0));
        return featureSet.getFeatures().stream()
        .filter(i -> channelId.equals(i.getType())).findFirst().orElse(features.get(0));
    } 

    private FeatureStatus getFeatureStatus(String featureId) { 
        return featureStatus.stream().filter(i -> featureId.equals(i.getFeatureId()))
        .findFirst().orElse(featureStatus.get(0));
    }

    private void updateChannels() {
        i = (int) Double.parseDouble(noChannels);
        for (int j=0; j < i; j++) { 
            groupType = (j+1) + "";
                for (Channel group : getThing().getChannelsOfGroup(groupType)) {
                    if (isLinked(group.getUID())) {
                        channelTypeId = group.getChannelTypeUID().getId();
                        feature = getFeature(j,channelTypeId);                    
                        if(channelList.contains(feature.getFeatureId()) == false) {
                            channelList.add(feature.getFeatureId());
                            logger.debug("channel added to update list {} for featureId {}", group.getUID().getId().toString(),feature.getFeatureId());
                        }
                        status = getFeatureStatus(feature.getFeatureId());
                        channelValue = status.getValue();
                        channelId = group.getUID().getId().toString();
                        updateChannels(channelId, channelValue,group.getUID().getIdWithoutGroup());
                        logger.debug("value {} sent to {} with channel name {}",channelValue,group.getUID().getId().toString(),group.getUID().getIdWithoutGroup()); 
                    }
                }     
        }
    }

    private void updateChannels(String channelId, Integer value,String channelName) {
        
        switch (channelName) {
        case "switch":
        case "diagnostics":
        case "outletInUse":
        case "protection":
        case "identify":
        case "reset":
        case "upgrade":
        case "heatState":
        case "callForHeat":
        case "bulbSetup":
        case "dimSetup":
            if (value == 1) {
                updateState(channelId, OnOffType.ON);
            } else {
                updateState(channelId, OnOffType.OFF);
            }
            break;
        case "power":
        case "energy":
            updateState(channelId, new DecimalType(Float.parseFloat(value.toString()) / 100));
            break;
        case "rssi":
        case "temperature":
        case "targetTemperature":
        case "voltage":
            updateState(channelId, new DecimalType(Float.parseFloat(value.toString()) / 10));
            break;
        case "dimLevel":
        case "valveLevel":
        case "batteryLevel":
            updateState(channelId, new DecimalType(value));
            break;
        case "rgbColor":
            Color color = new Color(value);
            Number red = color.getRed();
            Number green = color.getGreen();
            Number blue = color.getBlue();
            updateState(channelId, new StringType("(" + red.toString() + "," + green.toString() + "," + blue.toString() + ")"));
            break;
        case "periodOfBroadcast":
        case "monthArray":
        case "weekdayArray":
        case "day":
        case "month":
        case "year":
        case "timeZone":
            updateState(channelId, new StringType(value.toString()));
            break;
        case "date":
            Number abc = ((value).longValue()*100000);
            DateTimeType date = new DateTimeType(new DateTime(abc).toString());
            updateState(channelId, new StringType(date.toString()));
        break;
        case "currentTime":
            Number def = ((value).longValue()*1000);
            DateTimeType time = new DateTimeType(new DateTime(def).toString());
            updateState(channelId, new StringType(time.toString()));
            break;
        case "duskTime":
        case "dawnTime":
        case "time":
            String hoursPad = "";
            String minsPad = "";
            String secsPad = "";
            int minutes = ((Integer.parseInt(value.toString()) / 60)%60);
            int hours = (value / 3600);
            int seconds = (Integer.parseInt(value.toString()) % 60);
            if (hours < 10) {
                hoursPad = "0";
            }
            if (minutes < 10) {
                minsPad = "0";
            }
            if (seconds < 10) {
                secsPad = "0";
            }
            String timeValue = hoursPad + hours + ":" + minsPad + minutes + ":" + secsPad + seconds;
            updateState(channelId, new StringType(timeValue.toString()));
            break;
        case "weekday":
            if (value != 0) {
                updateState(channelId, new StringType(DayOfWeek.of(value).toString()));
                break;
            } else {
                break;
            }
        case "locationLongitude":
        case "locationLatitude":
            updateState(channelId, new StringType(new DecimalType(Float.parseFloat(value.toString()) / 1000000).toString()));
            break;
        }
        logger.debug("channel {} received update", channelId);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String channelName = channelUID.getIdWithoutGroup();
        String value = "";
        String group = channelUID.getGroupId();
        String channelId = channelUID.getId();
        i = ((int) Double.parseDouble(group) - 1);
        logger.debug("i: {}", i);
        logger.debug("Group Id: {}", group);
        logger.debug("Channel Name: {}", channelName);
        logger.debug("Channel Id: {}", channelId);

        if (command instanceof RefreshType) {
            logger.debug("Refresh command not supported");
            return;
        }
        if (accountHandler == null) {
            logger.warn("No connection to Lightwave available, ignoring command");
            return;
        }
        feature = getFeature(i,channelName);
        switch (channelName) {
        case "switch":
        case "diagnostics":
        case "outletInUse":
        case "protection":
        case "identify":
        case "reset":
        case "upgrade":
        case "heatState":
        case "callForHeat":
        case "bulbSetup":
        case "dimSetup":
            if (command.toString() == "ON") {
                value = "1";
            } else {
                value = "0";
            }
            break;
            case "rgbColor":
        case "timeZone":
        case "locationLongitude":
        case "locationLatitude":
        case "dimLevel":
        case "valveLevel":
            value = (new DecimalType(Float.parseFloat(command.toString()))).toString();
            break;
            case "temperature":
            case "targetTemperature":
            value = (new DecimalType((Float.parseFloat(command.toString())) * 10)).toString();
            break;
        default:
            value = "-1";
        }
        logger.debug("channel: {}", channelUID.getId());
        logger.debug("value: {}", value);
        try {
            setStatus(feature.getFeatureId(), value);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void setStatus(String featureId, String value) throws Exception {
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("value", value);
        InputStream data = new ByteArrayInputStream(jsonReq.toString().getBytes(StandardCharsets.UTF_8));
        Http.httpClient("feature", data, "application/json",featureId);  
        logger.debug("Command sent");       
    }
}
