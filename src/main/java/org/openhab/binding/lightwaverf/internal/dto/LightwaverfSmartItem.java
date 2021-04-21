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
public class LightwaverfSmartItem {

    @SerializedName("itemId")
    @Expose
    private String itemId;
    @SerializedName("success")
    @Expose
    private Boolean success;
    @SerializedName("payload")
    @Expose
    private LightwaverfSmartPayload payload;
    @SerializedName("error")
    @Expose
    private LightwaverfSmartError error;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public LightwaverfSmartPayload getPayload() {
        return payload;
    }

    public void setPayload(LightwaverfSmartPayload payload) {
        this.payload = payload;
    }

    public LightwaverfSmartError getError() {
        return error;
    }

    public void setError(LightwaverfSmartError error) {
        this.error = error;
    }
}
