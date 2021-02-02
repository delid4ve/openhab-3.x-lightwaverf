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
import org.openhab.binding.lightwaverf.internal.connect.config.SwitchConfig;
import org.openhab.binding.lightwaverf.internal.connect.dto.Response;
import org.openhab.binding.lightwaverf.internal.connect.listeners.MessageListener;
import org.openhab.binding.lightwaverf.internal.connect.utilities.LWCommand;
import org.openhab.binding.lightwaverf.internal.connect.utilities.LWType;
import org.openhab.binding.lightwaverf.internal.connect.utilities.MessageConvertor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwitchHandler extends BaseThingHandler implements MessageListener {

    private SwitchConfig config;
    private @Nullable BridgeHandler account;
    private static Logger logger = LoggerFactory.getLogger(TRVHandler.class);
    private final MessageConvertor converter = new MessageConvertor();

    public SwitchHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        Bridge bridge = getBridge();
        account = (BridgeHandler) bridge.getHandler();
        config = this.getConfigAs(SwitchConfig.class);
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
                case "switch":
                    translation = command.toString().equalsIgnoreCase("on") ? "1" : "0";
                    commandToSend = ",!" + "R" + config.roomId + "D" + config.deviceId + "F" + translation;
                    break;
            }
            LWCommand message = converter.convertToLightwaveRfMessage(config.roomId, config.deviceId, LWType.DIMMER,
                    command);
            logger.info("Sending Command:{} from device:{}", commandToSend, config.deviceId);
            account.sendUDP(message);
        }
    }

    private void updateState(Response message) {
        ChannelUID channelUID;
        String function = message.getFn();
        switch (function) {
            case "on":
            case "off":
                channelUID = this.thing.getChannel("switch").getUID();
                updateState(channelUID, (function.equals("on") ? OnOffType.ON : OnOffType.OFF));
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
