package org.openhab.binding.lightwaverf.internal.connect.handler;

import org.eclipse.jdt.annotation.Nullable;
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
import org.openhab.binding.lightwaverf.internal.connect.config.TRVConfig;
import org.openhab.binding.lightwaverf.internal.connect.dto.Response;
import org.openhab.binding.lightwaverf.internal.connect.listeners.MessageListener;
import org.openhab.binding.lightwaverf.internal.connect.utilities.LWCommand;
import org.openhab.binding.lightwaverf.internal.connect.utilities.LWType;
import org.openhab.binding.lightwaverf.internal.connect.utilities.MessageConvertor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TRVHandler extends BaseThingHandler implements MessageListener {

    private TRVConfig config;
    private @Nullable BridgeHandler account;
    private static Logger logger = LoggerFactory.getLogger(TRVHandler.class);
    private final MessageConvertor converter = new MessageConvertor();

    public TRVHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        Bridge bridge = getBridge();
        account = (BridgeHandler) bridge.getHandler();
        config = this.getConfigAs(TRVConfig.class);
        account.registerRoom(config.roomId, this);
        account.registerSerial(config.serialNo, this);
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        account.unregisterRoom(this);
        account.unregisterSerial(this);
        config = null;
        account = null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            return;
        } else {
            switch (channelUID.getId()) {
                case "targetTemperature":
                    String commandToSend = ",!" + "R" + config.roomId + "F*t" + "P" + command.toString();
                    LWCommand message = converter.convertToLightwaveRfMessage(config.serialNo, config.roomId,
                            LWType.HEATING_SET_TEMP, command);
                    logger.info("Sending Command:{} from device:{}", commandToSend, config.serialNo);
                    account.sendUDP(message);
                    break;
            }
        }
    }

    @Override
    public void roomDeviceMessageReceived(Response message) {
        // Nothing to do here as TRV
    }

    @Override
    public void roomMessageReceived(Response message) {
        logger.info("Room Message Received: {}", message.getCmd());
        logger.info("Room Message Received: {}", message.getRoom());
        logger.info("Room Message Received: {}", message.getFn());
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
