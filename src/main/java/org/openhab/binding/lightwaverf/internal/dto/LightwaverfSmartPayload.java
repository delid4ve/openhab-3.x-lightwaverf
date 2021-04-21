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
package org.openhab.binding.lightwaverf.internal.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 *
 * @author David Murton - Initial contribution
 */
public class LightwaverfSmartPayload {

    @SerializedName("workerUniqueId")
    @Expose
    private String workerUniqueId;
    @SerializedName("token")
    @Expose
    private String token;

    @SerializedName("handlerId")
    @Expose
    private String handlerId;
    @SerializedName("featureId")
    @Expose
    private String featureId;
    @SerializedName("deviceId")
    @Expose
    private Integer deviceId;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("channel")
    @Expose
    private Integer channel;
    @SerializedName("writable")
    @Expose
    private Boolean writable;
    @SerializedName("stateless")
    @Expose
    private Boolean stateless;
    @SerializedName("virtual")
    @Expose
    private Boolean virtual;
    @SerializedName("value")
    @Expose
    private Long value;
    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("clientDeviceId")
    @Expose
    private String clientDeviceId;
    @SerializedName("_feature")
    @Expose
    private LightwaverfSmartFeature feature;

    @SerializedName("serverName")
    @Expose
    private String serverName;
    @SerializedName("connectionId")
    @Expose
    private String connectionId;

    public String getWorkerUniqueId() {
        return workerUniqueId;
    }

    public void setWorkerUniqueId(String workerUniqueId) {
        this.workerUniqueId = workerUniqueId;
    }

    public String getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(String handlerId) {
        this.handlerId = handlerId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Boolean getWritable() {
        return writable;
    }

    public void setWritable(Boolean writable) {
        this.writable = writable;
    }

    public Boolean getStateless() {
        return stateless;
    }

    public void setStateless(Boolean stateless) {
        this.stateless = stateless;
    }

    public Boolean getVirtual() {
        return virtual;
    }

    public void setVirtual(Boolean virtual) {
        this.virtual = virtual;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getClientDeviceId() {
        return this.clientDeviceId;
    }

    public void setClientDeviceId(String clientDeviceId) {
        this.clientDeviceId = clientDeviceId;
    }

    public LightwaverfSmartFeature getFeature() {
        return feature;
    }

    public void setFeature(LightwaverfSmartFeature feature) {
        this.feature = feature;
    }

    public String getConnectionId() {
        return this.connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getServerName() {
        return this.serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
}
