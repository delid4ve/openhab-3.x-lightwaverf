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
package org.openhab.binding.lightwaverf.internal.handler;

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.lightwaverf.internal.UpdateListener;
import org.openhab.binding.lightwaverf.internal.api.FeatureStatus;
import org.openhab.binding.lightwaverf.internal.api.discovery.Devices;
import org.openhab.binding.lightwaverf.internal.api.discovery.FeatureSets;
import org.openhab.binding.lightwaverf.internal.config.AccountConfig;
import org.openhab.binding.lightwaverf.internal.LWDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LWAccountHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(LWAccountHandler.class);
    private ScheduledFuture<?> connectionCheckTask;
    private ScheduledFuture<?> refreshTask;
    private AccountConfig config;
    private UpdateListener listener;
    private ScheduledFuture<?> listTask;
    
    private String list;
    public LWAccountHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Lightwave account handler.");
        config = getConfigAs(AccountConfig.class);
        listener = new UpdateListener();
        startConnectionCheck();
        if (listTask == null || listTask.isCancelled()) {
            listTask = scheduler.schedule(this::startRefresh, 10, TimeUnit.SECONDS);
        }
    }

    private void startConnectionCheck() {
        if (connectionCheckTask == null || connectionCheckTask.isCancelled()) {
            logger.debug("Start periodic connection check");
            Runnable runnable = () -> {
                logger.debug("Checking Lightwave connection");
                if (isConnected()) {
                    logger.debug("Connection to Lightwave established");
                } else {
                        try {
                        connect();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                }
            };
            connectionCheckTask = scheduler.scheduleWithFixedDelay(runnable, 0, 60, TimeUnit.SECONDS);
        } else {
             logger.debug("Connection check task already running");
        }
    }

    private void connect() throws Exception {
        logger.debug("Initializing connection to Lightwave");
        updateStatus(ThingStatus.OFFLINE);
            listener.login(config.username, config.password);
            properties();
            updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        // logger.debug("Running dispose()");
        stopConnectionCheck();
        config = null;
        if (refreshTask != null) {
            refreshTask.cancel(true);
            listTask.cancel(true);
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(LWDiscoveryService.class);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

    }

    public ThingUID getID() {
        return getThing().getUID();
    }

    public synchronized boolean isConnected() {
        return listener.isConnected();
    }

    public synchronized List<Devices> devices() {
        return listener.devices();
    }

    public synchronized List<FeatureSets> featureSets() {
        return listener.featureSets();
    }
    public synchronized List<FeatureStatus> featureStatus() {
        return listener.featureStatus();
    }

    public synchronized List<String> channelList() {
        return listener.channelList();
    }

    public synchronized List<String> cLinked() {
        return listener.cLinked();
    }

    private void properties() {
        Map<String, String> properties = editProperties();
        properties.clear();
        for (int i=0; i < devices().size(); i++) { 
            String deviceArray[] = devices().get(i).getDeviceId().split("-");
            list = "Simple DeviceId (sdId): " + deviceArray[1] + ", Product: " + devices().get(i).getDesc() +
            ", Gen: " + devices().get(i).getGen() + ", Channels: " + devices().get(i).getFeatureSets().size() + "\r\n";
            properties.put("Connected Device: " + i + "", list);
        }
        updateProperties(properties);
    }
    private void startRefresh() {
        int refresh = Integer.valueOf(this.thing.getConfiguration().get("pollingInterval").toString());
        if (channelList().size() == 0) {
            logger.warn("Channel List For Updating Is Empty");
        }
        if (refreshTask == null || refreshTask.isCancelled()) {
            refreshTask = scheduler.scheduleWithFixedDelay(this::updateStateAndChannels, 0, refresh, TimeUnit.SECONDS);
        }
    }

    private void updateStateAndChannels() {
        if (channelList().size() > 0) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                        listener.updateListener();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
    }
}).start();
        }
    }

    

    private void stopConnectionCheck() {
        if (connectionCheckTask != null) {
            logger.debug("Stop periodic connection check");
            connectionCheckTask.cancel(true);
            connectionCheckTask = null;
        }
    }

    

}
