/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import java.awt.Color;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lightwaverf.internal.dto.LightwaverfSmartPayload;
import org.openhab.binding.lightwaverf.internal.dto.LightwaverfSmartRequest;
import org.openhab.binding.lightwaverf.internal.listeners.LightwaverfSmartDeviceListener;
import org.openhab.binding.lightwaverf.internal.listeners.LightwaverfSmartListener;
import org.openhab.core.library.types.*;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * 
 * @author David Murton - Initial contribution
 * 
 */
@NonNullByDefault
public class LightwaverfSmartCommandManager implements Runnable, LightwaverfSmartListener {

    private final Logger logger = LoggerFactory.getLogger(LightwaverfSmartCommandManager.class);
    private final ConcurrentMap<Integer, CountDownLatch> latchMap = new ConcurrentHashMap<Integer, CountDownLatch>();
    /** Queue of messages to send */
    private final BlockingDeque<LightwaverfSmartRequest> queue = new LinkedBlockingDeque<LightwaverfSmartRequest>();
    /** Map of Integers so we can count retry attempts. */
    private final ConcurrentMap<Integer, Integer> retryCountMap = new ConcurrentHashMap<Integer, Integer>();
    private final ConcurrentMap<Integer, LightwaverfSmartRequest> messages = new ConcurrentHashMap<Integer, LightwaverfSmartRequest>();
    // For multiple hubs the response only has a simplified id so we need to log the actual device
    // private final ConcurrentMap<Integer, String> sentByDevice = new ConcurrentHashMap<Integer, String>();

    private final Gson gson;
    private int timeoutForOkMessagesMs = 1000;
    private Integer retries = 0;
    private final LightwaverfSmartListener listener;
    // Device Listeners
    private Map<String, LightwaverfSmartDeviceListener> deviceListeners = new HashMap<>();
    // Current transactionid
    private int transactionId = 1;
    private final String uuid = UUID.randomUUID().toString();
    private final String deviceUuid = UUID.randomUUID().toString();

    public String getDeviceUuid() {
        return this.deviceUuid;
    }

    /** Boolean to indicate if we are running */
    private boolean running = false;
    private String token = "";
    private Boolean connected = false;

    private Map<String, String> featureMap = new HashMap<String, String>();

    public LightwaverfSmartCommandManager(LightwaverfSmartListener listener, Gson gson) {
        this.gson = gson;
        this.listener = listener;
    }

    public synchronized void start(Integer retries, Integer timeout) {
        this.retries = retries;
        this.timeoutForOkMessagesMs = timeout;
        startRunning();
    }

    public synchronized void stop() {
        stopRunning();
        queue.clear();
        retryCountMap.clear();
        latchMap.clear();
        messages.clear();
        this.connected = false;
    }

    public void startRunning() {
        this.running = true;
    }

    public void stopRunning() {
        this.running = false;
    }

    @Override
    public void run() {
        logger.trace("Message Queue is running");
        try {
            @Nullable
            LightwaverfSmartRequest command = queue.take();
            if (command != null) {
                if (command.getOperation() == null) {
                    sendMessage("{}");
                    return;
                }
                CountDownLatch latch = new CountDownLatch(1);
                latchMap.putIfAbsent(command.getTransactionId(), latch);
                retryCountMap.putIfAbsent(command.getTransactionId(), Integer.valueOf(1));
                sendMessage(gson.toJson(command));
                boolean unlatched = latch.await(timeoutForOkMessagesMs, TimeUnit.MILLISECONDS);
                latchMap.remove(command.getTransactionId());

                if (!unlatched) {
                    Integer sendCount = retryCountMap.get(command.getTransactionId());
                    if (sendCount == null) {
                        logger.error("Unable to get sendCount, aborting retrying for transaction {}",
                                command.getTransactionId());
                    } else {
                        if (sendCount.intValue() >= retries) {
                            logger.error(
                                    "Unable to send transaction {}, command value was {} : {} for Device: {}, after {} retry attempts",
                                    command.getTransactionId(), command.getItems().get(0).getPayload().getType(),
                                    command.getItems().get(0).getPayload().getValue(),
                                    command.getItems().get(0).getPayload().getDeviceId(), 5);
                            return;
                        }

                        if (!running) {
                            logger.error("Not retrying transactionId {} as we are stopping",
                                    command.getTransactionId());
                            return;

                        }
                        Integer newRetryCount = Integer.valueOf(sendCount.intValue() + 1);
                        logger.error(
                                "Ok message not received for transaction: {}, for Device: {}, retrying again. Retry count {}",
                                command.getTransactionId(),
                                featureMap.get(command.getItems().get(0).getPayload().getFeatureId()), newRetryCount);
                        retryCountMap.put(command.getTransactionId(), newRetryCount);
                        queue.addFirst(command);
                    }
                } else {
                    logger.trace("Ok message processed for transaction:{}", command.getTransactionId());
                }
            }
        } catch (InterruptedException e) {
            logger.error("Command manager threw an exception: {}", e.getMessage());
        }
    }

