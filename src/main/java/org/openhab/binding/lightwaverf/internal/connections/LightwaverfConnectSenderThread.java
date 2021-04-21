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
package org.openhab.binding.lightwaverf.internal.connections;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lightwaverf.internal.commands.CommandOk;
import org.openhab.binding.lightwaverf.internal.commands.HeatInfoRequest;
import org.openhab.binding.lightwaverf.internal.commands.VersionMessage;
import org.openhab.binding.lightwaverf.internal.dto.LightwaverfConnectResponse;
import org.openhab.binding.lightwaverf.internal.listeners.LightwaverfConnectMessageListener;
import org.openhab.binding.lightwaverf.internal.utilities.LWCommand;
import org.openhab.binding.lightwaverf.internal.utilities.MessageId;
import org.openhab.binding.lightwaverf.internal.utilities.RegistrationMessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for transmitting LightwaveRF commands on to the
 * network to the LightwaveRF link which will then act upon these commands.
 *
 * This class will also listen for the responses from the wifi link and notify
 * listeners of these responses.
 *
 * This class implements retry logic in case it doesn't get an "OK" response
 * from the LightwaveRF wifi link. However please note that due to the way the
 * wifi link works it may receive a command but may not actually action it. In
 * particular this happens when messages are sent to close together. As such we
 * also implement a delay between each message we send.
 * /**
 * 
 * @author Neil Renaud - Initial contribution
 * @author David Murton - Since OH 2.x
 * 
 */
@NonNullByDefault
public class LightwaverfConnectSenderThread implements Runnable, LightwaverfConnectMessageListener {
    // implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(LightwaverfConnectSenderThread.class);
    /** Message added to the send queue to indicate that we need to shutdown */
    private static final LWCommand STOP_MESSAGE = LWCommand.STOP_MESSAGE;
    private static final Integer ONE = 1;

    private static final int MAX_RETRY_ATTEMPS = 5;

    /**
     * Map of CountDownLatches, used to notify when we have received an ok for one
     * of our messages
     */
    private final ConcurrentMap<MessageId, CountDownLatch> latchMap = new ConcurrentHashMap<MessageId, CountDownLatch>();
    /** Queue of messages to send */
    private final BlockingDeque<LWCommand> queue = new LinkedBlockingDeque<LWCommand>();
    /** Map of Integers so we can count retry attempts. */
    private final ConcurrentMap<MessageId, Integer> retryCountMap = new ConcurrentHashMap<MessageId, Integer>();

    /**
     * Timeout for OK Messages - if we don't receive an ok in this time we will
     * re-send. Set as short as you can without missing replies
     */
    private final int timeoutForOkMessagesMs;
    /** LightwaveRF WIFI hub port. */
    // private final int lightwaveWifiLinkTransmitPort;
    /** LightwaveRF WIFI hub IP Address or broadcast address to send messages to */
    // private InetAddress ipAddress;
    private String ipaddress = "";
    private @Nullable InetAddress address;
    /** Socket to transmit messages */
    protected @Nullable DatagramSocket socket;

    /** Boolean to indicate if we are running */
    private boolean running = true;

    // private Integer transactionId = 100;

    public LightwaverfConnectSenderThread(String ipaddress, int timeoutForOkMessagesMs) {
        this.timeoutForOkMessagesMs = timeoutForOkMessagesMs;
        this.ipaddress = ipaddress;
    }

    /**
     * Stop the LightwaveRFReseiver Will set running to false, add a stop message to
     * the queue so that it stops when empty, close and set the socket to null
     */
    public synchronized void stopSender() {
        sendCommand(STOP_MESSAGE);
        socket.close();
        running = false;
        socket = null;
    }

    /**
     * Run thread, pulling off any items from the UDP commands buffer, then send
     * across network
     */

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(9760);
            LWCommand commandToSend = queue.take();
            MessageId messageid = commandToSend.getMessageId();
            if (messageid != null) {
                if (!commandToSend.equals(STOP_MESSAGE)) {
                    CountDownLatch latch = new CountDownLatch(1);
                    latchMap.putIfAbsent(messageid, latch);
                    retryCountMap.putIfAbsent(messageid, ONE);
                    netsendUDP(commandToSend);
                    boolean unlatched = latch.await(timeoutForOkMessagesMs, TimeUnit.MILLISECONDS);
                    // logger.info("Unlatched?" + unlatched);

                    latchMap.remove(messageid);
                    if (!unlatched) {
                        Integer sendCount = retryCountMap.get(messageid);
                        if (sendCount.intValue() >= MAX_RETRY_ATTEMPS) {
                            logger.error("Unable to send message {} after {} attempts giving up",
                                    commandToSend.getCommandString(), MAX_RETRY_ATTEMPS);
                            return;
                        }
                        if (!running) {
                            logger.error("Not retrying message {} as we are stopping",
                                    commandToSend.getCommandString());
                            return;

                        }
                        Integer newRetryCount = Integer.valueOf(sendCount.intValue() + 1);
                        logger.info("Ok message not received for {}, retrying again. Retry count {}",
                                commandToSend.getCommandString(), newRetryCount);
                        retryCountMap.put(messageid, newRetryCount);
                        queue.addFirst(commandToSend);
                    } else {
                        logger.info("Ok message received for {}", commandToSend.getCommandString());
                    }
                }

            } else {
                logger.info("Stop message received");
            }
        } catch (InterruptedException | SocketException e) {
            logger.error("Error waiting on queue: {}", e.getMessage());
        }
    }

    /**
     * Add LightwaveRFCommand command to queue to send.
     */
    public void sendCommand(LWCommand command) {
        try {
            if (running) {
                logger.info("added command to queue: {}", command.getCommandString());
                queue.put(command);
            } else {
                logger.info("Message not added to queue as we are shutting down, Message:{}",
                        command.getCommandString());
            }
        } catch (InterruptedException e) {
            logger.error("Error adding command[{}] to queue Throwable {}", command, e.getMessage());
        }
    }

    @Override
    public void okMessageReceived(CommandOk command) {
        CountDownLatch latch = latchMap.get(command.getMessageId());
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public void roomDeviceMessageReceived(LightwaverfConnectResponse message) {
        /*
         * Do Nothing
         */
    }

    @Override
    public void roomMessageReceived(LightwaverfConnectResponse message) {
        /* Do Nothing */
    }

    @Override
    public void serialMessageReceived(LightwaverfConnectResponse message) {
        /* Do Nothing */
    }

    @Override
    public void versionMessageReceived(VersionMessage message) {
        // If we receive a vesion message we assume this is in response to our
        // registration message and attempt to unlatch the sending thread.
        CountDownLatch latch = latchMap.get(new RegistrationMessageId());
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public void heatInfoMessageReceived(HeatInfoRequest command) {
        /* Do Nothing */
    }

    /**
     * Send the UDP message
     */
    private void netsendUDP(LWCommand command) {
        try {
            address = InetAddress.getByName(ipaddress);
            logger.debug("Sending command[{}]", command.getCommandString());
            byte[] sendData = new byte[1024];
            sendData = command.getCommandString().getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, 9760);
            socket.send(sendPacket);
        } catch (IOException e) {
            logger.error("Error sending command {}. Throwable {}", command, e.getMessage());
        }
    }
}
