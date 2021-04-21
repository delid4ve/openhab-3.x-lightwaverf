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
package org.openhab.binding.lightwaverf.internal.listeners;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.lightwaverf.internal.commands.CommandOk;
import org.openhab.binding.lightwaverf.internal.commands.HeatInfoRequest;
import org.openhab.binding.lightwaverf.internal.commands.VersionMessage;
import org.openhab.binding.lightwaverf.internal.dto.LightwaverfConnectResponse;

/**
 * 
 * @author Neil Renaud - Initial contribution
 * @author David Murton - Since OH 2.x
 * 
 */
@NonNullByDefault
public interface LightwaverfConnectMessageListener {

    public void roomDeviceMessageReceived(LightwaverfConnectResponse message);

    public void roomMessageReceived(LightwaverfConnectResponse message);

    public void serialMessageReceived(LightwaverfConnectResponse message);

    public void okMessageReceived(CommandOk message);

    public void versionMessageReceived(VersionMessage message);

    public void heatInfoMessageReceived(HeatInfoRequest command);
}
