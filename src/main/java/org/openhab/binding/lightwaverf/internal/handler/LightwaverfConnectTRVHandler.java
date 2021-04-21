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
import org.openhab.binding.lightwaverf.internal.config.LightwaverfConnectTRVConfig;
import org.openhab.binding.lightwaverf.internal.dto.LightwaverfConnectResponse;
import org.openhab.binding.lightwaverf.internal.listeners.LightwaverfConnectMessageListener;
import org.openhab.binding.lightwaverf.internal.utilities.LWCommand;
import org.openhab.binding.lightwaverf.internal.utilities.LWType;
import org.openhab.binding.lightwaverf.internal.utilities.MessageConvertor;
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
public class LightwaverfConnectTRVHandler extends BaseThingHandler implements LightwaverfConnectMessageListener {

    private @Nullable LightwaverfConnectTRVConfig config;
    private @Nullable LightwaverfConnectAccountHandler account;
    private static Logger logger = LoggerFactory.getLogger(LightwaverfConnectTRVHandler.class);
    private final MessageConvertor converter = new MessageConvertor();

    public LightwaverfConnectTRVHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        Bridge bridge = getBridge();
        account = (LightwaverfConnectAccountHandler) bridge.getHandler();
        config = this.getConfigAs(LightwaverfConnectTRVConfig.class);
        account.registerRoom(config.roomId, this);
        account.registerSerial(config.serialNo, this);
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        account.unregisterRoom(this);
        account.unregisterSerial(this);
        config = null;
        account = null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            return;
        } else {
            switch (channelUID.getId()) {
                case "targetTemperature":
                    String commandToSend = ",!" + "R" + config.roomId + "F*t" + "P" + command.toString();
                    LWCommand message = converter.convertToLightwaveRfMessage(config.serialNo, config.roomId,
                            LWType.HEATING_SET_TEMP, command);
                    if (message != null) {
                        logger.info("Sending Command:{} from device:{}", commandToSend, config.serialNo);
                        account.sendUDP(message);
                    }
                    break;
            }
        }
    }

    @Override
    public void roomDeviceMessageReceived(LightwaverfConnectResponse message) {
        // Nothing to do here as TRV
    }

    @Override
    public void roomMessageReceived(LightwaverfConnectResponse message) {
        logger.info("Room Message Received: {}", message.getCmd());
        logger.info("Room Message Received: {}", message.getRoom());
        logger.info("Room Message Received: {}", message.getFn());
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
