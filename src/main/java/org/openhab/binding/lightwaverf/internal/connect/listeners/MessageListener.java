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
package org.openhab.binding.lightwaverf.internal.connect.listeners;

import org.openhab.binding.lightwaverf.internal.connect.commands.CommandOk;
import org.openhab.binding.lightwaverf.internal.connect.commands.HeatInfoRequest;
import org.openhab.binding.lightwaverf.internal.connect.commands.VersionMessage;
import org.openhab.binding.lightwaverf.internal.connect.dto.Response;

/**
 * @author Neil Renaud
 * @since 1.7.0
 */
public interface MessageListener {

    public void roomDeviceMessageReceived(Response message);

    public void roomMessageReceived(Response message);

    public void serialMessageReceived(Response message);

    public void okMessageReceived(CommandOk message);

    public void versionMessageReceived(VersionMessage message);

    public void heatInfoMessageReceived(HeatInfoRequest command);
}
