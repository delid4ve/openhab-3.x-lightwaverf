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
package org.openhab.binding.lightwaverf.internal;

import static org.openhab.binding.lightwaverf.internal.LWBindingConstants.*;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.io.net.http.HttpClientFactory;
import org.eclipse.smarthome.io.net.http.WebSocketFactory;
import org.openhab.binding.lightwaverf.internal.connect.handler.BridgeHandler;
import org.openhab.binding.lightwaverf.internal.connect.handler.TRVHandler;
import org.openhab.binding.lightwaverf.internal.smart.handler.DeviceHandler;
import org.openhab.binding.lightwaverf.internal.smart.handler.LWAccountHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link lightwaverfHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author David Murton - Initial contribution
 */
// @NonNullByDefault
@Component(configurationPid = "binding.lightwaverf", service = { ThingHandlerFactory.class, })

public class LWHandlerFactory extends BaseThingHandlerFactory {

    private final WebSocketFactory webSocketFactory;
    private final HttpClientFactory httpClientFactory;
    private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPE_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_LIGHTWAVE_ACCOUNT.equals(thingTypeUID)) {
            return new LWAccountHandler((Bridge) thing, webSocketFactory, httpClientFactory, gson);
        } else if (THING_TYPE_LIGHTWAVE_1HUB.equals(thingTypeUID)) {
            return new BridgeHandler((Bridge) thing);
        } else if (THING_TYPE_LIGHTWAVE_1TRV.equals(thingTypeUID)) {
            return new TRVHandler((Thing) thing);
        } else {
            return new DeviceHandler(thing);
        }
    }

    @Activate
    public LWHandlerFactory(final @Reference WebSocketFactory webSocketFactory,
            final @Reference HttpClientFactory httpClientFactory) {
        this.webSocketFactory = webSocketFactory;
        this.httpClientFactory = httpClientFactory;
    }
}
