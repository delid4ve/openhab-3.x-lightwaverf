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

 /**
 * The {@link lightwaverfBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */

package org.openhab.binding.lightwaverf.internal.handler;

import java.util.Collection;
import java.util.Collections;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.openhab.binding.lightwaverf.internal.config.StructureConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openhab.binding.lightwaverf.internal.Http;
import org.openhab.binding.lightwaverf.internal.api.discovery.Root;
import org.openhab.binding.lightwaverf.internal.LWDiscoveryService;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class StructureHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(StructureHandler.class);
    private final static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
    .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
    private String t = this.thing.getConfiguration().get("structureId").toString();
    private StructureConfig config;
    private LWAccountHandler accountHandler;
    private Root structureStatus;
    public LWAccountHandler connection;

    public StructureHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing {} handler.", getThing().getThingTypeUID());
        config = getConfigAs(StructureConfig.class);
        logger.warn("Start Update");
        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge Not set");
            return;
        }
        
        logger.debug("Structure Config: {}", config);
        initializeBridge(bridge.getHandler(), bridge.getStatus());
        
    }

    @Override
    public void dispose() {
        // logger.debug("Running dispose()");
        
        accountHandler = null;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(LWDiscoveryService.class);
    }

    public ThingUID getID() {
        return getThing().getUID();
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        // logger.debug("Bridge Status Changed {} for thing {}", bridgeStatusInfo, getThing().getUID());
        Bridge bridge = getBridge();
        if (bridge != null) {
            initializeBridge(bridge.getHandler(), bridgeStatusInfo.getStatus());
        }
    }

    private void initializeBridge(ThingHandler thingHandler, ThingStatus bridgeStatus) {
        // logger.debug("Initialize Bridge {} for thing {}", bridgeStatus, getThing().getUID());

        if (thingHandler != null && bridgeStatus != null) {
            accountHandler = (LWAccountHandler) thingHandler;

            if (bridgeStatus == ThingStatus.ONLINE) {
                updateStatus(ThingStatus.ONLINE);
        
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    
    public void updateState(String channelId, String string) throws Exception {
        String response = Http.httpClient("structure", null, "application/json",t);
        Root structureStatus = gson.fromJson(response, Root.class);
        updateChannels(structureStatus);
    }
   
    private synchronized void updateChannels(Root newStructureStatus) throws Exception {
        structureStatus = newStructureStatus;
        for (Channel channel : getThing().getChannels()) {
            updateChannels(channel.getUID().getId(), structureStatus);
        }
    }

    private void updateChannels(String channelId, Root structureStatus) throws Exception {
        switch (channelId) {
            case "name":
                updateState(channelId, structureStatus.getName().toString());
                break;
        }
    }
    

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            logger.debug("Refresh command not supported");
            return;
        }
        if (accountHandler == null) {
            logger.warn("No connection to Lightwave available, ignoring command");
            return;
        }
    }
}
