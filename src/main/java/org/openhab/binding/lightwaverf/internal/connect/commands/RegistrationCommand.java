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

import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.lightwaverf.internal.connect.exception.MessageException;
import org.openhab.binding.lightwaverf.internal.connect.utilities.AbstractLWCommand;
import org.openhab.binding.lightwaverf.internal.connect.utilities.GeneralMessageId;
import org.openhab.binding.lightwaverf.internal.connect.utilities.LWCommand;
import org.openhab.binding.lightwaverf.internal.connect.utilities.LWType;
import org.openhab.binding.lightwaverf.internal.connect.utilities.MessageId;
import org.openhab.binding.lightwaverf.internal.connect.utilities.MessageType;
import org.openhab.binding.lightwaverf.internal.connect.utilities.RegistrationMessageId;

/**
 * @author Neil Renaud
 * @since 1.7.0
 */
public class RegistrationCommand extends AbstractLWCommand implements LWCommand {

    private static final Pattern REG_EXP = Pattern.compile(".*?([0-9]{1,3}),!F\\*p\\s*");
    private final MessageId messageId;
    private static final String FUNCTION = "*";
    private static final String PARAMETER = "";

    public RegistrationCommand(String message) throws MessageException {
        try {
            Matcher m = REG_EXP.matcher(message);
            m.matches();
            messageId = new GeneralMessageId(Integer.valueOf(m.group(1)));
        } catch (Exception e) {
            throw new MessageException("Error converting message: " + message, e);
        }
    }

    public RegistrationCommand() {
        this.messageId = new RegistrationMessageId();
    }

    @Override
    public String getCommandString() {
        return getDeviceRegistrationMessageString(messageId, FUNCTION, PARAMETER);
    }

    @Override
    public MessageId getMessageId() {
        return messageId;
    }

    @Override
    public State getState(LWType type) {
        return null;
    }

    public static boolean matches(String message) {
        Matcher m = REG_EXP.matcher(message);
        return m.matches();
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof RegistrationCommand) {
            return Objects.equals(this.messageId, ((RegistrationCommand) that).messageId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }

    @Override
    public String toString() {
        return "LightwaveRfDeviceRegistration[MessageId: " + messageId + "]";
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.DEVICE_REGISTRATION;
    }
}
