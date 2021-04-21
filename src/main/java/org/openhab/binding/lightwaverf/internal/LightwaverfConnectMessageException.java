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
package org.openhab.binding.lightwaverf.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This represents an exception parsing a LightwaveRf Message.
 * /**
 * 
 * @author Neil Renaud - Initial contribution
 * @author David Murton - Since OH 2.x
 * 
 */
@NonNullByDefault
public class LightwaverfConnectMessageException extends Exception {

    private static final long serialVersionUID = -2131620053984993990L;

    public LightwaverfConnectMessageException(String message) {
        super(message);
    }

    public LightwaverfConnectMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
