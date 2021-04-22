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
import org.openhab.core.types.State;

/**
 * 
 * @author Neil Renaud - Initial contribution
 * @author David Murton - Since OH 2.x
 * 
 */
@NonNullByDefault
public class HeatInfoRequest extends AbstractLWCommand implements RoomMessage {

    private static final Pattern REG_EXP = Pattern.compile(".*?([0-9]{1,3}),!R([0-9])F\\*r\\s*");
    private static final String FUNCTION = "*r";

    private final MessageId messageId;
    private final String roomId;

    public HeatInfoRequest(int messageId, String roomId) {
        this.messageId = new GeneralMessageId(messageId);
        this.roomId = roomId;
    }

    public HeatInfoRequest(String message) throws LightwaverfConnectMessageException {
        try {
            Matcher m = REG_EXP.matcher(message);
            m.matches();
            messageId = new GeneralMessageId(Integer.valueOf(m.group(1)));
            roomId = m.group(2);
        } catch (Exception e) {
            throw new LightwaverfConnectMessageException("Error converting message: " + message);
        }
    }

    @Override
    public @Nullable String getCommandString() {
        return getFunctionMessageString(messageId, roomId, FUNCTION);
    }

    @Override
    public String getRoomId() {
        return roomId;
    }

    @Override
    public @Nullable State getState(@Nullable LWType type) {
        return null;
    }

    @Override
    public @Nullable MessageId getMessageId() {
        return messageId;
    }

    public static boolean matches(String message) {
        return message.contains(FUNCTION);
    }

    @Override
    public @Nullable MessageType getMessageType() {
        return MessageType.HEAT_REQUEST;
    }
}
