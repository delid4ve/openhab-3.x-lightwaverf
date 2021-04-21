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

import java.util.ArrayList;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 *
 * @author David Murton - Initial contribution
 */
public class LightwaverfSmartStructures {

    @SerializedName("structures")
    @Expose
    private static ArrayList<LightwaverfSmartStructures> structures = null;

    public ArrayList<LightwaverfSmartStructures> getStructures() {
        return structures;
    }

    public void setStructures(ArrayList<LightwaverfSmartStructures> structures) {
        LightwaverfSmartStructures.structures = structures;
    }
}
