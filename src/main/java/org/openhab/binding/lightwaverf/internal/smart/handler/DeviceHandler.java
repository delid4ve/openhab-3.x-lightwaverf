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
package org.openhab.binding.lightwaverf.internal.smart.handler;

import java.awt.Color;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.*;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.lightwaverf.internal.smart.config.DeviceConfig;
import org.openhab.binding.lightwaverf.internal.smart.dto.api.Devices;
import org.openhab.binding.lightwaverf.internal.smart.dto.api.Features;
import org.openhab.binding.lightwaverf.internal.smart.dto.websocket.InitialStatus;
import org.openhab.binding.lightwaverf.internal.smart.listeners.DeviceStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which
 * are used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */

@NonNullByDefault
public class DeviceHandler extends BaseThingHandler implements DeviceStateListener {

    private final Logger logger = LoggerFactory.getLogger(DeviceHandler.class);
    private @Nullable Devices device = new Devices();
    private @Nullable DeviceConfig config;
    private @Nullable LWAccountHandler account;
    private double electricityCost = 0;

    public DeviceHandler(Thing thing) {
        super(thing);
    }

    public int getRandom() {
        Random random = new Random();
        return random.ints(0, 500000).findFirst().getAsInt();
    }

    @Override
    public void initialize() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge Not set");
        } else {
            account = (LWAccountHandler) bridge.getHandler();
            config = this.getConfigAs(DeviceConfig.class);
            device = account.getDevice(config.sdId);
            electricityCost = account.getElecCost() / 100;
            setProperties();
            setStatus("feature", "read", "request", null, null);
            account.registerStateListener(config.sdId, this);
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void thingUpdated(Thing thing) {
        dispose();
        this.thing = thing;
        initialize();
    }

    @Override
    public void dispose() {
        updateStatus(ThingStatus.OFFLINE);
        account.unregisterStateListener(config.sdId);
        logger.debug("LightwaveRF - Running dispose()");
        account = null;
        config = null;
    }

    private void setProperties() {
        String featureString = "";
        Map<String, String> dProperties = editProperties();
        dProperties.clear();
        for (int i = 0; i < device.getFeatureSets().size(); i++) {
            String name = device.getFeatureSets().get(i).getName();
            String list = "Channel: " + (i + 1) + ", Name: " + name + "\r\n";
            dProperties.put("Channel" + (i + 1), list);
        }
        for (int i = 0; i < device.getFeatureSets().size(); i++) {
            featureString = featureString + "Channel " + (i + 1) + ": ";
            for (int j = 0; j < device.getFeatureSets().get(i).getFeatures().size(); j++) {
                featureString = featureString + device.getFeatureSets().get(i).getFeatures().get(j).getType() + ",";
            }
        }
        dProperties.put("Available Channels", featureString.substring(0, featureString.length() - 1));
        dProperties.put("Device ID", device.getDeviceId());
        dProperties.put("Name", device.getName());
        dProperties.put("Device", device.getDevice());
        dProperties.put("Type", device.getType());
        dProperties.put("Description", device.getDesc());
        dProperties.put("Product", device.getProduct());
        dProperties.put("Product Code", device.getProductCode());
        dProperties.put("Category", device.getCat());
        dProperties.put("Generation", device.getGen().toString());
        dProperties.put("Channels", String.valueOf(device.getFeatureSets().size()));
        updateProperties(dProperties);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        Features features = new Features();
        String initialChannelName = channelUID.getIdWithoutGroup();
        long value = 0;
        String group = (channelUID.getGroupId() == null) ? "0" : channelUID.getGroupId();
        int i = (group == null) ? 0 : Integer.valueOf(group) - 1;
        String featureId = "";
        if (command instanceof RefreshType) {
            return;
        } else if (account == null) {
            logger.warn("LightwaveRF - No connection to Lightwave available, ignoring command");
            return;
        } else {
            logger.debug("LightwaveRF - handleCommand(list): channel = {} group = {}", initialChannelName, i);
            switch (initialChannelName) {
                case "energyReset":
                case "voltageReset":
                    value = 0;
                    break;
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
                case "valveSetup":
                    if (command.toString() == "ON") {
                        value = 1;
                    } else {
                        value = 0;
                    }
                    break;
                case "threeWayRelay":
                    if (command.toString() == "ON") {
                        value = 1;
                    } else {
                        value = 0;
                    }
                    // if (command.toString() == "OPEN") {
                    // value = "F(";
                    // } else if (command.toString() == "CLOSE") {
                    // value = "F)";
                    // } else if (command.toString() == "STOP") {
                    // value = "F^";
                    // }
                    // ;
                    // break;
                case "rgbColor":
                    if (command.toString().contains(",")) {
                        HSBType hsb = new HSBType(command.toString());
                        int hue = Integer.parseInt(hsb.getHue().toString());
                        int brightness = Integer.parseInt(hsb.getBrightness().toString());
                        if (brightness > 100) {
                            brightness = 100;
                        }
                        if (hue >= 0 && hue < 50) {
                            hue = 0;
                        } else if (hue >= 40 && hue < 75) {
                            hue = 75;
                        } else if (hue >= 75 && hue < 160) {
                            hue = 120;
                        } else if (hue >= 160 && hue < 260) {
                            hue = 220;
                        } else if (hue >= 260 && hue < 325) {
                            hue = 275;
                        } else if (hue >= 325) {
                            hue = 0;
                        }
                        PercentType brightness1 = new PercentType(brightness);
                        DecimalType hue1 = new DecimalType(hue);
                        PercentType saturation = new PercentType(100);
                        HSBType h = new HSBType(hue1, saturation, brightness1);
                        PercentType redp = h.getRed();
                        PercentType greenp = h.getGreen();
                        PercentType bluep = h.getBlue();
                        int redr = (int) (redp.doubleValue() * 255 / 100);
                        int greenr = (int) (greenp.doubleValue() * 255 / 100);
                        int bluer = (int) (bluep.doubleValue() * 255 / 100);
                        long d = (redr * 65536 + greenr * 256 + bluer);
                        value = d;
                        updateState(channelUID, h);
                        break;
                    } else {
                        logger.warn("LightwaveRF - Brightness Is Not Supported For the RGB Colour Channel");
                        break;
                    }
                case "timeZone":
                case "locationLongitude":
                case "locationLatitude":
                case "dimLevel":
                case "valveLevel":
                    value = (long) (Float.parseFloat(command.toString()));
                    break;
                case "temperature":
                case "targetTemperature":
                    value = (long) (Float.parseFloat(command.toString()) * 10);
                    break;
                default:
                    value = -1;
            }
            logger.debug("LightwaveRF - channel: {}", channelUID.getId());
            logger.debug("LightwaveRF - value: {}", value);
            switch (initialChannelName) {
                case "energyReset":
                case "voltageReset":
                    initialChannelName = initialChannelName.substring(0, initialChannelName.length() - 5);
                    break;
                default:
                    initialChannelName = channelUID.getIdWithoutGroup();
            }
            String newChannelName = initialChannelName;
            features = device.getFeatureSets().get(i).getFeatures().stream()
                    .filter(j -> newChannelName.equals(j.getType())).findFirst().orElse(null);
            featureId = features.getFeatureId();
            final Long finalValue = value;
            try {
                if (account.isConnected()) {
                    // account.getConnection().setStatus(featureId, "feature", finalValue, "featureId");
                    setStatus("feature", "write", "request", finalValue, featureId);
                } else {
                    logger.debug("Bridge Connection Offline");
                }
            } catch (Exception e) {
            }
        }
    }

    private void setStatus(String _class, String operation, String direction, @Nullable Long value,
            @Nullable String id) {
        InitialStatus initialStatus = new InitialStatus();
        initialStatus.setVersion(1);
        initialStatus.setClass_(_class);
        initialStatus.setOperation(operation);
        initialStatus.setDirection(direction);
        List<InitialStatus.Item> items = new ArrayList<>();
        if (id == null) { // initial Statuses
            for (int fS = 0; fS < device.getFeatureSets().size(); fS++) {
                for (int f = 0; f < device.getFeatureSets().get(fS).getFeatures().size(); f++) {
                    InitialStatus.Item item = initialStatus.new Item();
                    InitialStatus.Item.Payload payload = item.new Payload();
                    payload.setFeatureId(device.getFeatureSets().get(fS).getFeatures().get(f).getFeatureId());
                    item.setItemId(getRandom() + "");
                    item.setPayload(payload);
                    items.add(item);
                }
            }
            initialStatus.setItems(items);
            if (account.isConnected()) {
                account.getConnection().getInitalStatus(initialStatus);
            }
        } else if (value != null) { // handling a command
            InitialStatus.Item item = initialStatus.new Item();
            InitialStatus.Item.Payload payload = item.new Payload();
            payload.setFeatureId(id);
            payload.setValue(value);
            item.setItemId(getRandom() + "");
            item.setPayload(payload);
            items.add(item);
        }
        initialStatus.setItems(items);
        if (account.isConnected()) {
            account.getConnection().sendCommand(initialStatus);
        }
    }

    @Override
    public void stateUpdate(InitialStatus message) {
        logger.debug("message recieved on device handler with transactionId: {}", message.getTransactionId());
        // UpdateState processState = new UpdateState();
        for (int i = 0; i < message.getItems().size(); i++) {
            long value = message.getItems().get(i).getPayload().getValue();
            int channelNo = (message.getItems().get(i).getPayload().getChannel() + 1);
            String channelType = message.getItems().get(i).getPayload().getType();
            String channelWithGroup = channelNo + "#" + channelType;
            Channel channel = this.thing.getChannel(channelWithGroup);
            ChannelUID channelUID = channel.getUID();
            State state = processState(channelType, value);
            if (message.getDirection().equals("notification") && message.getOperation().equals("event")) {
                updateState(channelUID, state);
                if (channelType == "power" || channelType == "energy") {
                    ChannelUID costChannelUID = this.thing.getChannel((channelWithGroup + "Cost")).getUID();
                    State costState = new DecimalType(value / 1000.0 * electricityCost);
                    updateState(costChannelUID, costState);
                }
            } else if (message.getDirection().equals("response") && message.getOperation().equals("read")) {
                updateState(channelUID, state);
            }
        }
    }

    public State processState(String channelType, long value) {
        State state = OnOffType.ON;
        switch (channelType) {
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
            case "valveSetup":
            case "windowPosition":
                if (value == 1) {
                    state = OnOffType.ON;
                } else {
                    state = OnOffType.OFF;
                }
                break;
            case "threeWayRelay":
            case "periodOfBroadcast":
            case "monthArray":
            case "weekdayArray":
                state = new StringType(String.valueOf(value));
                break;
            case "power":
            case "batteryLevel":
                state = new DecimalType(value);
                break;
            case "rssi":
            case "timeZone":
            case "day":
            case "month":
            case "year":
            case "energy":
                state = new DecimalType((value / 1000.0));

                break;
            case "temperature":
            case "targetTemperature":
            case "voltage":
                state = new DecimalType((value / 10.0));
                break;
            case "dimLevel":
            case "valveLevel":
                state = new PercentType((int) value);
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
                state = new HSBType(hsb1);
                break;

            case "date":
                String monthPad = "";
                String dayPad = "";
                String hex = Integer.toHexString((int) value);
                int year = Integer.parseInt(hex.substring(0, 3), 16);
                int month = Integer.parseInt(hex.substring(3, 4), 16);
                int day = Integer.parseInt(hex.substring(4, 6), 16);
                if (month < 10) {
                    monthPad = "0";
                }
                if (day < 10) {
                    dayPad = "0";
                }
                String dateValue = year + "-" + monthPad + month + "-" + dayPad + day + "T00:00:00.000+0000";
                state = new DateTimeType(dateValue);
                break;
            case "currentTime":
                // ZonedDateTime instant = Instant.ofEpochMilli(value * 1000).atZone(ZoneId.systemDefault());
                state = new DateTimeType();
                break;
            case "duskTime":
            case "dawnTime":
            case "time":
                String hoursPad = "";
                String minsPad = "";
                String secsPad = "";
                int minutes = ((((int) value) / 60) % 60);
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

                String timeValue = now + "T" + hoursPad + hours + ":" + minsPad + minutes + ":" + secsPad + seconds;
                state = new DateTimeType(timeValue);
                break;
            case "weekday":
                if (value != 0) {
                    state = new StringType(DayOfWeek.of((int) value).toString());
                    break;
                } else {
                    break;
                }
            case "locationLongitude":
            case "locationLatitude":
                state = new StringType(new DecimalType(value / 1000000.0).toString());
                break;
        }
        return state;
    }
}
