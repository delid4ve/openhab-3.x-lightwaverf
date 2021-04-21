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
package org.openhab.binding.lightwaverf.internal.discovery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lightwaverf.internal.LWBindingConstants;
import org.openhab.binding.lightwaverf.internal.dto.api.*;
import org.openhab.binding.lightwaverf.internal.handler.*;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which
 * are used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */

@Component(service = LWDiscoveryService.class, immediate = true, configurationPid = "discovery.lightwaverf")
@NonNullByDefault
public class LWDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService, DiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(LWDiscoveryService.class);
    private static final int DISCOVER_TIMEOUT_SECONDS = 10;
    private @Nullable LightwaverfSmartAccountHandler account;
    private @Nullable ScheduledFuture<?> scanTask;

    public LWDiscoveryService() {
        super(LWBindingConstants.DISCOVERABLE_THING_TYPE_UIDS, DISCOVER_TIMEOUT_SECONDS, false);
    }

    @Override
    protected void activate(@Nullable Map<String, Object> configProperties) {
        logger.debug("Activate Background Discovery");
        super.activate(configProperties);
    }

    @Override
    public void deactivate() {
        logger.debug("Deactivate Background discovery");
        super.deactivate();
    }

    @Override
    public void startBackgroundDiscovery() {
        logger.debug("Start Background Discovery");
        try {
            discover();
        } catch (Exception e) {
        }
    }

    @Override
    protected void startScan() {
        // logger.debug("Start Scan");
        if (this.scanTask != null) {
            scanTask.cancel(true);
        }
        this.scanTask = scheduler.schedule(() -> {
            try {
                discover();
            } catch (Exception e) {
            }
        }, 0, TimeUnit.SECONDS);
    }

    @Override
    protected void stopScan() {
        // logger.debug("Stop Scan");
        super.stopScan();

        ScheduledFuture<?> scanTask = this.scanTask;
        if (scanTask != null) {
            scanTask.cancel(true);
            this.scanTask = null;
        }
    }

    private void discover() {
        logger.debug("Start Discovery");
        final LightwaverfSmartAccountHandler account = this.account;
        if (account != null) {
            ThingUID bridgeUID = account.getThing().getUID();
            // Create a list of all our current things so we dont re-add
            List<Thing> things = account.getThing().getThings();
            Set<String> deviceids = new HashSet<>();
            for (int i = 0; i < things.size(); i++) {
                String thingid = things.get(i).getConfiguration().get("deviceid").toString();
                deviceids.add(thingid);
            }

            // Get the device list
            List<LightwaverfSmartDevices> devices = account.getDevices();

            for (int j = 0; j < devices.size(); j++) {
                logger.debug("Processing Device {} of {}", (j + 1), devices.size());
                String deviceid = devices.get(j).getDeviceId();
                String productCode = devices.get(j).getProductCode();
                String label = devices.get(j).getName();
                logger.debug("Device has id {} with name {} and the product code is {}", deviceid, label, productCode);
                if (!deviceids.contains(productCode)) {
                    ThingTypeUID thingTypeUid = LWBindingConstants.createMap().get(productCode);
                    if (thingTypeUid != null) {
                        String uid = deviceid.replaceAll("[^a-zA-Z0-9]", "");
                        logger.debug("Attempting to create thing for: {} - {} - {}", thingTypeUid,
                                account.getThing().getUID(), uid);
                        ThingUID deviceThing = new ThingUID(thingTypeUid, account.getThing().getUID(), uid);
                        logger.debug("Found a new supported device: {} - {}", productCode, deviceid);
                        Map<String, Object> properties = new HashMap<>();
                        Integer channels = devices.get(j).getFeatureSets().size();
                        properties.put("deviceid", deviceid);
                        properties.put("Name", label);
                        properties.put("Device", devices.get(j).getDevice());
                        properties.put("Type", devices.get(j).getCat());
                        properties.put("Description", devices.get(j).getDesc());
                        properties.put("Product", devices.get(j).getProduct());
                        properties.put("Product Code", productCode);
                        properties.put("Category", devices.get(j).getCat());
                        properties.put("Generation", devices.get(j).getGen());
                        properties.put("Channels", channels.toString());
                        logger.debug("Added Properties for device: {} - {}", productCode, deviceid);
                        logger.debug("Created device thing for device: {} - {}", productCode, deviceid);
                        thingDiscovered(
                                DiscoveryResultBuilder.create(deviceThing).withLabel(label).withProperties(properties)
                                        .withRepresentationProperty("deviceid").withBridge(bridgeUID).build());
                        logger.debug("Added Supported device: {} - {}", productCode, deviceid);
                    } else {
                        logger.error("Unsupported device found, please inform the binding developer: {} - {} - {}",
                                productCode, label, deviceid);
                    }
                }
            }
        }
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof LightwaverfSmartAccountHandler) {
            account = (LightwaverfSmartAccountHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return account;
    }
}
