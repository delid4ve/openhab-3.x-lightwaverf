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

import java.io.IOException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.joda.time.DateTime;
import org.openhab.binding.lightwaverf.internal.UpdateListener;
import org.openhab.binding.lightwaverf.internal.api.FeatureStatus;
import org.openhab.binding.lightwaverf.internal.api.discovery.Devices;
import org.openhab.binding.lightwaverf.internal.api.discovery.FeatureSets;
import org.openhab.binding.lightwaverf.internal.api.discovery.Features;
import org.openhab.binding.lightwaverf.internal.api.discovery.Root;
import org.openhab.binding.lightwaverf.internal.api.discovery.StructureList;
import org.openhab.binding.lightwaverf.internal.config.AccountConfig;
import org.openhab.binding.lightwaverf.internal.Http;
import org.openhab.binding.lightwaverf.internal.LWDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.types.HSBType;
import java.awt.Color;

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which
 * are used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */
@NonNullByDefault
public class LWAccountHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(LWAccountHandler.class);
    private @Nullable List<Devices> devices;
    private Map<String, Long> locks = new HashMap<String,Long>();
    private @Nullable ScheduledFuture<?> connectionCheckTask;
    private @Nullable ScheduledFuture<?> connect;
    private @Nullable ScheduledFuture<?> tokenTask;
    private @Nullable ScheduledFuture<?> refreshTask;
    private @Nullable UpdateListener listener;
    private Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

    public LWAccountHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(LWDiscoveryService.class);
    }

    @Override
    public void thingUpdated(Thing thing) {
        dispose();
        this.thing = thing;
        initialize();
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Lightwave account handler.");
        //listener = new UpdateListener();
        try {
            listener = new UpdateListener();
            AccountConfig config = getConfigAs(AccountConfig.class);
            listener.login(config.username, config.password);
        } catch (IOException e) {
        }
        connect = scheduler.schedule(this::initialConnect,5, TimeUnit.SECONDS);
        connectionCheckTask = scheduler.schedule(this::startConnectionCheck,60, TimeUnit.SECONDS);
        tokenTask = scheduler.scheduleWithFixedDelay(this::getToken,24, 24, TimeUnit.HOURS);
    }

    private void initialConnect() {
        devices = new ArrayList<Devices>();
        try {
            devices = getDevices();
            for (int b = 0; b < devices.size(); b++) {
                addFeatureStatus(devices.get(b));
            }
        } catch (IOException e) {
        }
        properties();
        updateStatus(ThingStatus.ONLINE);
        refreshTask = scheduler.schedule(poll,5, TimeUnit.SECONDS);
    }

    private void getToken(){
        AccountConfig config = getConfigAs(AccountConfig.class);
        if (tokenTask == null || tokenTask.isCancelled()) {
            try {
                listener.login(config.username, config.password);
            }
            catch (Exception e) {
            }
        }
    }

    private StructureList getStructureList() throws IOException {
        String response = Http.httpClient("structures", null, null, null);
        StructureList structureList = gson.fromJson(response, StructureList.class);
        return structureList;
    }

    private Root getStructure(String structureId) throws IOException {
        String response = Http.httpClient("structure", null, null, structureId);
        Root structure = gson.fromJson(response, Root.class);
        return structure;
    }

    public List<Devices> getDevices() throws IOException {
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

    public boolean addFeatureStatus( Devices device ) {
        listener.addFeatureStatus(device);
        return true;
    }

    private void startConnectionCheck() {
            logger.debug("Start periodic connection check");
            Runnable runnable = () -> {
                logger.debug("Checking Lightwave connection");
                if (isConnected()) {
                    logger.debug("Connection to Lightwave in tact");
                } else {
                        try {
                        connect();
                    } catch (Exception e) {
                    }
                }
            };
            connectionCheckTask = scheduler.scheduleWithFixedDelay(runnable, 0, 60, TimeUnit.SECONDS);
        }

    private void connect() throws IOException {
        AccountConfig config = getConfigAs(AccountConfig.class);
        logger.debug("Initializing connection to Lightwave");
        updateStatus(ThingStatus.OFFLINE);
            listener.login(config.username, config.password);
            updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        logger.debug("Running dispose()");
        stopConnectionCheck();
        
        if (refreshTask != null) {
            refreshTask.cancel(true);
        }
        if (connectionCheckTask != null) {
            connectionCheckTask.cancel(true);
        }
        if (tokenTask != null) {
            tokenTask.cancel(true);
        }
        if (connect != null) {
            connect.cancel(true);
        }
            connectionCheckTask = null;
            refreshTask = null;
            tokenTask = null;
            connect = null;
    }

    private void properties() {
        Map<String, String> properties = editProperties();
        properties.clear();
        for (int i=0; i < devices.size(); i++) { 
            String deviceArray[] = devices.get(i).getDeviceId().split("-");
            String channelList = "Simple DeviceId (sdId): " + deviceArray[1] + ", Product: " + devices.get(i).getDesc() +
            ", Gen: " + devices.get(i).getGen() + ", Channels: " + devices.get(i).getFeatureSets().size() + "\r\n";
            properties.put("Connected Device: " + i + "", channelList);
        }
        updateProperties(properties);
    }

    private void polling() {
        logger.debug("Initiate Polling");
        List<String> channelList = new ArrayList<String>();
        channelList = channelList();
        List<List<String>> partitions = new ArrayList<List<String>>();
        int pollingInterval = Integer.valueOf(this.thing.getConfiguration().get("pollingInterval").toString());
        int partitionSize = Integer.valueOf(this.thing.getConfiguration().get("pollingGroupSize").toString());
        for (int i = 0; i < channelList.size(); i += partitionSize) {
            partitions.add(channelList.subList(i, Math.min(i + partitionSize, channelList.size())));
        }
        if (channelList.size() == 0) {
            logger.warn("Channel List For Updating Is Empty, rescheduling");
            refreshTask = scheduler.schedule(this::polling,10, TimeUnit.SECONDS);
        }
        else if (channelList.size() > 0) {
            try {
                logger.debug("Started polling");
                Map<String, Long> lockstemp = new HashMap<String,Long>();
                if (!locks.isEmpty()) {
                lockstemp.putAll(locks);
                }
                logger.debug("Start Listener");
                listener.updateListener(partitions,locks);
                logger.debug("Start Channel Update");
                channelUpdate();
                logger.debug("Finished Channel Update");
                for (Map.Entry<String, Long> map : lockstemp.entrySet()) {
                    String key = map.getKey();
                    Long value = map.getValue();
                    locks.remove(key, value);
                    logger.debug("lock removed: {} : {}", key, value);
                }
                logger.debug("Removed Redundant Locks");
                refreshTask = scheduler.schedule(poll, pollingInterval, TimeUnit.SECONDS);
                logger.debug("Scheduled refresh");
            } catch (Exception e) {
            }
        }
    } 

    private Devices device(String sdId) {
        Devices device = null;
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getDeviceId().contains("-" + sdId + "-")) {
                device = devices.get(i);
            }
        }
        return device;
    }

    public Features getFeature(String sdId, int featureSetNo,String channelId) {
        Devices device = device(sdId);
        String featureSetId = device.getFeatureSets().get(featureSetNo).getFeatureSetId();
        FeatureSets featureSet = featureSets().stream().filter(i -> featureSetId.equals(i.getFeatureSetId())).findFirst()
                .orElse(featureSets().get(0));
        if(channelId.contains("energy") || channelId.contains("power")  ) {
        return featureSet.getFeatures().stream().filter(i -> channelId.contains(i.getType())).findFirst()
                .orElse(featureSet.getFeatures().get(0));
        }
        else {        
        return featureSet.getFeatures().stream().filter(i -> channelId.equals(i.getType())).findFirst()
                .orElse(featureSet.getFeatures().get(0));
        }
    }

    private void stopConnectionCheck() {
        if (connectionCheckTask != null) {
            logger.debug("Stop periodic connection check");
            connectionCheckTask.cancel(true);
            connectionCheckTask = null;
        }
    }

    private FeatureStatus getFeatureStatus(String featureId) {
        List<FeatureStatus> featureStatus = featureStatus();
        return featureStatus().stream().filter(i -> featureId.equals(i.getFeatureId())).findFirst()
            .orElse(featureStatus.get(0));
    }

    private Runnable poll = new Runnable() {
        @Override
        public void run() {
            polling();
        }
    };

    private List<String> channelList() {
        List<String> channelList = new ArrayList<String>();
        List<Thing> things = new ArrayList<Thing>();
        things = this.getThing().getThings();
        for (int i=0; i < things.size(); i++) {
            String sdId = things.get(i).getConfiguration().get("sdId").toString();
            for (Channel channel : things.get(i).getChannels()) {
                if (isLinked(channel.getUID())) {
                    String channelName = channel.getUID().getIdWithoutGroup();
                    String channelId = channel.getUID().getId().toString();
                    int channelNo = (Integer.parseInt(channelId.substring(0,1))-1);
                    Features feature = getFeature(sdId,channelNo, channelName);
                    channelList.add(feature.getFeatureId());
                }
            }
        }
        return channelList;          
    }
    
    private void channelUpdate() {
        List<Thing> things = new ArrayList<Thing>();
        String channelHelper = "";
        things = this.getThing().getThings();
        for (int i=0; i < things.size(); i++) {
            String sdId = things.get(i).getConfiguration().get("sdId").toString();
            for (Channel channel : things.get(i).getChannels()) {
                if (isLinked(channel.getUID())) {
                    Double electricityCost = Double.parseDouble(this.getThing().getConfiguration().get("electricityCost").toString()) / 100;
                    String channelName = channel.getUID().getIdWithoutGroup();
                    
                    if(channelName == "energyCost") {
                        channelHelper = "energy";
                    }
                    else if(channelName == "powerCost") {
                        channelHelper = "power";
                    }
                    else {
                        channelHelper = channelName;
                    }

                    String channelId = channel.getUID().getId().toString();
                    ChannelUID channelUid = channel.getUID();
                    int channelNo = (Integer.parseInt(channelId.substring(0,1))-1);
                    Features feature = getFeature(sdId,channelNo, channelHelper);
                    String featureId = feature.getFeatureId();
                    if(!locks.containsKey(featureId)) {
                        FeatureStatus status = getFeatureStatus(featureId);
                        Long value = status.getValue();   
                        if(value != -1) {
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
                                    updateState(channelUid, OnOffType.ON);
                                } else {
                                    updateState(channelUid, OnOffType.OFF);
                                }
                                break;
                            case "power": 
                            case "rssi": 
                            case "timeZone":
                            case "day":
                            case "month":
                            case "year":
                                updateState(channelUid, new DecimalType(value));
                                break;
                            case "powerCost": 
                                updateState(channelUid, new DecimalType((double)value * electricityCost / 1000));
                                break;
                            case "energy":
                                updateState(channelUid, new DecimalType((double)value / 1000));
                                break;
                            case "energyCost":
                                updateState(channelUid, new DecimalType((double)value / 1000 * electricityCost));
                                break;
                            case "temperature":
                            case "targetTemperature":
                            case "voltage":
                                updateState(channelUid, new DecimalType((double)value / 10));
                                break;
                            case "dimLevel":
                            case "valveLevel":
                                updateState(channelUid, new PercentType(value.intValue()));
                                break;
                            case "batteryLevel":
                                updateState(channelUid, new DecimalType(value));
                                break;
                            case "periodOfBroadcast":
                            case "monthArray":
                            case "weekdayArray":
                                updateState(channelUid, new DecimalType(value));
                                break;
                            case "locationLongitude":
                            case "locationLatitude":
                                updateState(channelUid, new StringType(new DecimalType((double)value / 1000000).toString()));
                                break;
                            case "rgbColor":
                                Color color = new Color((value.intValue()));
                                int red = (color.getRed());
                                int green = (color.getGreen());
                                int blue = (color.getBlue());
                                float[] hsb = Color.RGBtoHSB(red, green, blue, null);
                                int hue = (int) Math.round(hsb[0] * 360);
                                int saturation = (int) Math.round(hsb[1] * 100);
                                int brightness = (int) Math.round(hsb[2] * 100);  
                                String hsb1 = hue + "," + saturation + "," + brightness;
                                updateState(channelUid, new HSBType(hsb1));
                                break;
                            case "date":
                                String monthPad = "";
                                String dayPad = "";
                                String hex = Integer.toHexString(value.intValue());
                                int year = Integer.parseInt(hex.substring(0, 3),16);
                                int month = Integer.parseInt(hex.substring(3, 4),16);
                                int day = Integer.parseInt(hex.substring(4, 6),16);
                                if (month < 10) {
                                    monthPad = "0";
                                }
                                if (day < 10) {
                                    dayPad = "0";
                                }
                                String dateValue = year + "-" + monthPad + month + "-" + dayPad + day + "T00:00:00.000+0000";
                                updateState(channelUid,new DateTimeType(dateValue));
                                break;
                            case "currentTime":
                                DateTimeType time = new DateTimeType(new DateTime(value*1000).toString());
                                updateState(channelUid, time);
                                break;
                            case "duskTime":
                            case "dawnTime":
                            case "time":
                                    String hoursPad = "";
                                    String minsPad = "";
                                    String secsPad = "";
                                    int minutes = (((int) value.intValue() / 60)%60);
                                    int hours = ((int) value.intValue() / 3600);
                                    int seconds = ((int) value.intValue() % 60);
                                    if (hours < 10) {
                                        hoursPad = "0";
                                    }
                                    if (minutes < 10) {
                                        minsPad = "0";
                                    }
                                    if (seconds < 10) {
                                        secsPad = "0";
                                    } 
                                    String timeValue = hoursPad + hours + ":" + minsPad + minutes + ":" + secsPad + seconds ;
                                    updateState(channelUid, new DateTimeType(timeValue));
                                    break;
                                case "weekday":
                                    if (value.intValue() > 0) {
                                        int weekday = value.intValue();
                                        String weekday1 = DayOfWeek.of(weekday).toString();
                                        updateState(channelUid, new StringType(weekday1));
                                        break;
                                    } else {
                                    break; 
                                    }
                            }
                        }
                    } else {
                        logger.debug("channel locked"); 
                    }   
                }
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }
    
    public boolean isConnected() {
           return listener.isConnected();
    }
    
    public boolean addLocked( String featureId, Long time ) {
        locks.put(featureId,time);
        return true;
    }
    
    public @Nullable List<Devices> devices() {
        return devices;
    }
    
    public Map<String,Long> locks() {
        return locks;
    }
    
    public List<FeatureSets> featureSets() {
        List<FeatureSets> featureSets = new ArrayList<FeatureSets>();
        for (int b = 0; b < devices.size(); b++) {   
            featureSets.addAll(devices.get(b).getFeatureSets());
        }
        return featureSets;
    }
    
    public List<FeatureStatus> featureStatus() {
        return listener.featureStatus();
    }
}