    public void okMessage(Integer itemid) {
        logger.debug("Ok Response received for sent Command, TransactionID: {}", itemid);
        CountDownLatch latch = latchMap.get(itemid);
        if (latch != null) {
            latch.countDown();
        }
    }

    public synchronized void setRunning(Boolean running) {
        this.running = running;
    }

    public void addDeviceListener(String deviceid, LightwaverfSmartDeviceListener listener) {
        deviceListeners.put(deviceid, listener);
        logger.debug("Added device listener for {}", deviceid);
    }

    public void removeDeviceListener(String deviceid) {
        deviceListeners.remove(deviceid);
    }

    public synchronized void queueCommand(LightwaverfSmartRequest command) {
        try {
            if (running) {
                if (command.getOperation() == null) {
                    logger.debug("Queueing Ping Command");
                    queue.put(command);
                    return;
                }
                command.setSenderId(uuid);
                command.setTransactionId(transactionId);
                command.getItems().get(0).setItemId(transactionId + "");
                if (command.getOperation().equals("authenticate")) {
                    logger.debug("Adding login command to the top of the queue");
                    queue.putFirst(command);
                } else {
                    logger.trace("Adding command to the queue");
                    messages.put(transactionId, command);
                    queue.put(command);
                }
                transactionId++;
            } else {
                logger.info("Message not added to queue as we are shutting down");
            }
        } catch (InterruptedException e) {
            logger.error("Error adding command to queue:{}", e.getMessage());
        }
    }

    public void sendLoginCommand() {
        logger.debug("Sending Login Command");
        LightwaverfSmartRequest command = new LightwaverfSmartRequest(token, this.deviceUuid);
        queueCommand(command);
    }

    public void sendPing() {
        logger.debug("Sending Ping Command");
        LightwaverfSmartRequest command = new LightwaverfSmartRequest();
        queueCommand(command);
    }

    @Override
    public void tokenUpdated(String token) {
        this.token = token;
    }

    @Override
    public void websocketConnected(Boolean connected) {
        this.connected = connected;
        if (this.connected) {
            setRunning(true);
            sendLoginCommand();
        } else {
            setRunning(false);
        }
    }

    @Override
    public void onMessage(String message) {
        logger.trace("Message received: {}", message);
        LightwaverfSmartRequest response = gson.fromJson(message, LightwaverfSmartRequest.class);
        if (response != null) {
            String operation = response.getOperation();
            String direction = response.getDirection();
            String class_ = response.getClass_();

            // if(class_.equals("user") && operation.equals("authenticate") && ) {
            //
            // }

            if (response.getItems().get(0).getError() != null) {
                logger.debug("Websocket response was an error with code {} and message {}",
                        response.getItems().get(0).getError().getCode(),
                        response.getItems().get(0).getError().getMessage());
                return;
            }

            if (response.getError() != null) {
                logger.debug("Websocket response was an error: {}", response.getError());
                return;
            }

            if (class_.equals("user") && operation.equals("authenticate")) {
                Boolean success = response.getItems().get(0).getSuccess();
                logger.debug("Login to server {}", response.getItems().get(0).getPayload().getServerName());
                processLogin(success, response.getTransactionId());
                return;
            }

            if (class_.equals("feature")) {
                String deviceid = "";
                for (int i = 0; i < response.getItems().size(); i++) {
                    Integer transactionid = Integer.valueOf(response.getItems().get(i).getItemId());
                    // Get the deviceid - lots of checking as theres a bug in the returned message from the websocket
                    LightwaverfSmartPayload payload = response.getItems().get(i).getPayload();
                    if (payload != null) {
                        if (payload.getFeature() == null) {
                            if (direction.equals("response")) {
                                LightwaverfSmartRequest request = messages.get(transactionid);
                                if (request != null) {
                                    String featureid = request.getItems().get(i).getPayload().getFeatureId();
                                    if (featureid != null) {
                                        deviceid = featureMap.get(featureid);
                                    } else {
                                        logger.error(
                                                " Cannot process update, unable to get device id from feature map");
                                        return;
                                    }
                                } else {
                                    logger.error(" Cannot process update, message wasnt present");
                                }
                            } else {
                                deviceid = featureMap.get(payload.getFeatureId());
                            }
                        } else {
                            deviceid = payload.getFeature().getDeviceId();
                        }

                        if (deviceid != null) {
                            if (direction.equals("response")) {
                                if (!response.getItems().get(i).getSuccess()) {
                                    proccesUnsuccessfulMessage(class_, transactionid);
                                } else {
                                    logger.debug("Command for transaction {} was sucessful", transactionid);
                                    okMessage(transactionid);
                                    messages.remove(transactionid);
                                    processDeviceEvent(deviceid, payload);
                                }
                            } else {
                                processDeviceEvent(deviceid, payload);
                            }
                        }
                    }
                }
                return;
            }

            if (class_.equals("server") && operation.equals("closing")) {
                logger.info(
                        "Closing existing connection at the server for connectionid {} as it wasnt closed correctly",
                        response.getItems().get(0).getPayload().getConnectionId());
                listener.websocketConnected(false);
                return;
            }

            logger.info("Unhandled websocket message {}", message);
        }
    }

