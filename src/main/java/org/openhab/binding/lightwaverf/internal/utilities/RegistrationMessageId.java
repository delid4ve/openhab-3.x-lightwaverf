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
package org.openhab.binding.lightwaverf.internal.utilities;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * 
 * @author Neil Renaud - Initial contribution
 * @author David Murton - Since OH 2.x
 * 
 */
@NonNullByDefault
public class RegistrationMessageId implements MessageId {

    private final int messageId;

    public RegistrationMessageId() {
        this.messageId = 100;
    }

    @Override
    public String getMessageIdString() {
        return String.valueOf(messageId);
    }

    @Override
    public boolean equals(@Nullable Object that) {
        if (that instanceof RegistrationMessageId) {
            return Objects.equals(this.messageId, ((RegistrationMessageId) that).messageId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }

    @Override
    public String toString() {
        return "LightwaveRfRegistrationMessageId[" + messageId + "]";
    }
}
