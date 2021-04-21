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
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.State;

/**
 * 
 * @author Neil Renaud - Initial contribution
 * @author David Murton - Since OH 2.x
 * 
 */
@NonNullByDefault
public class DimCommand extends AbstractLWCommand implements RoomDeviceMessage {

    private static final Pattern REG_EXP = Pattern.compile(".*?([0-9]{1,3}),!R([0-9])D([0-9])FdP([0-9]{1,2}).*\\s*");
    private static final String DIM_FUNCTION = "d";
    private static final String OFF_FUNCTION = "0";

    private final String roomId;
    private final String deviceId;
    private final int openhabDimLevel;
    private final int lightWaveDimLevel;
    private final MessageId messageId;

    /**
     * Commands are like: 100,!R2D3FdP1 (Lowest Brightness) 101,!R2D3FdP32 (High
     * brightness)
     */

    public DimCommand(int messageId, String roomId, String deviceId, int dimmingLevel) {
        this.roomId = roomId;
        this.deviceId = deviceId;
        this.openhabDimLevel = dimmingLevel;
        this.lightWaveDimLevel = convertOpenhabDimToLightwaveDim(dimmingLevel);
        this.messageId = new GeneralMessageId(messageId);
    }

    public DimCommand(String message) throws LightwaverfConnectMessageException {
        try {
            Matcher matcher = REG_EXP.matcher(message);
            matcher.matches();
            this.messageId = new GeneralMessageId(Integer.valueOf(matcher.group(1)));
            this.roomId = matcher.group(2);
            this.deviceId = matcher.group(3);
            this.lightWaveDimLevel = Integer.valueOf(matcher.group(4));
            this.openhabDimLevel = convertLightwaveDimToOpenhabDim(lightWaveDimLevel);
        } catch (Exception e) {
            throw new LightwaverfConnectMessageException("Error converting Dimming message: " + message, e);
        }
    }

    @Override
    public @Nullable String getCommandString() {
        // Sending a Dim command with 0 sets light to full brightness
        if (lightWaveDimLevel == 0) {
            return getMessageString(messageId, roomId, deviceId, OFF_FUNCTION);
        } else {
            return getMessageString(messageId, roomId, deviceId, DIM_FUNCTION, lightWaveDimLevel);
        }
    }

    /**
     * Convert a 0-31 scale value to a percent type.
     * 
     * @param pt
     *            percent type to convert
     * @return converted value 0-31
     */
    public static int convertOpenhabDimToLightwaveDim(int openhabDim) {
        return (int) Math.round(((32.0 / 100.0) * Double.valueOf(openhabDim)));
    }

    /**
     * Convert a 0-31 scale value to a percent type. 0 -> 0%, 1 -> 4%, 2 -> 7%, 3 ->
     * 10%, 4 -> 13%, 5 -> 16% 6 -> 19%, 7 -> 22%, 8 -> 25%, 9 -> 29%, 10 -> 32%, 11
     * -> 35%, 12 -> 38%, 13 -> 41%, 14 -> 44%, 15 -> 47%, 16 -> 50%, 17 -> 53%, 18
     * -> 57%, 19 -> 60%, 20 -> 63%, 21 -> 66%, 22 -> 69%, 23 -> 72%, 24 -> 75%, 25
     * -> 79%, 26 -> 82%, 27 -> 85%, 28 -> 88%, 29 -> 91%, 30 -> 94%, 31 -> 97%, 32
     * -> 100%,
     * 
     * @param pt percent type to convert
     * @return converted value 0-31
     * @throws LightwaverfConnectMessageException
     */
    public static int convertLightwaveDimToOpenhabDim(int lightwavedim) throws LightwaverfConnectMessageException {
        throw new LightwaverfConnectMessageException("Error converting Dimming message:" + lightwavedim);
        // return (int) Math.round(((100.0 / 32.0) * Double.valueOf(lightwavedim)));
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
                return new PercentType(openhabDimLevel);
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
        if (that instanceof DimCommand) {
            return Objects.equals(this.messageId, ((DimCommand) that).messageId)
                    && Objects.equals(this.roomId, ((DimCommand) that).roomId)
                    && Objects.equals(this.deviceId, ((DimCommand) that).deviceId)
                    && Objects.equals(this.openhabDimLevel, ((DimCommand) that).openhabDimLevel)
                    && Objects.equals(this.lightWaveDimLevel, ((DimCommand) that).lightWaveDimLevel);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, roomId, deviceId, openhabDimLevel, lightWaveDimLevel);
    }

    @Override
    public @Nullable MessageType getMessageType() {
        return MessageType.ROOM_DEVICE;
    }

    public static boolean matches(String message) {
        return message.contains("FdP");
    }
}
