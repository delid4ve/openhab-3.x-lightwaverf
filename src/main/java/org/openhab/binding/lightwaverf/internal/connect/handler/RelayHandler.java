package org.openhab.binding.lightwaverf.internal.connect.handler;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.*;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.lightwaverf.internal.connect.commands.CommandOk;
import org.openhab.binding.lightwaverf.internal.connect.commands.HeatInfoRequest;
import org.openhab.binding.lightwaverf.internal.connect.commands.VersionMessage;
import org.openhab.binding.lightwaverf.internal.connect.config.RelayConfig;
import org.openhab.binding.lightwaverf.internal.connect.dto.Response;
import org.openhab.binding.lightwaverf.internal.connect.listeners.MessageListener;
import org.openhab.binding.lightwaverf.internal.connect.utilities.LWCommand;
import org.openhab.binding.lightwaverf.internal.connect.utilities.LWType;
import org.openhab.binding.lightwaverf.internal.connect.utilities.MessageConvertor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelayHandler extends BaseThingHandler implements MessageListener {

    private RelayConfig config;
    private @Nullable BridgeHandler account;
    private static Logger logger = LoggerFactory.getLogger(TRVHandler.class);
    private final MessageConvertor converter = new MessageConvertor();

    public RelayHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        Bridge bridge = getBridge();
        account = (BridgeHandler) bridge.getHandler();
        config = this.getConfigAs(RelayConfig.class);
        account.registerRoom(config.roomId, this);
        account.registerDevice(config.deviceId, this);
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        account.unregisterRoom(this);
        account.unregisterDevice(this);
        config = null;
        account = null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String commandToSend = "";
        String translation = "";
        if (command instanceof RefreshType) {
            return;
        } else {
            switch (channelUID.getId()) {
                case "relayPosition":
                    if (command.toString().equalsIgnoreCase("0")) {
                        translation = "F^";
                    } else if (command.toString().equalsIgnoreCase("-1")) {
                        translation = "F)";
                    } else if (command.toString().equalsIgnoreCase("1")) {
                        translation = "F(";
                    }
                    commandToSend = ",!" + "R" + config.roomId + "D" + config.deviceId + "F" + translation;
                    break;
                case "switch":
                    translation = command.toString().equalsIgnoreCase("on") ? "1" : "0";
                    commandToSend = ",!" + "R" + config.roomId + "D" + config.deviceId + "F" + translation;
                    break;
            }
            LWCommand message = converter.convertToLightwaveRfMessage(config.roomId, config.deviceId, LWType.RELAY,
                    command);
            logger.info("Sending Command:{} from device:{}", commandToSend, config.deviceId);
            account.sendUDP(message);
        }
    }

    private void updateState(Response message) {
        ChannelUID channelUID;
        String function = message.getFn();
        Integer command = 0;
        switch (function) {
            case "on":
            case "off":
                channelUID = this.thing.getChannel("switch").getUID();
                updateState(channelUID, (function.equals("on") ? OnOffType.ON : OnOffType.OFF));
                break;
            case "open":
            case "close":
            case "stop":
                channelUID = this.thing.getChannel("relayPostion").getUID();
                if (function.equals("open")) {
                    command = 1;
                } else if (function.equals("close")) {
                    command = -1;
                } else if (function.equals("stop")) {
                    command = 0;
                }
                updateState(channelUID, new DecimalType(command));
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
