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

/**
 * The {@link lightwaverfBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author David Murton - Initial contribution
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.openhab.binding.lightwaverf.internal.api.AccessToken;
import org.openhab.io.net.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http {
    private final static Logger logger = LoggerFactory.getLogger(Http.class);


    private static Properties getHeader(String type) {
        Properties headers = new Properties();
        switch (type) {
        case "login":
            headers.put("x-lwrf-appid", "ios-01");
            break;
        case "structures":
        case "structure":
        case "feature":
        case "features":
            headers.put("Authorization", "Bearer " + AccessToken.getToken());
            break;
        }
        return headers;
    }

    private static String url(String type, String groupId) {
        String url;
        switch (type) {
        case "login":
            url = "https://auth.lightwaverf.com/v2/lightwaverf/autouserlogin/lwapps";
            break;
        case "structures":
            url = "https://publicapi.lightwaverf.com/v1/structures/";
            break;
        case "structure":
            url = "https://publicapi.lightwaverf.com/v1/structure/" + groupId;
            break;
        case "feature":
            url = "https://publicapi.lightwaverf.com/v1/feature/" + groupId;
            break;
        case "features":
            url = "https://publicapi.lightwaverf.com/v1/features/read";
            break;
        default:
            url = "";
        }
        return url;
    }

    public static String method(String type) {
        String method;
        switch (type) {
        case "login":
        case "feature":
        case "features":
            method = "POST";
            break;
        case "structures":
        case "structure":
            method = "GET";
            break;
        default:
            method = "GET";
        }
        return method;
    }

    public static String httpClient(String type, InputStream data, String other, String groupId) {
        String response = HttpUtil.executeUrl(method(type), url(type, groupId), getHeader(type), data, other, 100000);
        logger.debug("HTTP Client Response: {}", response);
        if (response.contains("structure not found")) {
            logger.warn("Structure not found, too many requests for {}", type);
        }
        return response;
    }

    public String httpClient2(String type, InputStream data, String other, String groupId) throws IOException {
        String response = HttpUtil.executeUrl(method(type), url(type, groupId), getHeader(type), data, other, 100000);
        logger.debug("HTTP Client Response: {}", response);
        if (response.contains("structure not found")) {
            logger.warn("Structure not found, too many requests for {}", type);
        }
        return response;
    }

    
    
}