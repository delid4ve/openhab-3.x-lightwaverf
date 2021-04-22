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
package org.openhab.binding.lightwaverf.internal.commands;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lightwaverf.internal.LightwaverfConnectMessageException;
import org.openhab.binding.lightwaverf.internal.utilities.AbstractLWCommand;
import org.openhab.binding.lightwaverf.internal.utilities.GeneralMessageId;
import org.openhab.binding.lightwaverf.internal.utilities.LWType;
import org.openhab.binding.lightwaverf.internal.utilities.MessageId;
import org.openhab.binding.lightwaverf.internal.utilities.MessageType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;

/**
 * Represents LightwaveRf Relay commands. On the LAN commands look like:
 * 103,!R1D4F^|Stop|Switch 1
 * 104,!R1D4F)|Close|Switch 1
 * 106,!R1D4F(|Open|Switch 1 *
 * 
 * /**
 * 
 * @author Neil Renaud - Initial contribution
 * @author David Murton - Since OH 2.x
 * 
 */
@NonNullByDefault
public class RelayCommand extends AbstractLWCommand implements RoomDeviceMessage {

    private static final Pattern REG_EXP = Pattern.compile(".*?(\\d{1,3}),!R(\\d)D(\\d)F([\\^\\)\\(]).*\\s*");
    private static final String STOP_FUNCTION = "^";
    private static final String OPEN_FUNCTION = ")";
    private static final String CLOSE_FUNCTION = "(";

    private final MessageId messageId;
    private final String roomId;
    private final String deviceId;
    private final int state;

    public RelayCommand(int messageId, String roomId, String deviceId, int state) {
        this.messageId = new GeneralMessageId(messageId);
        this.roomId = roomId;
        this.deviceId = deviceId;
        this.state = state;
    }

    public RelayCommand(String message) throws LightwaverfConnectMessageException {
        try {
            Matcher matcher = REG_EXP.matcher(message);
            matcher.matches();
            this.messageId = new GeneralMessageId(Integer.valueOf(matcher.group(1)));
            this.roomId = matcher.group(2);
            this.deviceId = matcher.group(3);
            String function = matcher.group(4);
            if (STOP_FUNCTION.equals(function)) {
                state = 0;
            } else if (OPEN_FUNCTION.equals(function)) {
                state = 1;
            } else if (CLOSE_FUNCTION.equals(function)) {
                state = -1;
            } else {
                throw new LightwaverfConnectMessageException(
                        "Received Message has invalid function[" + function + "]: " + message);
            }
        } catch (Exception e) {
            throw new LightwaverfConnectMessageException("Error converting message: " + message, e);
        }
    }

    @Override
    public @Nullable String getCommandString() {
        String function = null;
        switch (state) {
            case -1:
                function = CLOSE_FUNCTION;
                break;
            case 0:
                function = STOP_FUNCTION;
                break;
            case 1:
                function = OPEN_FUNCTION;
                break;
            default:
                break;
        }
        if (function != null) {
            return getMessageString(messageId, roomId, deviceId, function);
        }
        return null;
    }

    @Override
    public String getRoomId() {
        return roomId;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public @Nullable State getState(@Nullable LWType type) {
        switch (type) {
            case RELAY:
                return new DecimalType(state);
            default:
                return null;
        }
    }

    @Override
    public @Nullable MessageId getMessageId() {
        return messageId;
    }

    @Override
    public boolean equals(@Nullable Object that) {
        if (that instanceof RelayCommand) {
            return Objects.equals(this.messageId, ((RelayCommand) that).messageId)
                    && Objects.equals(this.roomId, ((RelayCommand) that).roomId)
                    && Objects.equals(this.deviceId, ((RelayCommand) that).deviceId)
                    && Objects.equals(this.state, ((RelayCommand) that).state);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, roomId, deviceId, state);
    }

    @Override
    public @Nullable MessageType getMessageType() {
        return MessageType.ROOM_DEVICE;
    }

    public static boolean matches(String message) {
        return message.contains("F^") || message.contains("F(") || message.contains("F)");
    }
}
