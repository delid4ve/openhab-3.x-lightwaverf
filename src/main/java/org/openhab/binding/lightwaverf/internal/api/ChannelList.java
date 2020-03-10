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
 * 
 */

package org.openhab.binding.lightwaverf.internal.api;

import java.util.List;
/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */
public class ChannelList {

private List<ChannelListItem> channelList = null;

public List<ChannelListItem> getChannelList() {
    return channelList;
}

public void setChannelList(List<ChannelListItem> channelList) {
    this.channelList = channelList;
}


}
