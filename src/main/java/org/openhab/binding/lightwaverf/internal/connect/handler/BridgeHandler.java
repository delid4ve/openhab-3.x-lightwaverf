package org.openhab.binding.lightwaverf.internal.connect.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.lightwaverf.internal.connect.commands.CommandOk;
import org.openhab.binding.lightwaverf.internal.connect.commands.RegistrationCommand;
import org.openhab.binding.lightwaverf.internal.connect.config.BridgeConfig;
import org.openhab.binding.lightwaverf.internal.connect.connections.ReceiverThread;
import org.openhab.binding.lightwaverf.internal.connect.connections.SenderThread;
import org.openhab.binding.lightwaverf.internal.connect.dto.Response;
import org.openhab.binding.lightwaverf.internal.connect.exception.MessageException;
import org.openhab.binding.lightwaverf.internal.connect.listeners.MessageListener;
import org.openhab.binding.lightwaverf.internal.connect.listeners.StringMessageListener;
import org.openhab.binding.lightwaverf.internal.connect.utilities.LWCommand;
import org.openhab.binding.lightwaverf.internal.connect.utilities.MessageConvertor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class BridgeHandler extends BaseBridgeHandler implements StringMessageListener {

    // private final Integer POLLING_TIME = 2000;
    private final Integer TIMEOUT_FOR_OK_MESSAGES_MS = 500;
    // Connections
    private ReceiverThread receiverThread;
    private SenderThread senderThread;
    // Listeners
    private Map<MessageListener, String> devices = new HashMap<>();
    private Map<MessageListener, String> rooms = new HashMap<>();
    private Map<MessageListener, String> serials = new HashMap<>();
    private final MessageConvertor converter = new MessageConvertor();

    // Other
    private static final Logger logger = LoggerFactory.getLogger(BridgeHandler.class);
    private BridgeConfig config;
    private Gson gson = new Gson();
    // private HeatPoller heatPoller;

    public BridgeHandler(Bridge thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        config = this.getConfigAs(BridgeConfig.class);
        receiverThread = new ReceiverThread(this);
        scheduler.schedule(receiverThread, 5, TimeUnit.SECONDS);
        senderThread = new SenderThread(config.ipaddress, TIMEOUT_FOR_OK_MESSAGES_MS);
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
        } catch (MessageException e) {
            logger.error("Error converting message: " + message);
        }
    }

    private void notifyOkListners(CommandOk message) {
        MessageListener ok = senderThread;
        ok.okMessageReceived(message);
    }

    @Override
    public void jsonReceived(String message) {
        Response response = gson.fromJson(message.substring(2), Response.class);
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

    private void updateBridge(Response response) {
        updateState(this.thing.getChannel("time").getUID(), new DateTimeType(response.getTime().toString()));
        updateState(this.thing.getChannel("timeZone").getUID(), new DecimalType(response.getTimezone().toString()));
        updateState(this.thing.getChannel("locationLongitude").getUID(),
                new DecimalType(response.get_long().toString()));
        updateState(this.thing.getChannel("locationLatitude").getUID(), new DecimalType(response.getLat().toString()));
        updateState(this.thing.getChannel("upTime").getUID(), new DateTimeType(response.getUptime().toString()));
    }

    private void notifyDevice(Response response) {
        logger.info("room device message received on bridge:{}", response.getFn());
        for (Map.Entry<MessageListener, String> deviceEntry : devices.entrySet()) {
            if (deviceEntry.getValue().equals(response.getDev().toString())) {
                MessageListener listener = deviceEntry.getKey();
                listener.roomDeviceMessageReceived(response);
                logger.info("Device Message received for id:{}, notifying listener: {}", response.getDev(),
                        deviceEntry.getKey());
            }
        }
    }

    private void notifyRoom(Response response) {
        logger.info("room message received on bridge:{}", response.getCmd());
        for (Map.Entry<MessageListener, String> roomEntry : rooms.entrySet()) {
            if (roomEntry.getValue().equals(response.getRoom().toString())) {
                MessageListener listener = roomEntry.getKey();
                listener.roomMessageReceived(response);
                logger.info("Room Message received for id:{}, notifying listener: {}", response.getRoom(),
                        roomEntry.getKey());
            }
        }
    }

    public void registerRoom(String roomid, MessageListener listener) {
        rooms.put(listener, roomid);
        // heatPoller.addRoomToPoll(itemName, roomid);
    }

    public void unregisterRoom(MessageListener listener) {
        rooms.remove(listener);
        // heatPoller.removeRoomToPoll(itemName);
    }

    public void registerSerial(String serial, MessageListener listener) {
        serials.put(listener, serial);
    }

    public void unregisterSerial(MessageListener listener) {
        serials.remove(listener);
    }

    public void registerDevice(String deviceid, MessageListener listener) {
        devices.put(listener, deviceid);
    }

    public void unregisterDevice(MessageListener listener) {
        devices.remove(listener);
    }
}
