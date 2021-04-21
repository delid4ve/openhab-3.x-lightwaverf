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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lightwaverf.internal.commands.CommandOk;
import org.openhab.binding.lightwaverf.internal.commands.HeatInfoRequest;
import org.openhab.binding.lightwaverf.internal.commands.VersionMessage;
import org.openhab.binding.lightwaverf.internal.config.LightwaverfConnectDimmerConfig;
import org.openhab.binding.lightwaverf.internal.dto.LightwaverfConnectResponse;
import org.openhab.binding.lightwaverf.internal.listeners.LightwaverfConnectMessageListener;
import org.openhab.binding.lightwaverf.internal.utilities.LWCommand;
import org.openhab.binding.lightwaverf.internal.utilities.LWType;
import org.openhab.binding.lightwaverf.internal.utilities.MessageConvertor;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author David Murton - Initial contribution
 * 
 */
@NonNullByDefault
public class LightwaverfConnectDimmerHandler extends BaseThingHandler implements LightwaverfConnectMessageListener {

    private @Nullable LightwaverfConnectDimmerConfig config;
    private @Nullable LightwaverfConnectAccountHandler account;
    private static Logger logger = LoggerFactory.getLogger(LightwaverfConnectDimmerHandler.class);
    private final MessageConvertor converter = new MessageConvertor();

    public LightwaverfConnectDimmerHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        Bridge bridge = getBridge();
        account = (LightwaverfConnectAccountHandler) bridge.getHandler();
        config = this.getConfigAs(LightwaverfConnectDimmerConfig.class);
        logger.info("Device configured with RoomID:{} and DeviceID:{}", config.deviceid, config.roomid);
        account.registerRoom(config.roomid, this);
        logger.info("Registering Room Listener for ID:{}", config.roomid);
        account.registerDevice(config.deviceid, this);
        logger.info("Registering Device Listener for ID:{}", config.deviceid);
        if (bridge.getStatus() == ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void dispose() {
        account.unregisterRoom(this);
        account.unregisterDevice(this);
        config = null;
        account = null;
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (config.deviceid.isEmpty() || config.roomid.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "One or more IDs not set");
            return;
        }
        if (bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            account.unregisterDevice(this);
            account.unregisterRoom(this);
            return;
        }
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            thingUpdated(this.thing);
            return;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        LWCommand message;
        if (command instanceof RefreshType) {
            return;
        } else {
            switch (channelUID.getId()) {
                case "dimLevel":
                    logger.info("openhab command:{}", command.toString());
                    message = converter.convertToLightwaveRfMessage(config.roomid, config.deviceid, LWType.DIMMER,
                            command);
                    logger.info("Sending Command:{} from device:{}", message.getCommandString(), config.deviceid);
                    account.sendUDP(message);
                    // if (command.toString().equals("OFF") || command.toString().equals("0")) {
                    // commandToSend = ",!" + "R" + config.roomid + "D" + config.deviceid + "F0";
                    // } else {
                    // dimLevel = (int) Math.round(((32 / 100) * (Integer.parseInt(command.toString()))));
                    // }
                    // commandToSend = ",!" + "R" + config.roomid + "D" + config.deviceid + "FdP" + dimLevel;
                    break;
            }
        }
    }

    private void updateState(LightwaverfConnectResponse message) {
        ChannelUID channelUID;
        String function = message.getFn();
        switch (function) {
            case "dim":
                Integer dimLevel = ((100 / 32) * message.getParam());
                logger.info("Dimmer State to process {}", dimLevel);
                channelUID = this.thing.getChannel("dimLevel").getUID();
                updateState(channelUID, new PercentType(dimLevel));
                break;
        }
    }

    @Override
    public void roomDeviceMessageReceived(LightwaverfConnectResponse message) {
        updateState(message);
    }

    @Override
    public void roomMessageReceived(LightwaverfConnectResponse message) {
    }

    @Override
    public void serialMessageReceived(LightwaverfConnectResponse message) {
        logger.info("Serial Message Received: {}", message.getCmd());
    }

    @Override
    public void okMessageReceived(CommandOk message) {
        // TODO Auto-generated method stub
    }

    @Override
    public void versionMessageReceived(VersionMessage message) {
        // TODO Auto-generated method stub
    }

    @Override
    public void heatInfoMessageReceived(HeatInfoRequest command) {
        // TODO Auto-generated method stub
    }
}
