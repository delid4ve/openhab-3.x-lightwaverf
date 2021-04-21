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
import org.openhab.binding.lightwaverf.internal.config.LightwaverfConnectRelayConfig;
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
public class LightwaverfConnectRelayHandler extends BaseThingHandler implements LightwaverfConnectMessageListener {

    private @Nullable LightwaverfConnectRelayConfig config;
    private @Nullable LightwaverfConnectAccountHandler account;
    private static Logger logger = LoggerFactory.getLogger(LightwaverfConnectRelayHandler.class);
    private final MessageConvertor converter = new MessageConvertor();

    public LightwaverfConnectRelayHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        Bridge bridge = getBridge();
        account = (LightwaverfConnectAccountHandler) bridge.getHandler();
        config = this.getConfigAs(LightwaverfConnectRelayConfig.class);
        account.registerRoom(config.roomId, this);
        account.registerDevice(config.deviceId, this);
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        account.unregisterRoom(this);
        account.unregisterDevice(this);
        config = null;
        account = null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String commandToSend = "";
        String translation = "";
        if (command instanceof RefreshType) {
            return;
        } else {
            switch (channelUID.getId()) {
                case "relayPosition":
                    if (command.toString().equalsIgnoreCase("0")) {
                        translation = "F^";
                    } else if (command.toString().equalsIgnoreCase("-1")) {
                        translation = "F)";
                    } else if (command.toString().equalsIgnoreCase("1")) {
                        translation = "F(";
                    }
                    commandToSend = ",!" + "R" + config.roomId + "D" + config.deviceId + "F" + translation;
                    break;
                case "switch":
                    translation = command.toString().equalsIgnoreCase("on") ? "1" : "0";
                    commandToSend = ",!" + "R" + config.roomId + "D" + config.deviceId + "F" + translation;
                    break;
            }
            LWCommand message = converter.convertToLightwaveRfMessage(config.roomId, config.deviceId, LWType.RELAY,
                    command);
            if (message != null) {
                logger.info("Sending Command:{} from device:{}", commandToSend, config.deviceId);
                account.sendUDP(message);
            }
        }
    }

    private void updateState(LightwaverfConnectResponse message) {
        ChannelUID channelUID;
        String function = message.getFn();
        Integer command = 0;
        switch (function) {
            case "on":
            case "off":
                channelUID = this.thing.getChannel("switch").getUID();
                updateState(channelUID, (function.equals("on") ? OnOffType.ON : OnOffType.OFF));
                break;
            case "open":
            case "close":
            case "stop":
                channelUID = this.thing.getChannel("relayPostion").getUID();
                if (function.equals("open")) {
                    command = 1;
                } else if (function.equals("close")) {
                    command = -1;
                } else if (function.equals("stop")) {
                    command = 0;
                }
                updateState(channelUID, new DecimalType(command));
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
