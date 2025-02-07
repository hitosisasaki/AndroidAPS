package info.nightscout.androidaps;

/**
 * Created by mike on 07.06.2016.
 */
public class Config {
    // MAIN FUCTIONALITY
    public static final boolean APS = true;
    // PLUGINS
    public static final boolean LOWSUSPEDENABLED = APS && true;
    public static final boolean OPENAPSMAENABLED = APS && true;
    public static final boolean LOOPENABLED = APS && true;
    public static final boolean OBJECTIVESENABLED = APS && true;
    public static final boolean CAREPORTALENABLED = true;

    public static final boolean detailedLog = true;
    public static final boolean logFunctionCalls = true;
    public static final boolean logIncommingBG = true;
    public static final boolean logIncommingData = true;
    public static final boolean logAPSResult = true;
    public static final boolean logPumpComm = true;
    public static final boolean logPrefsChange = true;
    public static final boolean logConfigBuilder = true;
    public static final boolean logConstraintsChanges = true;
    public static final boolean logTempBasalsCut = true;
    public static final boolean logNSUpload = true;

    // Developing mode only - never turn on
    // TODO: remove fakeGlucoseData
    public static final boolean fakeGlucoseData = false;
}
