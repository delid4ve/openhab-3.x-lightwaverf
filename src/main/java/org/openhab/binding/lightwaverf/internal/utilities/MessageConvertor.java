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
package org.openhab.binding.lightwaverf.internal.utilities;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lightwaverf.internal.LightwaverfConnectMessageException;
import org.openhab.binding.lightwaverf.internal.commands.AllOffCommand;
import org.openhab.binding.lightwaverf.internal.commands.CommandOk;
import org.openhab.binding.lightwaverf.internal.commands.DimCommand;
import org.openhab.binding.lightwaverf.internal.commands.MoodCommand;
import org.openhab.binding.lightwaverf.internal.commands.OnOffCommand;
import org.openhab.binding.lightwaverf.internal.commands.RegistrationCommand;
import org.openhab.binding.lightwaverf.internal.commands.RelayCommand;
import org.openhab.binding.lightwaverf.internal.commands.TemperatureCommand;
import org.openhab.binding.lightwaverf.internal.commands.VersionMessage;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts the openhab Type into a LightwaveRfCommand that can be sent to the
 * LightwaveRF Wifi link.
 *
 * /**
 * 
 * @author Neil Renaud - Initial contribution
 * @author David Murton - Since OH 2.x
 * 
 */
@NonNullByDefault
public class MessageConvertor {

    // LightwaveRF messageId
    private final Logger logger = LoggerFactory.getLogger(MessageConvertor.class);
    private int nextMessageId = 200;
    private final Lock lock = new ReentrantLock();

    public @Nullable LWCommand convertToLightwaveRfMessage(String roomId, String deviceId, LWType deviceType,
            Type command) {
        int messageId = getAndIncrementMessageId();

        switch (deviceType) {
            case HEATING_BATTERY:
            case SIGNAL:
            case HEATING_CURRENT_TEMP:
            case HEATING_MODE:
            case VERSION:
                logger.error("{} : is read only and cannot be set ", deviceType);
            case HEATING_SET_TEMP:
                return new TemperatureCommand(messageId, roomId, ((DecimalType) command).doubleValue());
            case DIMMER:
            case SWITCH:
                if (command instanceof OnOffType) {
                    boolean on = (command == OnOffType.ON);
                    return new OnOffCommand(messageId, roomId, deviceId, on);
                } else if (command instanceof PercentType) {
                    int dimmingLevel = ((PercentType) command).intValue();
                    return new DimCommand(messageId, roomId, deviceId, dimmingLevel);
                } else {
                    logger.error("Unsupported Command:{} ", command);
                }
            case RELAY:
                if (command instanceof DecimalType) {
                    int state = ((DecimalType) command).intValue();
                    return new RelayCommand(messageId, roomId, deviceId, state);
                } else {
                    logger.error("Unsupported Command:{} ", command);
                }
            case MOOD:
                if (command instanceof DecimalType) {
                    int state = ((DecimalType) command).intValue();
                    return new MoodCommand(messageId, roomId, state);
                } else {
                    logger.error("Unsupported Command:{} ", command);
                }
            case ALL_OFF:
                if (command instanceof OnOffType) {
                    return new AllOffCommand(messageId, roomId);
                } else {
                    logger.error("Unsupported Command:{} ", command);
                }
            default:
                logger.error("{} : is unexpected ", deviceType);
                return null;
        }
    }

    public LWCommand convertFromLightwaveRfMessage(String message) throws LightwaverfConnectMessageException {
        if (CommandOk.matches(message)) {
            return new CommandOk(message);
        } else if (VersionMessage.matches(message)) {
            return new VersionMessage(message);
        } else if (RegistrationCommand.matches(message)) {
            return new RegistrationCommand(message);
        } else if (DimCommand.matches(message)) {
            return new DimCommand(message);
        }
        throw new LightwaverfConnectMessageException("Message not recorgnised: " + message);
    }

    public LWCommand getRegistrationCommand() {
        return new RegistrationCommand();
    }

    /**
     * Increment message counter, so different messages have different IDs
     * Important for getting corresponding OK acknowledgements from port 9761
     * tagged with the same counter value
     */
    private int getAndIncrementMessageId() {
        try {
            lock.lock();
            int myMessageId = nextMessageId;
            if (myMessageId >= 999) {
                nextMessageId = 200;
            } else {
                nextMessageId++;
            }
            return myMessageId;
        } finally {
            lock.unlock();
        }
    }
}
