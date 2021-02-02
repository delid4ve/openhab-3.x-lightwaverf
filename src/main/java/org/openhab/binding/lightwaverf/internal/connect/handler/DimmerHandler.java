package org.openhab.binding.lightwaverf.internal.connect.handler;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.*;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.lightwaverf.internal.connect.commands.CommandOk;
import org.openhab.binding.lightwaverf.internal.connect.commands.HeatInfoRequest;
import org.openhab.binding.lightwaverf.internal.connect.commands.VersionMessage;
import org.openhab.binding.lightwaverf.internal.connect.config.DimmerConfig;
import org.openhab.binding.lightwaverf.internal.connect.dto.Response;
import org.openhab.binding.lightwaverf.internal.connect.listeners.MessageListener;
import org.openhab.binding.lightwaverf.internal.connect.utilities.LWCommand;
import org.openhab.binding.lightwaverf.internal.connect.utilities.LWType;
import org.openhab.binding.lightwaverf.internal.connect.utilities.MessageConvertor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DimmerHandler extends BaseThingHandler implements MessageListener {

    private @Nullable DimmerConfig config;
    private @Nullable BridgeHandler account;
    private static Logger logger = LoggerFactory.getLogger(DimmerHandler.class);
    private final MessageConvertor converter = new MessageConvertor();

    public DimmerHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        Bridge bridge = getBridge();
        account = (BridgeHandler) bridge.getHandler();
        config = this.getConfigAs(DimmerConfig.class);
        logger.info("Device configured with RoomID:{} and DeviceID:{}", config.deviceid, config.roomid);
        account.registerRoom(config.roomid, this);
        logger.info("Registering Room Listener for ID:{}", config.roomid);
        account.registerDevice(config.deviceid, this);
        logger.info("Registering Device Listener for ID:{}", config.deviceid);
        if (bridge.getStatus() == ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void dispose() {
        account.unregisterRoom(this);
        account.unregisterDevice(this);
        config = null;
        account = null;
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (config.deviceid.isEmpty() || config.roomid.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "One or more IDs not set");
            return;
        }
        if (bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            account.unregisterDevice(this);
            account.unregisterRoom(this);
            return;
        }
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            thingUpdated(this.thing);
            return;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        LWCommand message;
        if (command instanceof RefreshType) {
            return;
        } else {
            switch (channelUID.getId()) {
                case "dimLevel":
                    logger.info("openhab command:{}", command.toString());
                    message = converter.convertToLightwaveRfMessage(config.roomid, config.deviceid, LWType.DIMMER,
                            command);
                    logger.info("Sending Command:{} from device:{}", message.getCommandString(), config.deviceid);
                    account.sendUDP(message);
                    // if (command.toString().equals("OFF") || command.toString().equals("0")) {
                    // commandToSend = ",!" + "R" + config.roomid + "D" + config.deviceid + "F0";
                    // } else {
                    // dimLevel = (int) Math.round(((32 / 100) * (Integer.parseInt(command.toString()))));
                    // }
                    // commandToSend = ",!" + "R" + config.roomid + "D" + config.deviceid + "FdP" + dimLevel;
                    break;
            }
        }
    }

    private void updateState(Response message) {
        ChannelUID channelUID;
        String function = message.getFn();
        switch (function) {
            case "dim":
                Integer dimLevel = ((100 / 32) * message.getParam());
                logger.info("Dimmer State to process {}", dimLevel);
                channelUID = this.thing.getChannel("dimLevel").getUID();
                updateState(channelUID, new PercentType(dimLevel));
                break;
        }
    }

    @Override
    public void roomDeviceMessageReceived(Response message) {
        updateState(message);
    }

    @Override
    public void roomMessageReceived(Response message) {
    }

    @Override
    public void serialMessageReceived(Response message) {
        logger.info("Serial Message Received: {}", message.getCmd());
    }

    @Override
    public void okMessageReceived(CommandOk message) {
        // TODO Auto-generated method stub
    }

    @Override
    public void versionMessageReceived(VersionMessage message) {
        // TODO Auto-generated method stub
    }

    @Override
    public void heatInfoMessageReceived(HeatInfoRequest command) {
        // TODO Auto-generated method stub
    }
}
