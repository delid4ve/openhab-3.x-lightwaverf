/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lightwaverf.internal.config.LightwaverfSmartDeviceConfig;
import org.openhab.binding.lightwaverf.internal.dto.LightwaverfSmartItem;
import org.openhab.binding.lightwaverf.internal.dto.LightwaverfSmartPayload;
import org.openhab.binding.lightwaverf.internal.dto.LightwaverfSmartRequest;
import org.openhab.binding.lightwaverf.internal.dto.api.LightwaverfSmartDevices;
import org.openhab.binding.lightwaverf.internal.dto.api.LightwaverfSmartFeatureSets;
import org.openhab.binding.lightwaverf.internal.dto.api.LightwaverfSmartFeatures;
import org.openhab.binding.lightwaverf.internal.listeners.LightwaverfSmartDeviceListener;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LightwaverfSmartDeviceHandler} class handles gen2 lightwave devices
 * 
 *
 * @author David Murton - Initial contribution
 */

@NonNullByDefault
public class LightwaverfSmartDeviceHandler extends BaseThingHandler implements LightwaverfSmartDeviceListener {

    private final Logger logger = LoggerFactory.getLogger(LightwaverfSmartDeviceHandler.class);
    private @Nullable LightwaverfSmartAccountHandler account;
    private Map<String, String> channels = new HashMap<String, String>();
    LightwaverfSmartDeviceConfig config = new LightwaverfSmartDeviceConfig();

