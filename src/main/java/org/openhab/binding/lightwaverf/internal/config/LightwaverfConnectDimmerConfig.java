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
package org.openhab.binding.lightwaverf.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link LightwaverfConnectDimmerConfig} class defines the configuration for a
 * generation 1 connect series dimmer
 *
 * @author David Murton - Initial contribution
 */
@NonNullByDefault
public class LightwaverfConnectDimmerConfig {

    public String roomid = "";
    public String deviceid = "";

    @Override
    public String toString() {
        return "[roomid=" + roomid + ", deviceid=" + deviceid + "]";
    }
}
