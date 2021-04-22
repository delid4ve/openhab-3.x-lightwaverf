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
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.State;

/**
 * Represents LightwaveRf On/Off commands. On the LAN commands look like:
 * 100,!R2D3F0 (Off) 101,!R2D3F1 (On)
 * /**
 * 
 * @author Neil Renaud - Initial contribution
 * @author David Murton - Since OH 2.x
 * 
 */
@NonNullByDefault
public class OnOffCommand extends AbstractLWCommand implements RoomDeviceMessage {

    private static final Pattern REG_EXP = Pattern.compile(".*?(\\d{1,3}),!R(\\d)D(\\d)F([0,1]).*\\s*");
    private static final String ON_FUNCTION = "1";
    private static final String OFF_FUNCTION = "0";

    private final MessageId messageId;
    private final String roomId;
    private final String deviceId;
    private final boolean on;

    public OnOffCommand(int messageId, String roomId, String deviceId, boolean on) {
        this.messageId = new GeneralMessageId(messageId);
        this.roomId = roomId;
        this.deviceId = deviceId;
        this.on = on;
    }

    public OnOffCommand(String message) throws LightwaverfConnectMessageException {
        try {
            Matcher matcher = REG_EXP.matcher(message);
            matcher.matches();
            this.messageId = new GeneralMessageId(Integer.valueOf(matcher.group(1)));
            this.roomId = matcher.group(2);
            this.deviceId = matcher.group(3);
            String function = matcher.group(4);
            if (ON_FUNCTION.equals(function)) {
                on = true;
            } else if (OFF_FUNCTION.equals(function)) {
                on = false;
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
        String function = on ? "1" : "0";
        return getMessageString(messageId, roomId, deviceId, function);
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
            case DIMMER:
                return on ? PercentType.HUNDRED : OnOffType.OFF;
            case SWITCH:
                return on ? OnOffType.ON : OnOffType.OFF;
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
        if (that instanceof OnOffCommand) {
            return Objects.equals(this.messageId, ((OnOffCommand) that).messageId)
                    && Objects.equals(this.roomId, ((OnOffCommand) that).roomId)
                    && Objects.equals(this.deviceId, ((OnOffCommand) that).deviceId)
                    && Objects.equals(this.on, ((OnOffCommand) that).on);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, roomId, deviceId, on);
    }

    @Override
    public @Nullable MessageType getMessageType() {
        return MessageType.ROOM_DEVICE;
    }

    public static boolean matches(String message) {
        return message.contains("F0") || message.contains("F1");
    }
}
