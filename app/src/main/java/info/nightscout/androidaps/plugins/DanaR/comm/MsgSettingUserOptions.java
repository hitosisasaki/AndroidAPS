package info.nightscout.androidaps.plugins.DanaR.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mike on 05.07.2016.
 */
public class MsgSettingUserOptions extends DanaRMessage {
    private static Logger log = LoggerFactory.getLogger(MsgSettingShippingInfo.class);

    public MsgSettingUserOptions() {
        SetCommand(0x320B);
    }

    public void handleMessage(byte[] bytes) {

    }

}
