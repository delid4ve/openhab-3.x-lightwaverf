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

import static org.openhab.binding.lightwaverf.internal.LWBindingConstants.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

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
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DeviceHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(DeviceHandler.class);
    private DeviceConfig config;
    String value;
    private LWAccountHandler accountHandler;
    private ScheduledFuture<?> refreshTask;
    // private final static Gson gson = new
    // GsonBuilder().excludeFieldsWithoutExposeAnnotation()
    // .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
    List<FeatureSets> featureSets = LWBindingConstants.featureSets;
    List<FeatureStatus> featureStatus = LWBindingConstants.featureStatus;
    List<Devices> devices = LWBindingConstants.devices;
    private String s = this.thing.getConfiguration().get("deviceId").toString();
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
        if (listTask == null || listTask.isCancelled()) {
            listTask = scheduler.schedule(this::createUpdateList, 5, TimeUnit.SECONDS);
        }

    }

    private void createUpdateList() {
        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID())) {
                s = this.thing.getConfiguration().get("deviceId").toString();
                Devices device = devices.stream().filter(i -> s.equals(i.getDeviceId())).findFirst()
                        .orElse(devices.get(0));
                String featureSetId = device.getFeatureSets().get(0).getFeatureSetId().toString();
                FeatureSets featureSet = LWBindingConstants.featureSets.stream()
                        .filter(i -> featureSetId.equals(i.getFeatureSetId())).findFirst().orElse(featureSets.get(0));
                Features feature = featureSet.getFeatures().stream()
                        .filter(i -> channel.getUID().getId().equals(i.getType())).findFirst().orElse(features.get(0));
                channelList.add(feature.getFeatureId());
                logger.debug("channel added to update list {}", channel.getUID().toString());
            }
        }
        startRefresh();
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

    private void startRefresh() {
        int refresh;
        if (Integer.valueOf(this.getBridge().getConfiguration().get("pollingInterval").toString()) < 5) {
            refresh = 5;
        } else {
            refresh = Integer.valueOf(this.getBridge().getConfiguration().get("pollingInterval").toString());
        }
        if (refreshTask == null || refreshTask.isCancelled()) {
            refreshTask = scheduler.scheduleWithFixedDelay(this::updateStateAndChannels, 15, refresh, TimeUnit.SECONDS);
        }
    }

    private void updateStateAndChannels() {
        if (accountHandler.isConnected()) {
            logger.debug("Update device '{}' channels", getThing().getLabel().toString());
            // updateState();
            updateChannels();
        } else {
            logger.debug("Connection to Lightwave is down");
        }
    }

    private synchronized void updateChannels() {
        Devices device = devices.stream().filter(i -> s.equals(i.getDeviceId())).findFirst().orElse(devices.get(0));
        String t = device.getFeatureSets().get(0).getFeatureSetId().toString();
        FeatureSets featureSet = LWBindingConstants.featureSets.stream().filter(i -> t.equals(i.getFeatureSetId()))
                .findFirst().orElse(LWBindingConstants.featureSets.get(0));
        for (Channel channel : getThing().getChannels()) {
            Features feature = featureSet.getFeatures().stream()
                    .filter(i -> channel.getUID().getId().equals(i.getType())).findFirst()
                    .orElse(LWBindingConstants.features.get(0));
            FeatureStatus status = featureStatus.stream().filter(i -> feature.getFeatureId().equals(i.getFeatureId()))
                    .findFirst().orElse(LWBindingConstants.featureStatus.get(0));
            Integer value = status.getValue();
            updateChannels(channel.getUID().getId(), value);
        }
    }

    private void updateChannels(String channelId, Integer value) {
        switch (channelId) {
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
        case "batteryLevel":
        case "temperature":
        case "targetTemperature":
        case "voltage":
            updateState(channelId, new DecimalType(Float.parseFloat(value.toString()) / 10));
            break;
        case "dimLevel":
        case "valveLevel":
            updateState(channelId, new DecimalType(value));
            break;
        case "rgbColor":
        case "periodOfBroadcast":
        case "date":
        case "monthArray":
        case "weekdayArray":
        case "day":
        case "month":
        case "year":
        case "timeZone":
            updateState(channelId, new StringType(value.toString()));
            break;
        case "currentTime":
            DateTime date = new DateTime((Long.parseLong(value.toString())));
            updateState(channelId, new StringType(date.toString()));
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
        logger.debug("Finished Channel Update for {}", this.thing.toString());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String value = "";
        if (command instanceof RefreshType) {
            logger.debug("Refresh command not supported");
            return;
        }
        if (accountHandler == null) {
            logger.warn("No connection to Lightwave available, ignoring command");
            return;
        }
        logger.debug("Received command '{}' to channel {}", command, channelUID);
        String fSId = this.thing.getConfiguration().get("featureSetId").toString();
        Optional<FeatureSets> featureSetStatus = LWBindingConstants.featureSets.stream()
                .filter(i -> fSId.equals(i.getFeatureSetId())).findFirst();
        // FeatureSets cmdtoSend = getFeatureSetStatusCopy(featureSetStatus);
        Features Status = featureSetStatus.get().getFeatures().stream()
                .filter(i -> channelUID.getId().equals(i.getType())).findAny().orElse(null);

        switch (channelUID.getId()) {
        case CHANNEL_SWITCH:
        case CHANNEL_PROTECTION:
        case CHANNEL_IDENTIFY:
        case CHANNEL_RESET:
        case CHANNEL_UPGRADE:
        case CHANNEL_DIAGNOSTICS:
        case CHANNEL_BULB_SETUP:
        case CHANNEL_DIM_SETUP:
            if (command.toString() == "ON") {
                value = "1";
            } else {
                value = "0";
            }
            break;
        case CHANNEL_RGB_COLOR:
        case CHANNEL_TIME_ZONE:
        case CHANNEL_LOCATION_LATITUDE:
        case CHANNEL_LOCATION_LONGITUDE:
        case CHANNEL_DIM_LEVEL:
        case CHANNEL_VALVE_LEVEL:
            command.toString();
            break;
        case CHANNEL_TEMPERATURE:
        case CHANNEL_TARGET_TEMPERATURE:
            new DecimalType((Float.parseFloat(command.toString())) * 10);
            break;
        default:
            value = "-1";
        }
        logger.debug("channel: {}", channelUID.getId());
        logger.debug("value: {}", value);
        try {
            setStatus(Status.getFeatureId(), value);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void setStatus(String featureId, String value) throws Exception {
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("value", value);
        InputStream data = new ByteArrayInputStream(jsonReq.toString().getBytes(StandardCharsets.UTF_8));
        //logger.debug("json: {}", data.toString());
        Http.httpClient("feature", data, "application/json",featureId);  
        logger.debug("Command sent");            
    }

}
