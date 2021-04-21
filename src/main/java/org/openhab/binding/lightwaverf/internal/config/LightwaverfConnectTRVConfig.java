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
 * The {@link LightwaverfConnectTRVConfig} class defines the configuration for a
 * generation 1 connect series TRV
 *
 * @author David Murton - Initial contribution
 */
@NonNullByDefault
public class LightwaverfConnectTRVConfig {

    public String roomId = "";
    public String serialNo = "";

    @Override
    public String toString() {
        return "[roomid=" + roomId + ", serialno=" + serialNo + "]";
    }
}
