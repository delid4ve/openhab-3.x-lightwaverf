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
 * The {@link lightwaverfBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */
@NonNullByDefault
public class LightwaverfSmartAccountConfig {

    public String username = "";
    public String password = "";
    public int electricityCost = 20;
    public int retries = 5;
    public int delay = 100;
    public int timeout = 1000;

    @Override
    public String toString() {
        return "[username=" + username + ", password=" + getPasswordForPrinting() + ", electricityCost="
                + electricityCost + ", retries=" + retries + ", delay=" + delay + ", timeout=" + timeout + "]";
    }

    private String getPasswordForPrinting() {
        if (password != null) {
            return password.isEmpty() ? "<empty>" : "*********";
        }
        return "<null>";
    }
}
