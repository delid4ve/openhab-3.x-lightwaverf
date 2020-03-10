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

import org.eclipse.smarthome.core.thing.ChannelUID;

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which
 * are used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */

public class ChannelListItem {

    private String sdId;
    private ChannelUID uid;
    private String featureId;
    
    public ChannelListItem(String sdId,ChannelUID uid, String featureId) {
        this.sdId = sdId;
        this.uid = uid;
        this.featureId = featureId;
    }
    
    public String getFeatureId() {
    return featureId;
    }
    
    public void setFeatureId(String featureId) {
    this.featureId = featureId;
    }
    
    public String getSdId() {
    return sdId;
    }
    
    public void setSdId(String sdId) {
    this.sdId = sdId;
    }
    
    public ChannelUID getUID() {
        return uid;
        }
        
        public void setUID(ChannelUID uid) {
        this.uid = uid;
        }
    }