    public void processDeviceEvent(String deviceid, LightwaverfSmartPayload payload) {
        String channelid = (payload.getChannel() + 1) + "#" + payload.getType();
        Long value = payload.getValue();
        String type = payload.getType();
        LightwaverfSmartDeviceListener listener = deviceListeners.get(deviceid);
        if (listener != null) {
            State state = processState(type, value);
            listener.updateChannel(channelid, state);
            return;
        }
        logger.warn("Command response for {} wasnt processed as there is no listener present", deviceid);
    }

    private void proccesUnsuccessfulMessage(String class_, Integer transactionid) {
        logger.info("Command response for transaction {} wasnt successful so we are sending again", transactionid);
        LightwaverfSmartRequest command = messages.get(transactionid);
        if (command != null) {
            messages.remove(transactionid);
            okMessage(transactionid);
            queueCommand(command);
            return;
        }
        logger.info("Unable to re-queue command for transaction {} as it wasnt in the maessage list", transactionid);
    }

    private void processLogin(Boolean success, Integer transactionid) {
        logger.debug("Login {}", success.equals(true) ? "successful" : "failed");
        okMessage(transactionid);
        if (success) {
            this.connected = true;
            listener.websocketLoggedIn();
            return;
        }
        this.connected = false;
        listener.websocketConnected(false);
    }

    private State processState(String channelType, long value) {
        State state = OnOffType.ON;
        switch (channelType) {
            case "switch":
            case "diagnostics":
            case "outletInUse":
            case "protection":
            case "identify":
            case "reset":
            case "upgrade":
            case "heatState":
            case "callForHeat":
            case "bulbSetup":
            case "dimSetup":
            case "valveSetup":
            case "windowPosition":
                if (value == 1) {
                    state = OnOffType.ON;
                } else {
                    state = OnOffType.OFF;
                }
                break;
            case "threeWayRelay":
            case "periodOfBroadcast":
            case "monthArray":
            case "weekdayArray":
                state = new StringType(String.valueOf(value));
                break;
            case "power":
            case "batteryLevel":
            case "rssi":
                state = new DecimalType(value);
                break;
            case "timeZone":
            case "day":
            case "month":
            case "year":
            case "energy":
                state = new DecimalType((value / 1000.0));

                break;
            case "temperature":
            case "targetTemperature":
            case "voltage":
                state = new DecimalType((value / 10.0));
                break;
            case "dimLevel":
            case "valveLevel":
                state = new PercentType((int) value);
                break;

            case "rgbColor":
                Color color = new Color((int) value);
                int red = (color.getRed());
                int green = (color.getGreen());
                int blue = (color.getBlue());
                float[] hsb = Color.RGBtoHSB(red, green, blue, null);
                int hue = (int) Math.round(hsb[0] * 360);
                int saturation = (int) Math.round(hsb[1] * 100);
                int brightness = (int) Math.round(hsb[2] * 100);
                String hsb1 = hue + "," + saturation + "," + brightness;
                state = new HSBType(hsb1);
                break;

            case "date":
                String monthPad = "";
                String dayPad = "";
                String hex = Integer.toHexString((int) value);
                int year = Integer.parseInt(hex.substring(0, 3), 16);
                int month = Integer.parseInt(hex.substring(3, 4), 16);
                int day = Integer.parseInt(hex.substring(4, 6), 16);
                if (month < 10) {
                    monthPad = "0";
                }
                if (day < 10) {
                    dayPad = "0";
                }
                String dateValue = year + "-" + monthPad + month + "-" + dayPad + day + "T00:00:00.000+0000";
                state = new DateTimeType(dateValue);
                break;
            case "currentTime":
                // ZonedDateTime instant = Instant.ofEpochMilli(value * 1000).atZone(ZoneId.systemDefault());
                state = new DateTimeType();
                break;
            case "duskTime":
            case "dawnTime":
            case "time":
                String hoursPad = "";
                String minsPad = "";
                String secsPad = "";
                int minutes = ((((int) value) / 60) % 60);
                int hours = ((int) value / 3600);
                int seconds = (((int) value) % 60);
                if (hours < 10) {
                    hoursPad = "0";
                }
                if (minutes < 10) {
                    minsPad = "0";
                }
                if (seconds < 10) {
                    secsPad = "0";
                }
                LocalDate now = LocalDate.now();

                String timeValue = now + "T" + hoursPad + hours + ":" + minsPad + minutes + ":" + secsPad + seconds;
                state = new DateTimeType(timeValue);
                break;
            case "weekday":
                if (value != 0) {
                    state = new StringType(DayOfWeek.of((int) value).toString());
                    break;
                } else {
                    break;
                }
            case "locationLongitude":
            case "locationLatitude":
                state = new StringType(new DecimalType(value / 1000000.0).toString());
                break;
        }
        return state;
    }

    public Boolean addFeature(String featureid, String deviceid) {
        if (featureMap.containsKey(featureid)) {
            return false;
        }
        featureMap.put(featureid, deviceid);
        return true;
    }

    @Override
    public void websocketLoggedIn() {
        // na
    }

    @Override
    public void sendMessage(String message) {
        listener.sendMessage(message);
    }
}
