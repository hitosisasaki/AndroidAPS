package info.nightscout.androidaps.plugins.DanaR.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.Config;
import info.nightscout.androidaps.plugins.DanaR.DanaRFragment;
import info.nightscout.androidaps.plugins.DanaR.DanaRPump;

/**
 * Created by mike on 05.07.2016.
 */
public class MsgSettingBasal extends DanaRMessage {
    private static Logger log = LoggerFactory.getLogger(MsgSettingBasal.class);

    public MsgSettingBasal() {
        SetCommand(0x3202);
    }

    public void handleMessage(byte[] bytes) {
        DanaRPump pump = DanaRFragment.getDanaRPump();
        if (pump.pumpProfiles == null) pump.pumpProfiles = new double[4][];
        pump.pumpProfiles[pump.activeProfile] = new double[24];
        for (int index = 0; index < 24; index++) {
            int basal = intFromBuff(bytes, 2 * index, 2);
            if (basal < 10) basal = 0;
            pump.pumpProfiles[pump.activeProfile][index] = basal / 100d;
        }

        if (Config.logDanaMessageDetail)
            for (int index = 0; index < 24; index++) {
                log.debug("Basal " + String.format("%02d", index) + "h: " + DanaRFragment.getDanaRPump().pumpProfiles[DanaRFragment.getDanaRPump().activeProfile][index]);
            }
    }
}
