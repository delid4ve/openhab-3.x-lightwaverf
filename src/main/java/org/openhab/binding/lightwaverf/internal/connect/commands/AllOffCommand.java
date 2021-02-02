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
package org.openhab.binding.lightwaverf.internal.connect.commands;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.lightwaverf.internal.connect.exception.MessageException;
import org.openhab.binding.lightwaverf.internal.connect.utilities.AbstractLWCommand;
import org.openhab.binding.lightwaverf.internal.connect.utilities.GeneralMessageId;
import org.openhab.binding.lightwaverf.internal.connect.utilities.LWType;
import org.openhab.binding.lightwaverf.internal.connect.utilities.MessageId;
import org.openhab.binding.lightwaverf.internal.connect.utilities.MessageType;

/**
 * @author Neil Renaud
 * @since 1.8.0
 */
public class AllOffCommand extends AbstractLWCommand implements RoomMessage {

    private static final Pattern REG_EXP = Pattern.compile(".*?([0-9]{1,3}),!R([0-9])Fa.*\\s*");
    private static final String FUNCTION = "a";

    private final MessageId messageId;
    private final String roomId;

    /**
     * Commands are like: 100,!R2Fa
     */

    public AllOffCommand(int messageId, String roomId) {
        this.roomId = roomId;
        this.messageId = new GeneralMessageId(messageId);
    }

    public AllOffCommand(String message) throws MessageException {
        try {
            Matcher matcher = REG_EXP.matcher(message);
            matcher.matches();
            this.messageId = new GeneralMessageId(Integer.valueOf(matcher.group(1)));
            this.roomId = matcher.group(2);
        } catch (Exception e) {
            throw new MessageException("Error converting Dimming message: " + message, e);
        }
    }

    @Override
    public String getCommandString() {
        return getMessageString(messageId, roomId, FUNCTION);
    }

    @Override
    public String getRoomId() {
        return roomId;
    }

    @Override
    public State getState(LWType type) {
        switch (type) {
            case ALL_OFF:
                return OnOffType.OFF;
            default:
                return null;
        }
    }

    @Override
    public MessageId getMessageId() {
        return messageId;
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof AllOffCommand) {
            return Objects.equals(this.messageId, ((AllOffCommand) that).messageId)
                    && Objects.equals(this.roomId, ((AllOffCommand) that).roomId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, roomId);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.ROOM;
    }

    public static boolean matches(String message) {
        return message.contains("Fa");
    }
}
