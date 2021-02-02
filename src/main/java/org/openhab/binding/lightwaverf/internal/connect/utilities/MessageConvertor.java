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
package org.openhab.binding.lightwaverf.internal.connect.utilities;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.types.Type;
import org.openhab.binding.lightwaverf.internal.connect.commands.AllOffCommand;
import org.openhab.binding.lightwaverf.internal.connect.commands.CommandOk;
import org.openhab.binding.lightwaverf.internal.connect.commands.DimCommand;
import org.openhab.binding.lightwaverf.internal.connect.commands.MoodCommand;
import org.openhab.binding.lightwaverf.internal.connect.commands.OnOffCommand;
import org.openhab.binding.lightwaverf.internal.connect.commands.RegistrationCommand;
import org.openhab.binding.lightwaverf.internal.connect.commands.RelayCommand;
import org.openhab.binding.lightwaverf.internal.connect.commands.TemperatureCommand;
import org.openhab.binding.lightwaverf.internal.connect.commands.VersionMessage;
import org.openhab.binding.lightwaverf.internal.connect.exception.MessageException;

/**
 * Converts the openhab Type into a LightwaveRfCommand that can be sent to the
 * LightwaveRF Wifi link.
 *
 * @author Neil Renaud
 * @since 1.7.0
 */
public class MessageConvertor {

    // LightwaveRF messageId
    private int nextMessageId = 200;
    private final Lock lock = new ReentrantLock();

    public LWCommand convertToLightwaveRfMessage(String roomId, String deviceId, LWType deviceType, Type command) {
        int messageId = getAndIncrementMessageId();

        switch (deviceType) {
            case HEATING_BATTERY:
            case SIGNAL:
            case HEATING_CURRENT_TEMP:
            case HEATING_MODE:
            case VERSION:
                throw new IllegalArgumentException(deviceType + " : is read only it can't be set");
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
                    throw new RuntimeException("Unsupported Command: " + command);
                }
            case RELAY:
                if (command instanceof DecimalType) {
                    int state = ((DecimalType) command).intValue();
                    return new RelayCommand(messageId, roomId, deviceId, state);
                } else {
                    throw new RuntimeException("Unsupported Command: " + command);
                }
            case MOOD:
                if (command instanceof DecimalType) {
                    int state = ((DecimalType) command).intValue();
                    return new MoodCommand(messageId, roomId, state);
                } else {
                    throw new RuntimeException("Unsupported Command: " + command);
                }
            case ALL_OFF:
                if (command instanceof OnOffType) {
                    return new AllOffCommand(messageId, roomId);
                } else {
                    throw new RuntimeException("Unsupported Command: " + command);
                }
            default:
                throw new IllegalArgumentException(deviceType + " : is unexpected");
        }
    }

    public LWCommand convertFromLightwaveRfMessage(String message) throws MessageException {
        if (CommandOk.matches(message)) {
            return new CommandOk(message);
        } else if (VersionMessage.matches(message)) {
            return new VersionMessage(message);
        } else if (RegistrationCommand.matches(message)) {
            return new RegistrationCommand(message);
        } else if (DimCommand.matches(message)) {
            return new DimCommand(message);
        }
        throw new MessageException("Message not recorgnised: " + message);
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
