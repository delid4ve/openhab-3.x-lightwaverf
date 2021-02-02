package org.openhab.binding.lightwaverf.internal.smart.queues;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.openhab.binding.lightwaverf.internal.smart.connections.Ws;
import org.openhab.binding.lightwaverf.internal.smart.listeners.WsEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class MessageSender implements Runnable, WsEventListener {

    private final Logger logger = LoggerFactory.getLogger(MessageSender.class);
    private final ConcurrentMap<Integer, CountDownLatch> latchMap = new ConcurrentHashMap<Integer, CountDownLatch>();
    /** Queue of messages to send */
    private final BlockingDeque<WsMessageQueue> queue = new LinkedBlockingDeque<WsMessageQueue>();
    /** Map of Integers so we can count retry attempts. */
    private final ConcurrentMap<Integer, Integer> retryCountMap = new ConcurrentHashMap<Integer, Integer>();
    private final Gson gson;
    private final Ws ws;
    private final int timeoutForOkMessagesMs = 1000;

    /** Boolean to indicate if we are running */
    private boolean running = true;

    public MessageSender(Ws ws, Gson gson) {
        this.gson = gson;
        this.ws = ws;
        // ws.addOkListener(this);
    }

    public synchronized void stopRunning() {
        running = false;
    }

    public void sendCommand(WsMessageQueue command) {
        try {
            if (running) {
                queue.put(command);
            } else {
                logger.info("Message not added to queue as we are shutting down");
            }
        } catch (InterruptedException e) {
            logger.error("Error adding command to queue:{}", e);
        }
    }

    @Override
    public void run() {
        try {
            WsMessageQueue command = queue.take();
            CountDownLatch latch = new CountDownLatch(1);
            latchMap.putIfAbsent(command.getTransactionid(), latch);
            retryCountMap.putIfAbsent(command.getTransactionid(), Integer.valueOf(1));
            ws.send(gson.toJson(command.getInitialStatus()));
            boolean unlatched = latch.await(timeoutForOkMessagesMs, TimeUnit.MILLISECONDS);
            latchMap.remove(command.getTransactionid());

            if (!unlatched) {
                Integer sendCount = retryCountMap.get(command.getTransactionid());
                if (sendCount.intValue() >= 5) {
                    logger.error(
                            "Unable to send transaction {}, command was {} : {} for Device: {}, after {} retry attempts",
                            command.getTransactionid(),
                            command.getInitialStatus().getItems().get(0).getPayload().getType(),
                            command.getInitialStatus().getItems().get(0).getPayload().getValue(),
                            command.getInitialStatus().getItems().get(0).getPayload().getDeviceId(), 5);
                    return;
                }
                if (!running) {
                    logger.error("Not retrying transactionId {} as we are stopping", command.getTransactionid());
                    return;

                }
                Integer newRetryCount = Integer.valueOf(sendCount.intValue() + 1);
                logger.info(
                        "Ok message not received for transaction: {}, command was {} : {} for Device: {}, retrying again. Retry count {}",
                        command.getTransactionid(), command.getInitialStatus().getItems().get(0).getPayload().getType(),
                        command.getInitialStatus().getItems().get(0).getPayload().getValue(),
                        command.getInitialStatus().getItems().get(0).getPayload().getDeviceId(), newRetryCount);
                retryCountMap.put(command.getTransactionid(), newRetryCount);
                queue.addFirst(command);
            } else {
                logger.info("Ok message processed for transaction:{}", command.getTransactionid());
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void updateConnectionState(boolean connected) {
        // not applicable
    }

    @Override
    public void onMessage(String message) {
        CountDownLatch latch = latchMap.get(Integer.parseInt(message));
        if (latch != null) {
            latch.countDown();
        }
    }
}