    public LightwaverfSmartDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        config = this.getConfigAs(LightwaverfSmartDeviceConfig.class);
        if (!config.deviceid.isEmpty()) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge Not set");
            } else {
                LightwaverfSmartAccountHandler account = (LightwaverfSmartAccountHandler) bridge.getHandler();
                if (account != null) {
                    this.account = account;
                    // this.electricityCost = account.getElectricityCost();

                    // this.deviceid = config.deviceid;
                    LightwaverfSmartDevices device = account.getDevice(config.deviceid);
                    if (device == null) {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                                "Please check the deviceid as the data couldnt be retrieved");
                        return;
                    } else {
                        // Create a channel map and add the features to the featuremap for received messages
                        List<LightwaverfSmartFeatureSets> featureSets = device.getFeatureSets();
                        for (int i = 0; i < featureSets.size(); i++) {
                            List<LightwaverfSmartFeatures> features = featureSets.get(i).getFeatures();
                            for (int j = 0; j < features.size(); j++) {
                                String channel = (i + 1) + "#" + features.get(j).getType();
                                String featureid = features.get(j).getFeatureId();
                                logger.trace("Adding Channel {} with featureid {} to map for device {}", channel,
                                        featureid, config.deviceid);
                                account.addFeature(featureid, config.deviceid);
                                channels.putIfAbsent(channel, featureid);
                            }
                        }
                        setProperties(device);
                        account.addDeviceListener(config.deviceid, this);
                    }
                    if (bridge.getStatus() == ThingStatus.ONLINE) {
                        updateStatus(ThingStatus.ONLINE);
                        updateChannels();
                    } else {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                    }
                }
            }
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("bridgeStatusChanged {} for thing {}", bridgeStatusInfo, getThing().getUID());
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
            updateChannels();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    @Override
    public void dispose() {
        logger.debug("LightwaveRF - Running dispose()");
        LightwaverfSmartAccountHandler account = this.account;
        if (account != null) {
            account.removeDeviceListener(config.deviceid);
            account = null;
        }
    }

    private void updateChannels() {
        for (int j = 0; j < this.getThing().getChannels().size(); j++) {
            handleCommand(this.getThing().getChannels().get(j).getUID(), RefreshType.REFRESH);
        }
    }

    private void setProperties(LightwaverfSmartDevices device) {
        Map<String, String> properties = editProperties();
        Integer channelSize = device.getFeatureSets().size();
        properties.clear();
        properties.put("deviceid", device.getDeviceId());
        properties.put("Name", device.getName());
        properties.put("Device", device.getDevice());
        properties.put("Type", device.getType());
        properties.put("Description", device.getDesc());
        properties.put("Product", device.getProduct());
        properties.put("Product Code", device.getProductCode());
        properties.put("Category", device.getCat());
        properties.put("Generation", device.getGen().toString());
        properties.put("Channels", channelSize.toString());
        updateProperties(properties);
    }

    private long getState(String channel, String command) {
        switch (channel) {
            case "energyReset":
            case "voltageReset":
                return 0;
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
            case "threeWayRelay":
                if (command == "ON") {
                    return 1;
                } else {
                    return 0;
                }
            case "rgbColor":
                if (command.contains(",")) {
                    HSBType hsb = new HSBType(command);
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
                    return d;
                } else {
                    logger.warn("LightwaveRF - Brightness Is Not Supported For the RGB Colour Channel");
                    return -1;
                }
            case "timeZone":
            case "locationLongitude":
            case "locationLatitude":
            case "dimLevel":
            case "valveLevel":
                return (long) (Float.parseFloat(command));
            case "temperature":
            case "targetTemperature":
                return (long) (Float.parseFloat(command) * 10);
            default:
                return -1;
        }
    }

    private void resetCommand(ChannelUID channelUID, Command command) {
        if (command.toString().toLowerCase().equals("on")) {
            if (channelUID.getIdWithoutGroup().equals("energyReset")) {
                String featureid = channels.get(channelUID.getGroupId() + "#" + "energy");
                if (featureid != null) {
                    setStatus("feature", "write", "request", 0L, featureid);
                } else {
                    logger.error("Command {} for device {} returned a null featureid and couldnt be sent",
                            command.toString(), config.deviceid);
                }
            } else if (channelUID.getIdWithoutGroup().equals("voltageReset")) {
                String featureid = channels.get(channelUID.getGroupId() + "#" + "voltage");
                if (featureid != null) {
                    setStatus("feature", "write", "request", 0L, featureid);
                } else {
                    logger.error("Command {} for device {} returned a null featureid and couldnt be sent",
                            command.toString(), config.deviceid);
                }
            }
            updateState(channelUID.getId().toString(), OnOffType.OFF);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            String featureid = channels.get(channelUID.getId());
            if (featureid != null) {
                logger.trace("Device {} is requesting an update for {}", config.deviceid, channelUID.getId());
                setStatus("feature", "read", "request", null, featureid);
            }
            return;
        } else {
            String channelid = channelUID.getIdWithoutGroup();
            if (channelid.equals("energyReset") || channelid.equals("voltageReset")) {
                resetCommand(channelUID, command);
            } else {
                String featureid = channels.get(channelUID.getId());
                if (featureid != null) {
                    long value = getState(channelUID.getIdWithoutGroup(), command.toString());
                    setStatus("feature", "write", "request", value, featureid);
                } else {
                    logger.error("Command {} for device {} returned a null featureid and couldnt be sent",
                            command.toString(), config.deviceid);
                }
            }
        }
    }

    private void setStatus(String _class, String operation, String direction, @Nullable Long value, String id) {
        LightwaverfSmartItem item = new LightwaverfSmartItem();
        LightwaverfSmartPayload payload = new LightwaverfSmartPayload();
        // Random random = new Random();
        payload.setFeatureId(id);
        if (value != null) {
            payload.setValue(value);
        }
        item.setPayload(payload);
        LightwaverfSmartRequest command = new LightwaverfSmartRequest(_class, operation, direction, item);
        if (payload.getFeatureId() == null) {
            logger.error("Payload was emtpy from device {}, not sending message {} - {} - {}", config.deviceid, id,
                    _class, operation);
        } else {
            LightwaverfSmartAccountHandler account = this.account;
            if (account != null && account.isConnected()) {
                account.sendDeviceCommand(command);
            } else {
                logger.error("Could not set status for device {} as the account is disconnected", config.deviceid);
            }
        }
    }

    @Override
    public void updateChannel(String channelId, State state) {
        updateState(channelId, state);
        logger.debug("Device {} Updated Channel {}", config.deviceid, channelId);
        if (channelId.contains("power") || channelId.contains("energy")) {
            LightwaverfSmartAccountHandler account = this.account;
            if (account != null) {
                double electricityCost = account.getElectricityCost();
                double value = Double.valueOf(state.toString());
                if (channelId.contains("power")) {
                    updateState(channelId + "Cost", new DecimalType((value / 100000) * electricityCost));
                }
                if (channelId.contains("energy")) {
                    updateState(channelId + "Cost", new DecimalType(value / 100 * electricityCost));
                }
            }
        }
    }
}
