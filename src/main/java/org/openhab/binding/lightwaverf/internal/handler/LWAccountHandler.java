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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
import com.google.gson.reflect.TypeToken;

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
import org.openhab.binding.lightwaverf.internal.api.ChannelListItem;
import org.openhab.binding.lightwaverf.internal.api.discovery.Devices;
import org.openhab.binding.lightwaverf.internal.api.discovery.FeatureSets;
import org.openhab.binding.lightwaverf.internal.api.discovery.Features;
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
    private @Nullable Map<String, Long> locks;
    private @Nullable List<ChannelListItem> linkList;
    private @Nullable ScheduledFuture<?> pollingCheck;
    private @Nullable ScheduledFuture<?> tokenTask;
    private @Nullable ScheduledFuture<?> refreshTask;
    private @Nullable Thread pollingThread;
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
    public void initialize() {
        logger.debug("Initializing Lightwave account handler.");
        logger.warn("Polling Interval set to {} milliseconds",Long.parseLong(this.thing.getConfiguration().get("pollingInterval").toString()));
        if (this.thing.getConfiguration().get("pollingInterval").toString() == "0") {
            logger.debug("Polling interval needs to be greater than 0 milliseconds");
            updateStatus(ThingStatus.OFFLINE);
        }
        else{
            linkList = Collections.synchronizedList(new ArrayList<ChannelListItem>());
            locks = new HashMap<String,Long>();
            devices = new ArrayList<Devices>();
            updateToken();
            updateDevices();
            updateProperties();
            createTasks();
            updateStatus(ThingStatus.ONLINE);
        }
    }

    private void updateToken() {
        AccountConfig config = getConfigAs(AccountConfig.class);
        Http http = new Http();
        http.getToken(config.username,config.password);
    }

    private void updateDevices() {
        Http http = new Http();
        devices = http.getDevices();
    }

    private void createTasks() {
        long pollingInterval = Long.parseLong(this.thing.getConfiguration().get("pollingInterval").toString());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                pollingThread = Thread.currentThread();
                if (!pollingThread.isInterrupted()) { polling(); }
                //polling();
            }
        };
        refreshTask = scheduler.scheduleWithFixedDelay(runnable,10000, pollingInterval, TimeUnit.MILLISECONDS);
        tokenTask = scheduler.scheduleWithFixedDelay(this::updateToken,24, 24, TimeUnit.HOURS);
    }
  
    @Override
    public void thingUpdated(Thing thing) {
        this.thing = thing;
        dispose();    
        initialize();
    }

    @Override
    public void dispose() {
        logger.debug("Running dispose()");
        pollingThread.interrupt(); 
        if (refreshTask != null) {
            refreshTask.cancel(true);
        }
        if (tokenTask != null) {
            tokenTask.cancel(true);
        }
        if (pollingCheck != null) {
            pollingCheck.cancel(true);
        }
            pollingCheck = null;
            refreshTask = null;
            tokenTask = null;
            linkList = null;
            locks = null;
            devices = null;
    }

    private void updateProperties() {
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

    public void addLink(String sdId, ChannelUID uid) {
        synchronized(linkList) {
            if(!linkList.stream().filter(i -> uid.equals(i.getUID())).findAny().isPresent()) {
        linkList.add(channelListItem(sdId,uid));
        logger.debug("Channel Added to Polling List: {}",uid.toString());
            } else {
                logger.debug("Channel Already In Polling List: {}",uid.toString());
            }
        }

    }

    public void removeLink(String sdId, ChannelUID uid) {
        synchronized(linkList) {
            if(linkList.stream().filter(i -> uid.equals(i.getUID())).findAny().isPresent()) {
                linkList.removeIf(e -> e.getUID().equals(uid));
            //linkList.stream().filter(i -> uid.equals(i.getUID())).forEach(
            //    linkList::remove);
                logger.debug("Channel Removed from Polling List: {}",uid.toString());
            } else {
                logger.debug("Channel Not Present In Polling List: {}",uid.toString());
            }
        }        

    }

    private ChannelListItem channelListItem(String sdId, ChannelUID uid) {
        String channelName = uid.getIdWithoutGroup();
        String channelId = uid.getId().toString();
        int channelNo = (Integer.parseInt(channelId.substring(0,1))-1);
        Features feature = getFeature(sdId,channelNo, channelName);
        String featureId = feature.getFeatureId();
        ChannelListItem channelListItem = new ChannelListItem(sdId,uid,featureId);
        return channelListItem;
    }

    private void restartPolling() {
        long pollingInterval = Long.parseLong(this.thing.getConfiguration().get("pollingInterval").toString());
        logger.warn("Polling restarting");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                polling();
            }
        };
        refreshTask = scheduler.scheduleWithFixedDelay(runnable,10, pollingInterval, TimeUnit.MILLISECONDS);
    }

    private List<List<ChannelListItem>> getChannelList() {
        int partitionSize = Integer.valueOf(this.thing.getConfiguration().get("pollingGroupSize").toString());
        List<List<ChannelListItem>> partitions = new ArrayList<List<ChannelListItem>>();
        synchronized(linkList) {
            for (int i = 0; i < linkList.size(); i += partitionSize) {
                partitions.add(linkList.subList(i, Math.min(i + partitionSize, linkList.size())));
            }
        }
        return partitions;
    }

    private Map<String, Long> getLocks() {
        Map<String, Long> locksT = new HashMap<String,Long>();
            if (!locks.isEmpty()) {
                locksT.putAll(locks);
            }
        return locksT;
    }

    private void removeLocks(Map<String, Long> locksT) {
        for (Map.Entry<String, Long> map : locksT.entrySet()) {
            String key = map.getKey();
            Long value = map.getValue();
            locks.remove(key, value);
            logger.debug("lock removed: {} : {}", key, value);
        }
    }

    private void polling() {
        synchronized(linkList) {
        logger.debug("Polling List Size: {}",linkList.size());
        if (linkList.size() == 0) {
            logger.warn("Channel List For Updating Is Empty, rescheduling");
            if (refreshTask != null) {
                refreshTask.cancel(true);
            }
            restartPolling();
        } else {
            logger.debug("Initiate Polling");
            pollingCheck = scheduler.schedule(this::restartPolling,30, TimeUnit.SECONDS);
            List<List<ChannelListItem>> partitions = getChannelList();
            Map<String, Long> locksT = getLocks();                
            int l = 0;
            for (l = 0; l < partitions.size(); l++) {
                List<ChannelListItem> partition = partitions.get(l);
                logger.debug("Start Partition: {}", (l+1));
                runUpdate(partition,locksT,(l+1));
            }
            logger.debug("Finished Channel Update");
            removeLocks(locksT);
            logger.debug("Removed Redundant Locks");                
            if (pollingCheck != null) {
                pollingCheck.cancel(true);
            }
        }
    }
    }

    public void runUpdate(List<ChannelListItem> partition, Map<String, Long> locks, int number) {
        //Runnable runnable = new Runnable() {
            //@Override
            //public void run() { 
                String body = createJson(partition);             
                InputStream data = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
                Http http = new Http();
                String response = http.httpClient("features", data, "application/json", "");
                if (errorHandler(response)) {
                    HashMap<String, Long> featureStatuses = gson.fromJson(response,new TypeToken<HashMap<String, Long>>() {}.getType());
                    for (Map.Entry<String, Long> myMap : featureStatuses.entrySet()) {
                        String key = myMap.getKey().toString();
                        ChannelListItem channel = partition.stream().filter(i -> key.equals(i.getFeatureId())).findFirst().orElse(partition.get(0));
                        channelUpdate(myMap.getValue(),channel.getUID(),myMap.getKey().toString(),channel.getSdId());
                    }
                logger.debug("Finished Partition: {}", number);
                }
            //}
        //};
        //runnable.run();
    }

    public Features getFeature(String sdId, int featureSetNo,String channelId) {
        String t = "-" + sdId + "-";
        Devices device = devices.stream().filter(i -> i.getDeviceId().contains(t)).findFirst().orElse(devices.get(0));
        String featureSetId = device.getFeatureSets().get(featureSetNo).getFeatureSetId();
        FeatureSets featureSet = featureSets().stream().filter(i -> featureSetId.equals(i.getFeatureSetId())).findFirst()
                .orElse(featureSets().get(0)); 
        return featureSet.getFeatures().stream().filter(i -> channelId.equals(i.getType())).findFirst()
                .orElse(featureSet.getFeatures().get(0));
    }    
    
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }
    
    public boolean addLocked( String featureId, Long time ) {
        locks.put(featureId,time);
        return true;
    }
    
    private List<FeatureSets> featureSets() {
        List<FeatureSets> featureSets = new ArrayList<FeatureSets>();
        for (int b = 0; b < devices.size(); b++) {   
            featureSets.addAll(devices.get(b).getFeatureSets());
        }
        return featureSets;
    }

    public List<Devices> getDevices() {
        Http http = new Http();
        return http.getDevices();
    }

    private String createJson(List<ChannelListItem> partition) {
        String jsonBody = "";
        String jsonEnd = "";
        String jsonMain = "";
            jsonBody = "{\"features\": [";
            jsonEnd = "";
            //logger.debug("Fixer Partition Size: {}",partition.size());
            for (int m = 0; m < partition.size(); m++) {
                if (m < (partition.size() - 1)) {
                    jsonEnd = ",";
                } else {
                    jsonEnd = "]}";
                }
                //logger.debug("Fixer Partition: {}",partition.get(m).getFeatureId().toString());
                jsonMain = "{\"featureId\": \"" + partition.get(m).getFeatureId().toString() + "\"}";
                jsonBody = jsonBody + jsonMain + jsonEnd;
                //logger.debug("Fixer JSON: {}",jsonBody);
            }
        return jsonBody;
    }

    private Boolean errorHandler(String response) {
        if(response.contains("{\"message\":\"Structure not found\"}")) {
            logger.warn("Api Timed Out Returning Data, decrease your group size.");
            return false;
        }
        else if(response.contains("{\"message\":\"Request rate reached limit\"}")) {
            logger.warn("Your polling too fast, increase your interval");
            return false;
        }
        else if(response.contains("{\"message\":\"FeatureRead Failed\"}")) {
            logger.warn("Lightwaves Servers currently in error state, try and reduce your polling to see if helps");
            return false;
        }
        else {
            //logger.debug("JSON Is Normal");
            //logger.debug("Response is: {}",response);
            return true;
        }
    }

    private void channelUpdate(long value,ChannelUID channel,String featureId,String sdId) {
        String channelName = channel.getIdWithoutGroup(); 
        double electricityCost = Double.parseDouble(this.getThing().getConfiguration().get("electricityCost").toString()) / 100;
        List<Thing> things = new ArrayList<Thing>();
        things = this.getThing().getThings();   
        String helper;
        Thing thing;
        Channel channelCost;
                    if(!locks.containsKey(featureId)) {               
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
                                    updateState(channel, OnOffType.ON);
                                } else {
                                    updateState(channel, OnOffType.OFF);
                                }
                                break;
                            case "power": 
                                updateState(channel, new DecimalType(value));
                                helper = channel.getGroupId() + "#powerCost";
                                thing = things.stream().filter(i -> i.getConfiguration().get("sdId").equals(sdId)).findFirst().orElse(things.get(0));
                                channelCost = thing.getChannels().stream().filter(i -> i.getUID().getId().contains(helper)).findFirst().orElse(thing.getChannels().get(0));
                                updateState(channelCost.getUID(), new DecimalType(value / 1000.0 * electricityCost));
                                break;
                            case "rssi": 
                            case "timeZone":
                            case "day":
                            case "month":
                            case "year":
                                updateState(channel, new DecimalType(value));
                                break;
                            case "energy":
                                updateState(channel, new DecimalType((value / 1000.0)));
                                helper = channel.getGroupId() + "#energyCost";
                                thing = things.stream().filter(i -> i.getConfiguration().get("sdId").equals(sdId)).findFirst().orElse(things.get(0));
                                channelCost = thing.getChannels().stream().filter(i -> i.getUID().getId().contains(helper)).findFirst().orElse(thing.getChannels().get(0));
                                updateState(channelCost.getUID(), new DecimalType((value / 1000.0 * electricityCost)));
                                break;
                            case "temperature":
                            case "targetTemperature":
                            case "voltage":
                                updateState(channel, new DecimalType((value / 10.0)));
                                break;
                            case "dimLevel":
                            case "valveLevel":
                                updateState(channel, new PercentType((int) value));
                                break;
                                case "batteryLevel":
                                updateState(channel, new DecimalType(value));
                                break;
                            case "rgbColor":
                                Color color = new Color((int) value);
                                int red = (color.getRed());
                                int green = (color.getGreen());
                                int blue = (color.getBlue());
                                float[] hsb = Color.RGBtoHSB(red, green, blue, null);
                                int hue = (int) Math.round(hsb[0] * 360);
                                int saturation = (int) Math.round(hsb[1] * 100);
                                int brightness = (int) Math.round(hsb[2] * 100);  
                                String hsb1 = hue + "," + saturation + "," + brightness;
                                updateState(channel, new HSBType(hsb1));
                                break;
                            case "periodOfBroadcast":
                            case "monthArray":
                            case "weekdayArray":
                                updateState(channel, new StringType(String.valueOf(value)));
                                break;
                            case "date":
                                String monthPad = "";
                                String dayPad = "";
                                String hex = Integer.toHexString((int) value);
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
                                updateState(channel,new DateTimeType(dateValue));
                                break;
                            case "currentTime":
                                DateTimeType time = new DateTimeType(new DateTime(value * 1000).toString());
                                updateState(channel, time);
                                break;
                            case "duskTime":
                            case "dawnTime":
                            case "time":

                                String hoursPad = "";
                                String minsPad = "";
                                String secsPad = "";
                                int minutes = ((((int) value) / 60)%60);
                                int hours = ((int) value / 3600);
                                int seconds = (((int) value) % 60);
                                if (hours < 10) {
                                    hoursPad = "0";
                                }
                                if (minutes < 10) {
                                    minsPad = "0";
                                }
                                if (seconds < 10) {
                                    secsPad = "0";
                                }
                                LocalDate now = LocalDate.now();

                                String timeValue = now + "T" + hoursPad + hours + ":" + minsPad + minutes + ":" + secsPad + seconds ;
                                updateState(channel, new DateTimeType(timeValue));
                                break;
                            case "weekday":
                                if (value != 0) {
                                    updateState(channel, new StringType(DayOfWeek.of((int) value).toString()));
                                    break;
                                } else {
                                break;
                                }
                            case "locationLongitude":
                            case "locationLatitude":
                                updateState(channel, new StringType(new DecimalType(value / 1000000.0).toString()));
                                break;
                            }
                        }
                    } else {
                        logger.debug("channel locked"); 
                    }   
        }

}
