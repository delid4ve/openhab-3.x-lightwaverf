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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lightwaverf.internal.LightwaverfConnectMessageException;
import org.openhab.binding.lightwaverf.internal.commands.CommandOk;
import org.openhab.binding.lightwaverf.internal.commands.RegistrationCommand;
import org.openhab.binding.lightwaverf.internal.config.LightwaverfConnectAccountConfig;
import org.openhab.binding.lightwaverf.internal.connections.LightwaverfConnectReceiverThread;
import org.openhab.binding.lightwaverf.internal.connections.LightwaverfConnectSenderThread;
import org.openhab.binding.lightwaverf.internal.dto.LightwaverfConnectResponse;
import org.openhab.binding.lightwaverf.internal.listeners.LightwaverfConnectMessageListener;
import org.openhab.binding.lightwaverf.internal.listeners.LightwaverfConnectStringMessageListener;
import org.openhab.binding.lightwaverf.internal.utilities.LWCommand;
import org.openhab.binding.lightwaverf.internal.utilities.MessageConvertor;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * 
 * @author David Murton - Initial contribution
 * 
 */
@NonNullByDefault
public class LightwaverfConnectAccountHandler extends BaseBridgeHandler
        implements LightwaverfConnectStringMessageListener {

    // private final Integer POLLING_TIME = 2000;
    private final Integer TIMEOUT_FOR_OK_MESSAGES_MS = 500;
    // Connections
    private @Nullable LightwaverfConnectReceiverThread receiverThread;
    private @Nullable LightwaverfConnectSenderThread senderThread;
    // Listeners
    private Map<LightwaverfConnectMessageListener, String> devices = new HashMap<>();
    private Map<LightwaverfConnectMessageListener, String> rooms = new HashMap<>();
    private Map<LightwaverfConnectMessageListener, String> serials = new HashMap<>();
    private final MessageConvertor converter = new MessageConvertor();

    // Other
    private static final Logger logger = LoggerFactory.getLogger(LightwaverfConnectAccountHandler.class);
    private @Nullable LightwaverfConnectAccountConfig config;
    private Gson gson = new Gson();
    // private HeatPoller heatPoller;

    public LightwaverfConnectAccountHandler(Bridge thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        config = this.getConfigAs(LightwaverfConnectAccountConfig.class);
        receiverThread = new LightwaverfConnectReceiverThread(this);
        scheduler.schedule(receiverThread, 5, TimeUnit.SECONDS);
        senderThread = new LightwaverfConnectSenderThread(config.ipaddress, TIMEOUT_FOR_OK_MESSAGES_MS);
        scheduler.schedule(senderThread, 5, TimeUnit.SECONDS);
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        receiverThread.stopReceiver(this);
        receiverThread = null;
        senderThread.stopSender();
        senderThread = null;
    }

    @Override
    public void thingUpdated(Thing thing) {
        dispose();
        this.thing = thing;
        initialize();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO Auto-generated method stub
    }

    public void sendUDP(LWCommand command) {
        senderThread.sendCommand(command);
    }

    @Override
    public void messageReceived(String message) {
        logger.info("Message received on bridge");
        try {
            LWCommand command = converter.convertFromLightwaveRfMessage(message);
            switch (command.getMessageType()) {
                case OK:
                    notifyOkListners((CommandOk) command);
                    logger.info("Bridge notifying ok listeners");
                    break;
                case ROOM_DEVICE:
                    logger.info("Bridge received Device Message");
                    // handled by json
                    break;
                case ROOM:
                    logger.info("Bridge received Room Message");
                    // handled by json
                    break;
                case HEAT_REQUEST:
                    // notifyHeatRequest((HeatInfoRequest) command);
                    break;
                case SERIAL:
                    // notifySerialListners((SerialMessage) command);
                    break;
                case VERSION:
                    // notifyVersionListners((VersionMessage) command);
                    break;
                case NOT_PROCESSED:
                default:
                    // Do nothing
                    break;
            }
        } catch (LightwaverfConnectMessageException e) {
            logger.error("Error converting message:{} ", message);
        }
    }

    private void notifyOkListners(CommandOk message) {
        LightwaverfConnectMessageListener ok = senderThread;
        ok.okMessageReceived(message);
    }

    @Override
    public void jsonReceived(String message) {
        LightwaverfConnectResponse response = gson.fromJson(message.substring(2), LightwaverfConnectResponse.class);
        logger.warn("Binding received some json: {}", response);
        if (response.getPkt().equals("system") && response.getFn().equals("hubCall")) {
            updateBridge(response);
        } else if (response.getPkt().equals("error")) {
            logger.warn("Lightwave Link error received: {}", response.getPayload());
            if (response.getPayload().contains("Not yet registered. Send !F*p to register")) {
                logger.info(
                        "A Message was sent but the link isnt registered - you now have 12 seconds to press the pairing button");
                sendUDP((LWCommand) new RegistrationCommand());
            }
        } else if (response.getRoom() != null && response.getDev() != null) {
            notifyDevice(response);
        } else if (response.getDev() != null && response.getRoom() == null) {
            notifyRoom(response);
        } else {
            logger.info("Message received but its not in the binding, please send to @delid4ve to get added: {}",
                    response);
        }
    }

    private void updateBridge(LightwaverfConnectResponse response) {
        updateState(this.thing.getChannel("time").getUID(), new DateTimeType(response.getTime().toString()));
        updateState(this.thing.getChannel("timeZone").getUID(), new DecimalType(response.getTimezone().toString()));
        updateState(this.thing.getChannel("locationLongitude").getUID(),
                new DecimalType(response.get_long().toString()));
        updateState(this.thing.getChannel("locationLatitude").getUID(), new DecimalType(response.getLat().toString()));
        updateState(this.thing.getChannel("upTime").getUID(), new DateTimeType(response.getUptime().toString()));
    }

    private void notifyDevice(LightwaverfConnectResponse response) {
        logger.info("room device message received on bridge:{}", response.getFn());
        for (Map.Entry<LightwaverfConnectMessageListener, String> deviceEntry : devices.entrySet()) {
            if (deviceEntry.getValue().equals(response.getDev().toString())) {
                LightwaverfConnectMessageListener listener = deviceEntry.getKey();
                listener.roomDeviceMessageReceived(response);
                logger.info("Device Message received for id:{}, notifying listener: {}", response.getDev(),
                        deviceEntry.getKey());
            }
        }
    }

    private void notifyRoom(LightwaverfConnectResponse response) {
        logger.info("room message received on bridge:{}", response.getCmd());
        for (Map.Entry<LightwaverfConnectMessageListener, String> roomEntry : rooms.entrySet()) {
            if (roomEntry.getValue().equals(response.getRoom().toString())) {
                LightwaverfConnectMessageListener listener = roomEntry.getKey();
                listener.roomMessageReceived(response);
                logger.info("Room Message received for id:{}, notifying listener: {}", response.getRoom(),
                        roomEntry.getKey());
            }
        }
    }

    public void registerRoom(String roomid, LightwaverfConnectMessageListener listener) {
        rooms.put(listener, roomid);
        // heatPoller.addRoomToPoll(itemName, roomid);
    }

    public void unregisterRoom(LightwaverfConnectMessageListener listener) {
        rooms.remove(listener);
        // heatPoller.removeRoomToPoll(itemName);
    }

    public void registerSerial(String serial, LightwaverfConnectMessageListener listener) {
        serials.put(listener, serial);
    }

    public void unregisterSerial(LightwaverfConnectMessageListener listener) {
        serials.remove(listener);
    }

    public void registerDevice(String deviceid, LightwaverfConnectMessageListener listener) {
        devices.put(listener, deviceid);
    }

    public void unregisterDevice(LightwaverfConnectMessageListener listener) {
        devices.remove(listener);
    }
}
