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
import java.util.List;
import java.util.Map;
import com.google.gson.JsonObject;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.lightwaverf.internal.Http;
import org.openhab.binding.lightwaverf.internal.api.discovery.Devices;
import org.openhab.binding.lightwaverf.internal.api.discovery.Features;
import org.eclipse.smarthome.core.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which
 * are used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */

@NonNullByDefault
public class DeviceHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(DeviceHandler.class);
    private @Nullable LWAccountHandler account;
    private String sdId = this.thing.getConfiguration().get("sdId").toString();
    private @Nullable Devices device;

    public DeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing {} handler.", getThing().getThingTypeUID());
        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge Not set");
        }
        initializeBridge(bridge.getHandler(), bridge.getStatus());
            device = new Devices();
            device = device(sdId);
        properties();
        updateStatus(ThingStatus.ONLINE);
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
        account = null;
        device = null;
    }

    @Override
    public void channelLinked(final ChannelUID channelUID) {
        if(!channelUID.getIdWithoutGroup().contains("powerCost") && !channelUID.getIdWithoutGroup().contains("energyCost") &&
        !channelUID.getIdWithoutGroup().contains("energyReset") && !channelUID.getIdWithoutGroup().contains("voltageReset")) {
            account.addLink(sdId,channelUID);
        }
        super.channelUnlinked(channelUID);
    }

    @Override
    public void channelUnlinked(final ChannelUID channelUID) {
        if(!channelUID.getIdWithoutGroup().contains("powerCost") && !channelUID.getIdWithoutGroup().contains("energyCost") &&
        !channelUID.getIdWithoutGroup().contains("energyReset") && !channelUID.getIdWithoutGroup().contains("voltageReset")) {
            account.removeLink(sdId,channelUID);
        }
        super.channelLinked(channelUID);
    }


    private void initializeBridge(@Nullable ThingHandler thingHandler, ThingStatus bridgeStatus) {
        logger.debug("initializeBridge {} for thing {}", bridgeStatus, getThing().getUID());

        if (thingHandler != null && bridgeStatus != null) {
            account = (LWAccountHandler) thingHandler;
            if (bridgeStatus != ThingStatus.ONLINE) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    private Devices device(String sdId) {
        Devices device = null;
        List<Devices> devices = account.getDevices();
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getDeviceId().contains("-" + sdId + "-")) {
                device = devices.get(i);
            }
        }
        return device;
    }

    private void properties() {
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
        dProperties.put("sdId", sdId);
        dProperties.put("Name", device.getName());
        dProperties.put("Device", device.getDevice());
        dProperties.put("Type", device.getType());
        dProperties.put("Description", device.getDesc());
        dProperties.put("Product", device.getProduct());
        dProperties.put("Product Code", device.getProductCode());
        dProperties.put("Category", device.getCat());
        dProperties.put("Generation", device.getGen().toString());
        dProperties.put("Channels", new Integer(device.getFeatureSets().size()).toString());
        updateProperties(dProperties);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String channelName = channelUID.getIdWithoutGroup();
        String value = "";
        String group = channelUID.getGroupId();
        int i = ((int) Double.parseDouble(group) - 1);
        if (command instanceof RefreshType) {
            return;
        } else if (account == null) {
            logger.warn("No connection to Lightwave available, ignoring command");
            return;
        } else {
            logger.debug("handleCommand(list): channel = {} group = {}", channelName, i);
            switch (channelName) {
                case "energyReset":
                case "voltageReset":
                    value = "0";
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
                    if (command.toString() == "ON") {
                        value = "1";
                    } else {
                        value = "0";
                    }
                    break;
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
                        value = Long.toString(d);
                        updateState(channelUID, h);
                        break;
                    } else {
                        logger.warn("Brightness Is Not Supported For the RGB Colour Channel");
                        break;
                    }
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

            if (channelUID.getIdWithoutGroup() == "energyReset") {
                channelName = "energy";
            } else if (channelUID.getIdWithoutGroup() == "voltageReset") {
                channelName = "voltage";
            } else {
                channelName = channelUID.getIdWithoutGroup();
            }
            Features feature = account.getFeature(sdId, i, channelName);
            String featureId = feature.getFeatureId();
            long now = System.currentTimeMillis();
            account.addLocked(featureId, now);
            logger.debug("lock added: {} : {}", featureId, now);
            final String temp = value;
            try {
                setStatus(featureId, temp);
            } catch (Exception e) {
            }
        }
    }
    
    public void setStatus(String featureId,String value) throws Exception {
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("value", value);
        InputStream data = new ByteArrayInputStream(jsonReq.toString().getBytes(StandardCharsets.UTF_8));
        Http http = new Http();
        http.httpClient("feature", data, "application/json",featureId);     
    } 

}
