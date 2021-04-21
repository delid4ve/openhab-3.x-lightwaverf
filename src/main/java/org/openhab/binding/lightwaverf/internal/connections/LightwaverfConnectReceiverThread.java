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
import java.net.SocketException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lightwaverf.internal.listeners.LightwaverfConnectStringMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LightwaverfConnectReceiverThread} class handles communication received
 * from the lightwave link
 * /**
 * 
 * @author Neil Renaud - Initial contribution
 * @author David Murton - Since OH 2.x
 * 
 */
@NonNullByDefault
public class LightwaverfConnectReceiverThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(LightwaverfConnectReceiverThread.class);
    private final CopyOnWriteArrayList<LightwaverfConnectStringMessageListener> listeners = new CopyOnWriteArrayList<LightwaverfConnectStringMessageListener>();
    private boolean running = false;
    protected byte[] data = new byte[1024];
    protected @Nullable DatagramSocket socket;

    public LightwaverfConnectReceiverThread(LightwaverfConnectStringMessageListener listener) {
        listeners.add(listener);
    }

    public synchronized void stopReceiver(LightwaverfConnectStringMessageListener listener) {
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            socket.close();
            running = false;
            socket = null;
        }
    }

    /**
     * Run method, this will listen to the socket and receive messages. The
     * blocking is stopped when the socket is closed.
     */
    @Override
    public void run() {
        running = true;
        try {
            socket = new DatagramSocket(9761);
            logger.info("Receiver thread running");
            while (running) {
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                String[] message = new String[2];
                message[0] = packet.getAddress().toString().substring(1);
                message[1] = new String(packet.getData(), 0, packet.getLength());
                logger.debug("Message received from: {}, message: {}", message[0], message[1]);
                processMessage(message);
            }
        } catch (SocketException e) {
            logger.debug("Receive Thread Socket exception:{}", e.getMessage());
            running = false;
        } catch (IOException e) {
            logger.debug("Receive Thread IO exception:{}", e.getMessage());
            running = false;
        }
    }

    private void processMessage(String[] message) {
        for (LightwaverfConnectStringMessageListener listener : listeners) {
            if (message[1].startsWith("*!{")) {
                listener.jsonReceived(message[1]);
            } else {
                listener.messageReceived(message[1]);
            }
        }
    }
}
