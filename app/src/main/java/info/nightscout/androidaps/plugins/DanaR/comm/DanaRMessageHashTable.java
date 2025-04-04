package info.nightscout.androidaps.plugins.DanaR.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Created by mike on 28.05.2016.
 */
public class DanaRMessageHashTable {
    private static Logger log = LoggerFactory.getLogger(DanaRMessageHashTable.class);

    public static HashMap<Integer, DanaRMessage> messages = null;

    static {
        if (messages == null) {
            messages = new HashMap<Integer, DanaRMessage>();
            put(new MsgBolusStop());                 // 0x0101 CMD_MEALINS_STOP
            put(new MsgBolusStart());                // 0x0102 CMD_MEALINS_START_DATA
            put(new MsgBolusProgress());             // 0x0202 CMD_PUMP_THIS_REMAINDER_MEAL_INS
            put(new MsgStatusProfile());             // 0x0204 CMD_PUMP_CALCULATION_SETTING
            put(new MsgStatusTempBasal());           // 0x0205 CMD_PUMP_EXERCISE_MODE
            put(new MsgStatusBolusExtended());       // 0x0207 CMD_PUMP_EXPANS_INS_I
            put(new MsgStatusBasic());               // 0x020A CMD_PUMP_INITVIEW_I
            put(new MsgStatus());                    // 0x020B CMD_PUMP_STATUS
            put(new MsgInitConnStatusTime());        // 0x0301 CMD_PUMPINIT_TIME_INFO
            put(new MsgInitConnStatusBolus());       // 0x0302 CMD_PUMPINIT_BOLUS_INFO
            put(new MsgInitConnStatusBasic());       // 0x0303 CMD_PUMPINIT_INIT_INFO
            put(new MsgInitConnStatusOption());      // 0x0304 CMD_PUMPINIT_OPTION
            put(new MsgSetTempBasalStart());         // 0x0401 CMD_PUMPSET_EXERCISE_S
            put(new MsgSetCarbsEntry());             // 0x0402 CMD_PUMPSET_HIS_S
            put(new MsgSetTempBasalStop());          // 0x0403 CMD_PUMPSET_EXERCISE_STOP
            put(new MsgSetExtendedBolusStop());      // 0x0406 CMD_PUMPSET_EXPANS_INS_STOP
            put(new MsgSetExtendedBolusStart());     // 0x0407 CMD_PUMPSET_EXPANS_INS_S
            put(new MsgOcclusion());                 // 0x0601 CMD_PUMPOWAY_SYSTEM_STATUS
            put(new MsgPCCommStart());               // 0x3001 CMD_CONNECT
            put(new MsgPCCommStop());                // 0x3002 CMD_DISCONNECT
            put(new MsgSettingBasal());              // 0x3202 CMD_SETTING_V_BASAL_INS_I
            put(new MsgSettingProfileRatios());      // 0x3204 CMD_SETTING_V_CCC_I
            put(new MsgSettingMaxValues());          // 0x3205 CMD_SETTING_V_MAX_VALUE_I
            put(new MsgSettingBasalProfileAll());    // 0x3206 CMD_SETTING_V_BASAL_PROFILE_ALL
            put(new MsgSettingShippingInfo());       // 0x3207 CMD_SETTING_V_SHIPPING_I
            put(new MsgSettingGlucose());            // 0x3209 CMD_SETTING_V_GLUCOSEandEASY
            put(new MsgSettingPumpTime());           // 0x320A CMD_SETTING_V_TIME_I
            put(new MsgSettingUserOptions());        // 0x320B CMD_SETTING_V_USER_OPTIONS
            put(new MsgSettingActiveProfile());      // 0x320C CMD_SETTING_V_PROFILE_NUMBER
            put(new MsgSettingProfileRatiosAll());   // 0x320D CMD_SETTING_V_CIR_CF_VALUE
            put(new MsgSetBasalProfile());           // 0x3306 CMD_SETTING_BASAL_PROFILE_S
            put(new MsgSetActivateBasalProfile());   // 0x330C CMD_SETTING_PROFILE_NUMBER_S
            put(new MsgHistoryAll());                // 0x41F2 CMD_HISTORY_ALL
            put(new MsgHistoryAllDone());            // 0x41F1 CMD_HISTORY_ALL_DONE
            put(new MsgCheckValue());                // 0xF0F1 CMD_PUMP_CHECK_VALUE
        }
    }

    public static void put(DanaRMessage message) {
        int command = message.getCommand();
        //String name = DanaRMessageNames.getName(command);
        messages.put(command, message);
        //log.debug(String.format("%04x ", command) + " " + name);
    }

    public static DanaRMessage findMessage(Integer command) {
        if (messages.containsKey(command)) {
            return messages.get(command);
        } else {
            return new DanaRMessage();
        }
    }
}
