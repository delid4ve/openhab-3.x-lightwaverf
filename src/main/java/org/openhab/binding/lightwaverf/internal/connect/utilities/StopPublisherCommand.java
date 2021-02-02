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

import org.eclipse.smarthome.core.types.State;

/**
 * @author Neil Renaud
 * @since 1.7.0
 */
public class StopPublisherCommand implements LWCommand {

    @Override
    public String getCommandString() {
        return null;
    }

    public String getRoomId() {
        return null;
    }

    public String getDeviceId() {
        return null;
    }

    @Override
    public State getState(LWType type) {
        return null;
    }

    @Override
    public MessageId getMessageId() {
        return null;
    }

    @Override
    public MessageType getMessageType() {
        return null;
    }
}
