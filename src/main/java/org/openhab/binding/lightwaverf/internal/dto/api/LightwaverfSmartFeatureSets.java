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

package org.openhab.binding.lightwaverf.internal.dto.api;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 *
 * @author David Murton - Initial contribution
 */
public class LightwaverfSmartFeatureSets {
    @SerializedName("featureSetId")
    @Expose
    private String featureSetId;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("features")
    @Expose
    private List<LightwaverfSmartFeatures> features = null;

    public String getFeatureSetId() {
        return featureSetId;
    }

    public void setFeatureSetId(String featureSetId) {
        this.featureSetId = featureSetId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<LightwaverfSmartFeatures> getFeatures() {
        return features;
    }

    public void setFeatures(List<LightwaverfSmartFeatures> features) {
        this.features = features;
    }
}
