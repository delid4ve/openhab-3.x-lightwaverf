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
package org.openhab.binding.lightwaverf.internal.smart.listeners;

import java.util.EventListener;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Informs about the websocket connection.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public interface WsEventListener extends EventListener {

    void updateConnectionState(boolean connected);

    void onMessage(String message);
}
