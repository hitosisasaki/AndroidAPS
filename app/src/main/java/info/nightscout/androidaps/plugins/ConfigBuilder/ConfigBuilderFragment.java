package info.nightscout.androidaps.plugins.ConfigBuilder;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import info.nightscout.androidaps.Config;
import info.nightscout.androidaps.MainActivity;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.Services.Intents;
import info.nightscout.androidaps.data.PumpEnactResult;
import info.nightscout.androidaps.db.TempBasal;
import info.nightscout.androidaps.db.Treatment;
import info.nightscout.androidaps.events.EventRefreshGui;
import info.nightscout.androidaps.events.EventTempBasalChange;
import info.nightscout.androidaps.events.EventTreatmentChange;
import info.nightscout.androidaps.interfaces.BgSourceInterface;
import info.nightscout.androidaps.interfaces.ConstraintsInterface;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.interfaces.ProfileInterface;
import info.nightscout.androidaps.interfaces.PumpInterface;
import info.nightscout.androidaps.interfaces.TempBasalsInterface;
import info.nightscout.androidaps.interfaces.TreatmentsInterface;
import info.nightscout.androidaps.plugins.Loop.APSResult;
import info.nightscout.androidaps.plugins.Loop.DeviceStatus;
import info.nightscout.androidaps.plugins.Loop.LoopFragment;
import info.nightscout.androidaps.plugins.OpenAPSMA.DetermineBasalResult;
import info.nightscout.client.data.DbLogger;
import info.nightscout.client.data.NSProfile;
import info.nightscout.utils.DateUtil;

public class ConfigBuilderFragment extends Fragment implements PluginBase, PumpInterface, ConstraintsInterface {
    private static Logger log = LoggerFactory.getLogger(ConfigBuilderFragment.class);

    ListView bgsourceListView;
    ListView pumpListView;
    ListView loopListView;
    TextView loopLabel;
    ListView treatmentsListView;
    ListView tempsListView;
    ListView profileListView;
    ListView apsListView;
    TextView apsLabel;
    ListView constraintsListView;
    ListView generalListView;
    TextView nsclientVerView;
    TextView nightscoutVerView;

    PluginCustomAdapter bgsourceDataAdapter = null;
    PluginCustomAdapter pumpDataAdapter = null;
    PluginCustomAdapter loopDataAdapter = null;
    PluginCustomAdapter treatmentsDataAdapter = null;
    PluginCustomAdapter tempsDataAdapter = null;
    PluginCustomAdapter profileDataAdapter = null;
    PluginCustomAdapter apsDataAdapter = null;
    PluginCustomAdapter constraintsDataAdapter = null;
    PluginCustomAdapter generalDataAdapter = null;


    BgSourceInterface activeBgSource;
    PumpInterface activePump;
    ProfileInterface activeProfile;
    TreatmentsInterface activeTreatments;
    TempBasalsInterface activeTempBasals;
    LoopFragment activeLoop;

    public String nightscoutVersionName = "";
    public Integer nightscoutVersionCode = 0;
    public String nsClientVersionName = "";
    public Integer nsClientVersionCode = 0;

    ArrayList<PluginBase> pluginList;

    // TODO: sorting

    public ConfigBuilderFragment() {
        super();
        registerBus();
    }

    public void initialize() {
        pluginList = MainActivity.getPluginsList();
        loadSettings();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void registerBus() {
        try {
            MainApp.bus().unregister(this);
        } catch (RuntimeException x) {
            // Ignore
        }
        MainApp.bus().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.configbuilder_fragment, container, false);
        bgsourceListView = (ListView) view.findViewById(R.id.configbuilder_bgsourcelistview);
        pumpListView = (ListView) view.findViewById(R.id.configbuilder_pumplistview);
        loopListView = (ListView) view.findViewById(R.id.configbuilder_looplistview);
        loopLabel = (TextView) view.findViewById(R.id.configbuilder_looplabel);
        treatmentsListView = (ListView) view.findViewById(R.id.configbuilder_treatmentslistview);
        tempsListView = (ListView) view.findViewById(R.id.configbuilder_tempslistview);
        profileListView = (ListView) view.findViewById(R.id.configbuilder_profilelistview);
        apsListView = (ListView) view.findViewById(R.id.configbuilder_apslistview);
        apsLabel = (TextView) view.findViewById(R.id.configbuilder_apslabel);
        constraintsListView = (ListView) view.findViewById(R.id.configbuilder_constraintslistview);
        generalListView = (ListView) view.findViewById(R.id.configbuilder_generallistview);
        nsclientVerView = (TextView) view.findViewById(R.id.configbuilder_nsclientversion);
        nightscoutVerView = (TextView) view.findViewById(R.id.configbuilder_nightscoutversion);

        nsclientVerView.setText(nsClientVersionName);
        nightscoutVerView.setText(nightscoutVersionName);
        if (nsClientVersionCode < 117) nsclientVerView.setTextColor(Color.RED);
        if (nightscoutVersionCode < 900) nightscoutVerView.setTextColor(Color.RED);
        setViews();
        return view;
    }

    void setViews() {
        bgsourceDataAdapter = new PluginCustomAdapter(getContext(), R.layout.configbuilder_simpleitem, MainActivity.getSpecificPluginsList(PluginBase.BGSOURCE), PluginBase.BGSOURCE);
        bgsourceListView.setAdapter(bgsourceDataAdapter);
        setListViewHeightBasedOnChildren(bgsourceListView);
        pumpDataAdapter = new PluginCustomAdapter(getContext(), R.layout.configbuilder_simpleitem, MainActivity.getSpecificPluginsList(PluginBase.PUMP), PluginBase.PUMP);
        pumpListView.setAdapter(pumpDataAdapter);
        setListViewHeightBasedOnChildren(pumpListView);
        loopDataAdapter = new PluginCustomAdapter(getContext(), R.layout.configbuilder_simpleitem, MainActivity.getSpecificPluginsList(PluginBase.LOOP), PluginBase.LOOP);
        loopListView.setAdapter(loopDataAdapter);
        setListViewHeightBasedOnChildren(loopListView);
        if (MainActivity.getSpecificPluginsList(PluginBase.LOOP).size() == 0) loopLabel.setVisibility(View.GONE);
        treatmentsDataAdapter = new PluginCustomAdapter(getContext(), R.layout.configbuilder_simpleitem, MainActivity.getSpecificPluginsList(PluginBase.TREATMENT), PluginBase.TREATMENT);
        treatmentsListView.setAdapter(treatmentsDataAdapter);
        setListViewHeightBasedOnChildren(treatmentsListView);
        tempsDataAdapter = new PluginCustomAdapter(getContext(), R.layout.configbuilder_simpleitem, MainActivity.getSpecificPluginsList(PluginBase.TEMPBASAL), PluginBase.TEMPBASAL);
        tempsListView.setAdapter(tempsDataAdapter);
        setListViewHeightBasedOnChildren(tempsListView);
        profileDataAdapter = new PluginCustomAdapter(getContext(), R.layout.configbuilder_simpleitem, MainActivity.getSpecificPluginsListByInterface(ProfileInterface.class), PluginBase.PROFILE);
        profileListView.setAdapter(profileDataAdapter);
        setListViewHeightBasedOnChildren(profileListView);
        apsDataAdapter = new PluginCustomAdapter(getContext(), R.layout.configbuilder_simpleitem, MainActivity.getSpecificPluginsList(PluginBase.APS), PluginBase.APS);
        apsListView.setAdapter(apsDataAdapter);
        setListViewHeightBasedOnChildren(apsListView);
        if (MainActivity.getSpecificPluginsList(PluginBase.APS).size() == 0) apsLabel.setVisibility(View.GONE);
        constraintsDataAdapter = new PluginCustomAdapter(getContext(), R.layout.configbuilder_simpleitem, MainActivity.getSpecificPluginsListByInterface(ConstraintsInterface.class), PluginBase.CONSTRAINTS);
        constraintsListView.setAdapter(constraintsDataAdapter);
        setListViewHeightBasedOnChildren(constraintsListView);
        generalDataAdapter = new PluginCustomAdapter(getContext(), R.layout.configbuilder_simpleitem, MainActivity.getSpecificPluginsList(PluginBase.GENERAL), PluginBase.GENERAL);
        generalListView.setAdapter(generalDataAdapter);
        setListViewHeightBasedOnChildren(generalListView);

    }

    /*
     * PluginBase interface
     */
    @Override
    public int getType() {
        return PluginBase.GENERAL;
    }

    @Override
    public String getName() {
        return MainApp.instance().getString(R.string.configbuilder);
    }

    @Override
    public boolean isEnabled(int type) {
        return true;
    }

    @Override
    public boolean isVisibleInTabs(int type) {
        return true;
    }

    @Override
    public boolean canBeHidden(int type) {
        return false;
    }

    @Override
    public void setFragmentEnabled(int type, boolean fragmentEnabled) {
        // Always enabled
    }

    @Override
    public void setFragmentVisible(int type, boolean fragmentVisible) {
        // Always visible
    }

    public static ConfigBuilderFragment newInstance() {
        ConfigBuilderFragment fragment = new ConfigBuilderFragment();
        return fragment;
    }


    /*
     * Pump interface
     *
     * Config builder return itself as a pump and check constraints before it passes command to pump driver
     */
    @Override
    public boolean isTempBasalInProgress() {
        return activePump.isTempBasalInProgress();
    }

    @Override
    public boolean isExtendedBoluslInProgress() {
        return activePump.isExtendedBoluslInProgress();
    }

    @Override
    public void setNewBasalProfile(NSProfile profile) {
        activePump.setNewBasalProfile(profile);
    }

    @Override
    public double getBaseBasalRate() {
        return activePump.getBaseBasalRate();
    }

    @Override
    public double getTempBasalAbsoluteRate() {
        return activePump.getTempBasalAbsoluteRate();
    }

    @Override
    public double getTempBasalRemainingMinutes() {
        return activePump.getTempBasalRemainingMinutes();
    }

    @Override
    public TempBasal getTempBasal() {
        return activePump.getTempBasal();
    }

    @Override
    public TempBasal getExtendedBolus() {
        return activePump.getExtendedBolus();
    }

    @Override
    public PumpEnactResult deliverTreatment(Double insulin, Integer carbs) {
        insulin = applyBolusConstraints(insulin);
        carbs = applyCarbsConstraints(carbs);

        PumpEnactResult result = activePump.deliverTreatment(insulin, carbs);

        if (result.success) {
            Treatment t = new Treatment();
            t.insulin = result.bolusDelivered;
            t.carbs = (double) result.carbsDelivered;
            t.created_at = new Date();
            try {
                MainApp.getDbHelper().getDaoTreatments().create(t);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            t.setTimeIndex(t.getTimeIndex());
            t.sendToNSClient();
            MainApp.bus().post(new EventTreatmentChange());
        }
        return result;
    }

    /**
     * apply constraints, set temp based on absolute valus and expecting absolute result
     *
     * @param absoluteRate
     * @param durationInMinutes
     * @return
     */
    @Override
    public PumpEnactResult setTempBasalAbsolute(Double absoluteRate, Integer durationInMinutes) {
        Double rateAfterConstraints = applyBasalConstraints(absoluteRate);
        PumpEnactResult result = activePump.setTempBasalAbsolute(rateAfterConstraints, durationInMinutes);
        if (result.enacted && result.success) {
            uploadTempBasalStartAbsolute(result.absolute, result.duration);
            MainApp.bus().post(new EventTempBasalChange());
        }
        return result;
    }

    /**
     * apply constraints, set temp based on percent and expecting result in percent
     *
     * @param percent           0 ... 100 ...
     * @param durationInMinutes
     * @return result
     */
    @Override
    public PumpEnactResult setTempBasalPercent(Integer percent, Integer durationInMinutes) {
        Integer percentAfterConstraints = applyBasalConstraints(percent);
        PumpEnactResult result = activePump.setTempBasalPercent(percentAfterConstraints, durationInMinutes);
        if (result.enacted && result.success) {
            uploadTempBasalStartPercent(result.percent, result.duration);
            MainApp.bus().post(new EventTempBasalChange());
        }
        return result;
    }

    @Override
    public PumpEnactResult setExtendedBolus(Double insulin, Integer durationInMinutes) {
        Double rateAfterConstraints = applyBolusConstraints(insulin);
        PumpEnactResult result = activePump.setExtendedBolus(rateAfterConstraints, durationInMinutes);
        if (result.enacted && result.success) {
            uploadExtendedBolus(result.bolusDelivered, result.duration);
            MainApp.bus().post(new EventTreatmentChange());
        }
        return result;
    }

    @Override
    public PumpEnactResult cancelTempBasal() {
        PumpEnactResult result = activePump.cancelTempBasal();
        if (result.enacted && result.success) {
            uploadTempBasalEnd();
            MainApp.bus().post(new EventTempBasalChange());
        }
        return result;
    }

    @Override
    public PumpEnactResult cancelExtendedBolus() {
        return activePump.cancelExtendedBolus();
    }

    /**
     * expect absolute request and allow both absolute and percent response based on pump capabilities
     *
     * @param request
     * @return
     */
    // TODO: logging all actions in configbuilder
    public PumpEnactResult applyAPSRequest(APSResult request) {
        request.rate = applyBasalConstraints(request.rate);
        PumpEnactResult result = null;

        if (request.rate == getBaseBasalRate()) {
            if (isTempBasalInProgress()) {
                result = cancelTempBasal();
                if (result.enacted) {
                    uploadTempBasalEnd();
                    MainApp.bus().post(new EventTempBasalChange());
                }
            } else {
                result = new PumpEnactResult();
                result.absolute = request.rate;
                result.duration = 0;
                result.enacted = false;
                result.comment = "Basal set correctly";
                result.success = true;
            }
        } else if (isTempBasalInProgress() && Math.abs(request.rate - getTempBasalAbsoluteRate()) < 0.05) {
            result = new PumpEnactResult();
            result.absolute = getTempBasalAbsoluteRate();
            result.duration = activePump.getTempBasal().getPlannedRemainingMinutes();
            result.enacted = false;
            result.comment = "Temp basal set correctly";
            result.success = true;
        } else {
            result = setTempBasalAbsolute(request.rate, request.duration);
            if (result.enacted) {
                if (result.isPercent) {
                    uploadTempBasalStartPercent(result.percent, result.duration);
                } else {
                    uploadTempBasalStartAbsolute(result.absolute, result.duration);
                }
                MainApp.bus().post(new EventTempBasalChange());
            }
        }
        return result;
    }

    @Override
    public JSONObject getJSONStatus() {
        return activePump.getJSONStatus();
    }

    @Override
    public String deviceID() {
        return activePump.deviceID();
    }

    /*
     * ConfigBuilderFragment code
     */

    private class PluginCustomAdapter extends ArrayAdapter<PluginBase> {

        private ArrayList<PluginBase> pluginList;
        final private int type;

        public PluginCustomAdapter(Context context, int textViewResourceId,
                                   ArrayList<PluginBase> pluginList, int type) {
            super(context, textViewResourceId, pluginList);
            this.pluginList = new ArrayList<PluginBase>();
            this.pluginList.addAll(pluginList);
            this.type = type;
        }

        private class PluginViewHolder {
            TextView name;
            CheckBox checkboxEnabled;
            CheckBox checkboxVisible;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            PluginViewHolder holder = null;

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.configbuilder_simpleitem, null);

                holder = new PluginViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.configbuilder_simpleitem_name);
                holder.checkboxEnabled = (CheckBox) convertView.findViewById(R.id.configbuilder_simpleitem_checkboxenabled);
                holder.checkboxVisible = (CheckBox) convertView.findViewById(R.id.configbuilder_simpleitem_checkboxvisible);
                convertView.setTag(holder);

                holder.checkboxEnabled.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        CheckBox cb = (CheckBox) v;
                        PluginBase plugin = (PluginBase) cb.getTag();
                        plugin.setFragmentEnabled(type, cb.isChecked());
                        if (cb.isChecked()) plugin.setFragmentVisible(type, true);
                        onEnabledCategoryChanged(plugin, type);
                        storeSettings();
                    }
                });

                holder.checkboxVisible.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        CheckBox cb = (CheckBox) v;
                        PluginBase plugin = (PluginBase) cb.getTag();
                        plugin.setFragmentVisible(type, cb.isChecked());
                        storeSettings();
                        MainApp.bus().post(new EventRefreshGui());
                    }
                });
            } else {
                holder = (PluginViewHolder) convertView.getTag();
            }

            PluginBase plugin = pluginList.get(position);
            holder.name.setText(plugin.getName());
            holder.checkboxEnabled.setChecked(plugin.isEnabled(type));
            holder.checkboxVisible.setChecked(plugin.isVisibleInTabs(type));
            holder.name.setTag(plugin);
            holder.checkboxEnabled.setTag(plugin);
            holder.checkboxVisible.setTag(plugin);

            if (!plugin.canBeHidden(type)) {
                holder.checkboxEnabled.setEnabled(false);
                holder.checkboxVisible.setEnabled(false);
            }

            int type = plugin.getType();
            // Force enabled if there is only one plugin
            if (type == PluginBase.PUMP || type == PluginBase.TREATMENT || type == PluginBase.TEMPBASAL || type == PluginBase.PROFILE)
                if (pluginList.size() < 2)
                    holder.checkboxEnabled.setEnabled(false);

            // Constraints cannot be disabled
            if (type == PluginBase.CONSTRAINTS)
                holder.checkboxEnabled.setEnabled(false);

            // Hide disabled profiles by default
            if (type == PluginBase.PROFILE) {
                if (!plugin.isEnabled(type)) {
                    holder.checkboxVisible.setEnabled(false);
                    holder.checkboxVisible.setChecked(false);
                } else {
                    holder.checkboxVisible.setEnabled(true);
                }
            }

            return convertView;

        }

    }

    public BgSourceInterface getActiveBgSource() {
        return activeBgSource;
    }

    public PumpInterface getActivePump() {
        return this;
    }

    @Nullable
    public ProfileInterface getActiveProfile() {
        return activeProfile;
    }

    public TreatmentsInterface getActiveTreatments() {
        return activeTreatments;
    }

    public TempBasalsInterface getActiveTempBasals() {
        return activeTempBasals;
    }

    public LoopFragment getActiveLoop() {
        return activeLoop;
    }

    void onEnabledCategoryChanged(PluginBase changedPlugin, int type) {
        int category = changedPlugin.getType();
        ArrayList<PluginBase> pluginsInCategory = null;
        switch (category) {
            // Multiple selection allowed
            case PluginBase.APS:
            case PluginBase.GENERAL:
            case PluginBase.CONSTRAINTS:
                break;
            // Single selection allowed
            case PluginBase.PROFILE:
                pluginsInCategory = MainActivity.getSpecificPluginsListByInterface(ProfileInterface.class);
                break;
            case PluginBase.BGSOURCE:
            case PluginBase.LOOP:
            case PluginBase.TEMPBASAL:
            case PluginBase.TREATMENT:
            case PluginBase.PUMP:
                pluginsInCategory = MainActivity.getSpecificPluginsList(category);
                break;
        }
        if (pluginsInCategory != null) {
            boolean newSelection = changedPlugin.isEnabled(type);
            if (newSelection) { // new plugin selected -> disable others
                for (PluginBase p : pluginsInCategory) {
                    if (p.getName().equals(changedPlugin.getName())) {
                        // this is new selected
                    } else {
                        p.setFragmentEnabled(type, false);
                        setViews();
                    }
                }
            } else { // enable first plugin in list
                pluginsInCategory.get(0).setFragmentEnabled(type, true);
            }
        }
    }

    private void verifySelectionInCategories() {
        ArrayList<PluginBase> pluginsInCategory;

        // PluginBase.PROFILE
        pluginsInCategory = MainActivity.getSpecificPluginsListByInterface(ProfileInterface.class);
        activeProfile = (ProfileInterface) getTheOneEnabledInArray(pluginsInCategory, PluginBase.PROFILE);
        if (Config.logConfigBuilder)
            log.debug("Selected profile interface: " + ((PluginBase) activeProfile).getName());
        for (PluginBase p : pluginsInCategory) {
            if (!p.getName().equals(((PluginBase) activeProfile).getName())) {
                p.setFragmentVisible(PluginBase.PROFILE, false);
            }
        }

        // PluginBase.BGSOURCE
        pluginsInCategory = MainActivity.getSpecificPluginsList(PluginBase.BGSOURCE);
        activeBgSource = (BgSourceInterface) getTheOneEnabledInArray(pluginsInCategory, PluginBase.BGSOURCE);
        if (Config.logConfigBuilder)
            log.debug("Selected bgSource interface: " + ((PluginBase) activeBgSource).getName());
        for (PluginBase p : pluginsInCategory) {
            if (!p.getName().equals(((PluginBase) activeBgSource).getName())) {
                p.setFragmentVisible(PluginBase.BGSOURCE, false);
            }
        }

        // PluginBase.PUMP
        pluginsInCategory = MainActivity.getSpecificPluginsList(PluginBase.PUMP);
        activePump = (PumpInterface) getTheOneEnabledInArray(pluginsInCategory, PluginBase.PUMP);
        if (Config.logConfigBuilder)
            log.debug("Selected pump interface: " + ((PluginBase) activePump).getName());
        for (PluginBase p : pluginsInCategory) {
            if (!p.getName().equals(((PluginBase) activePump).getName())) {
                p.setFragmentVisible(PluginBase.PUMP, false);
            }
        }

        // PluginBase.LOOP
        pluginsInCategory = MainActivity.getSpecificPluginsList(PluginBase.LOOP);
        activeLoop = (LoopFragment) getTheOneEnabledInArray(pluginsInCategory, PluginBase.LOOP);
        if (activeLoop != null) {
            if (Config.logConfigBuilder)
                log.debug("Selected loop interface: " + activeLoop.getName());
            for (PluginBase p : pluginsInCategory) {
                if (!p.getName().equals(activeLoop.getName())) {
                    p.setFragmentVisible(PluginBase.LOOP, false);
                }
            }
        }

        // PluginBase.TEMPBASAL
        pluginsInCategory = MainActivity.getSpecificPluginsList(PluginBase.TEMPBASAL);
        activeTempBasals = (TempBasalsInterface) getTheOneEnabledInArray(pluginsInCategory, PluginBase.TEMPBASAL);
        if (Config.logConfigBuilder)
            log.debug("Selected tempbasal interface: " + ((PluginBase) activeTempBasals).getName());
        for (PluginBase p : pluginsInCategory) {
            if (!p.getName().equals(((PluginBase) activeTempBasals).getName())) {
                p.setFragmentVisible(PluginBase.TEMPBASAL, false);
            }
        }

        // PluginBase.TREATMENT
        pluginsInCategory = MainActivity.getSpecificPluginsList(PluginBase.TREATMENT);
        activeTreatments = (TreatmentsInterface) getTheOneEnabledInArray(pluginsInCategory, PluginBase.TREATMENT);
        if (Config.logConfigBuilder)
            log.debug("Selected treatment interface: " + ((PluginBase) activeTreatments).getName());
        for (PluginBase p : pluginsInCategory) {
            if (!p.getName().equals(((PluginBase) activeTreatments).getName())) {
                p.setFragmentVisible(PluginBase.TREATMENT, false);
            }
        }
    }

    @Nullable
    private PluginBase getTheOneEnabledInArray(ArrayList<PluginBase> pluginsInCategory, int type) {
        PluginBase found = null;
        for (PluginBase p : pluginsInCategory) {
            if (p.isEnabled(type) && found == null) {
                found = p;
                continue;
            } else if (p.isEnabled(type)) {
                // set others disabled
                p.setFragmentEnabled(type, false);
            }
        }
        // If none enabled, enable first one
        if (found == null && pluginsInCategory.size() > 0)
            found = pluginsInCategory.get(0);
        return found;
    }

    private void storeSettings() {
        if (pluginList != null) {
            if (Config.logPrefsChange)
                log.debug("Storing settings");
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
            SharedPreferences.Editor editor = settings.edit();

            for (int type = 1; type < PluginBase.LAST; type++) {
                for (PluginBase p : pluginList) {
                    String settingEnabled = "ConfigBuilder_" + type + "_" + p.getName() + "_Enabled";
                    String settingVisible = "ConfigBuilder_" + type + "_" + p.getName() + "_Visible";
                    editor.putBoolean(settingEnabled, p.isEnabled(type));
                    editor.putBoolean(settingVisible, p.isVisibleInTabs(type));
                }
            }
            editor.commit();
            verifySelectionInCategories();
        }
    }

    private void loadSettings() {
        if (Config.logPrefsChange)
            log.debug("Loading stored settings");
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
        for (int type = 1; type < PluginBase.LAST; type++) {
            for (PluginBase p : pluginList) {
                String settingEnabled = "ConfigBuilder_" + type + "_" + p.getName() + "_Enabled";
                String settingVisible = "ConfigBuilder_" + type + "_" + p.getName() + "_Visible";
                if (SP.contains(settingEnabled))
                    p.setFragmentEnabled(type, SP.getBoolean(settingEnabled, true));
                if (SP.contains("ConfigBuilder" + p.getName() + "Visible"))
                    p.setFragmentVisible(type, SP.getBoolean(settingVisible, true));
            }
        }
        verifySelectionInCategories();
    }

    /****
     * Method for Setting the Height of the ListView dynamically.
     * *** Hack to fix the issue of not showing all the items of the ListView
     * *** when placed inside a ScrollView
     ****/
    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    /**
     * Constraints interface
     **/
    @Override
    public boolean isLoopEnabled() {
        boolean result = true;

        ArrayList<PluginBase> constraintsPlugins = MainActivity.getSpecificPluginsListByInterface(ConstraintsInterface.class);
        for (PluginBase p : constraintsPlugins) {
            ConstraintsInterface constrain = (ConstraintsInterface) p;
            if (!p.isEnabled(PluginBase.CONSTRAINTS)) continue;
            result = result && constrain.isLoopEnabled();
        }
        return result;
    }

    @Override
    public boolean isClosedModeEnabled() {
        boolean result = true;

        ArrayList<PluginBase> constraintsPlugins = MainActivity.getSpecificPluginsListByInterface(ConstraintsInterface.class);
        for (PluginBase p : constraintsPlugins) {
            ConstraintsInterface constrain = (ConstraintsInterface) p;
            if (!p.isEnabled(PluginBase.CONSTRAINTS)) continue;
            result = result && constrain.isClosedModeEnabled();
        }
        return result;
    }

    @Override
    public boolean isAutosensModeEnabled() {
        boolean result = true;

        ArrayList<PluginBase> constraintsPlugins = MainActivity.getSpecificPluginsListByInterface(ConstraintsInterface.class);
        for (PluginBase p : constraintsPlugins) {
            ConstraintsInterface constrain = (ConstraintsInterface) p;
            if (!p.isEnabled(PluginBase.CONSTRAINTS)) continue;
            result = result && constrain.isAutosensModeEnabled();
        }
        return result;
    }

    @Override
    public boolean isAMAModeEnabled() {
        boolean result = true;

        ArrayList<PluginBase> constraintsPlugins = MainActivity.getSpecificPluginsListByInterface(ConstraintsInterface.class);
        for (PluginBase p : constraintsPlugins) {
            ConstraintsInterface constrain = (ConstraintsInterface) p;
            if (!p.isEnabled(PluginBase.CONSTRAINTS)) continue;
            result = result && constrain.isAMAModeEnabled();
        }
        return result;
    }

    @Override
    public Double applyBasalConstraints(Double absoluteRate) {
        Double rateAfterConstrain = absoluteRate;
        ArrayList<PluginBase> constraintsPlugins = MainActivity.getSpecificPluginsListByInterface(ConstraintsInterface.class);
        for (PluginBase p : constraintsPlugins) {
            ConstraintsInterface constrain = (ConstraintsInterface) p;
            if (!p.isEnabled(PluginBase.CONSTRAINTS)) continue;
            rateAfterConstrain = Math.min(constrain.applyBasalConstraints(absoluteRate), rateAfterConstrain);
        }
        return rateAfterConstrain;
    }

    @Override
    public Integer applyBasalConstraints(Integer percentRate) {
        Integer rateAfterConstrain = percentRate;
        ArrayList<PluginBase> constraintsPlugins = MainActivity.getSpecificPluginsListByInterface(ConstraintsInterface.class);
        for (PluginBase p : constraintsPlugins) {
            ConstraintsInterface constrain = (ConstraintsInterface) p;
            if (!p.isEnabled(PluginBase.CONSTRAINTS)) continue;
            rateAfterConstrain = Math.min(constrain.applyBasalConstraints(percentRate), rateAfterConstrain);
        }
        return rateAfterConstrain;
    }

    @Override
    public Double applyBolusConstraints(Double insulin) {
        Double insulinAfterConstrain = insulin;
        ArrayList<PluginBase> constraintsPlugins = MainActivity.getSpecificPluginsListByInterface(ConstraintsInterface.class);
        for (PluginBase p : constraintsPlugins) {
            ConstraintsInterface constrain = (ConstraintsInterface) p;
            if (!p.isEnabled(PluginBase.CONSTRAINTS)) continue;
            insulinAfterConstrain = Math.min(constrain.applyBolusConstraints(insulin), insulinAfterConstrain);
        }
        return insulinAfterConstrain;
    }

    @Override
    public Integer applyCarbsConstraints(Integer carbs) {
        Integer carbsAfterConstrain = carbs;
        ArrayList<PluginBase> constraintsPlugins = MainActivity.getSpecificPluginsListByInterface(ConstraintsInterface.class);
        for (PluginBase p : constraintsPlugins) {
            ConstraintsInterface constrain = (ConstraintsInterface) p;
            if (!p.isEnabled(PluginBase.CONSTRAINTS)) continue;
            carbsAfterConstrain = Math.min(constrain.applyCarbsConstraints(carbs), carbsAfterConstrain);
        }
        return carbsAfterConstrain;
    }

    @Override
    public Double applyMaxIOBConstraints(Double maxIob) {
        Double maxIobAfterConstrain = maxIob;
        ArrayList<PluginBase> constraintsPlugins = MainActivity.getSpecificPluginsListByInterface(ConstraintsInterface.class);
        for (PluginBase p : constraintsPlugins) {
            ConstraintsInterface constrain = (ConstraintsInterface) p;
            if (!p.isEnabled(PluginBase.CONSTRAINTS)) continue;
            maxIobAfterConstrain = Math.min(constrain.applyMaxIOBConstraints(maxIob), maxIobAfterConstrain);
        }
        return maxIobAfterConstrain;
    }

    public static void uploadTempBasalStartAbsolute(Double absolute, double durationInMinutes) {
        try {
            Context context = MainApp.instance().getApplicationContext();
            JSONObject data = new JSONObject();
            data.put("eventType", "Temp Basal");
            data.put("duration", durationInMinutes);
            data.put("absolute", absolute);
            data.put("created_at", DateUtil.toISOString(new Date()));
            data.put("enteredBy", MainApp.instance().getString(R.string.app_name));
            Bundle bundle = new Bundle();
            bundle.putString("action", "dbAdd");
            bundle.putString("collection", "treatments");
            bundle.putString("data", data.toString());
            Intent intent = new Intent(Intents.ACTION_DATABASE);
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            context.sendBroadcast(intent);
            DbLogger.dbAdd(intent, data.toString(), ConfigBuilderFragment.class);
        } catch (JSONException e) {
        }
    }

    public static void uploadTempBasalStartPercent(Integer percent, double durationInMinutes) {
        try {
            Context context = MainApp.instance().getApplicationContext();
            JSONObject data = new JSONObject();
            data.put("eventType", "Temp Basal");
            data.put("duration", durationInMinutes);
            data.put("percent", percent - 100);
            data.put("created_at", DateUtil.toISOString(new Date()));
            data.put("enteredBy", MainApp.instance().getString(R.string.app_name));
            Bundle bundle = new Bundle();
            bundle.putString("action", "dbAdd");
            bundle.putString("collection", "treatments");
            bundle.putString("data", data.toString());
            Intent intent = new Intent(Intents.ACTION_DATABASE);
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            context.sendBroadcast(intent);
            DbLogger.dbAdd(intent, data.toString(), ConfigBuilderFragment.class);
        } catch (JSONException e) {
        }
    }

    public static void uploadTempBasalEnd() {
        try {
            Context context = MainApp.instance().getApplicationContext();
            JSONObject data = new JSONObject();
            data.put("eventType", "Temp Basal");
            data.put("created_at", DateUtil.toISOString(new Date()));
            data.put("enteredBy", MainApp.instance().getString(R.string.app_name));
            Bundle bundle = new Bundle();
            bundle.putString("action", "dbAdd");
            bundle.putString("collection", "treatments");
            bundle.putString("data", data.toString());
            Intent intent = new Intent(Intents.ACTION_DATABASE);
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            context.sendBroadcast(intent);
            DbLogger.dbAdd(intent, data.toString(), ConfigBuilderFragment.class);
        } catch (JSONException e) {
        }
    }

    public static void uploadExtendedBolus(Double insulin, double durationInMinutes) {
        try {
            Context context = MainApp.instance().getApplicationContext();
            JSONObject data = new JSONObject();
            data.put("eventType", "Combo Bolus");
            data.put("duration", durationInMinutes);
            data.put("splitNow", 0);
            data.put("splitExt", 100);
            data.put("enteredinsulin", insulin);
            data.put("relative", insulin);
            data.put("created_at", DateUtil.toISOString(new Date()));
            data.put("enteredBy", MainApp.instance().getString(R.string.app_name));
            Bundle bundle = new Bundle();
            bundle.putString("action", "dbAdd");
            bundle.putString("collection", "treatments");
            bundle.putString("data", data.toString());
            Intent intent = new Intent(Intents.ACTION_DATABASE);
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            context.sendBroadcast(intent);
            DbLogger.dbAdd(intent, data.toString(), ConfigBuilderFragment.class);
        } catch (JSONException e) {
        }
    }

    public void uploadDeviceStatus() {
        DeviceStatus deviceStatus = new DeviceStatus();
        try {
            LoopFragment.LastRun lastRun = LoopFragment.lastRun;
            if (lastRun == null) return;
            if (lastRun.lastAPSRun.getTime() < new Date().getTime() - 60 * 1000L)
                return; // do not send if result is older than 1 min

            APSResult apsResult = lastRun.request;
            apsResult.json().put("timestamp", DateUtil.toISOString(lastRun.lastAPSRun));
            deviceStatus.suggested = apsResult.json();

            if (lastRun.request instanceof DetermineBasalResult) {
                DetermineBasalResult result = (DetermineBasalResult) lastRun.request;
                deviceStatus.iob = result.iob.json();
                deviceStatus.iob.put("time", DateUtil.toISOString(lastRun.lastAPSRun));
            }

            if (lastRun.setByPump != null && lastRun.setByPump.enacted) { // enacted
                deviceStatus.enacted = lastRun.request.json();
                deviceStatus.enacted.put("rate", lastRun.setByPump.json().get("rate"));
                deviceStatus.enacted.put("duration", lastRun.setByPump.json().get("duration"));
                deviceStatus.enacted.put("recieved", true);
                JSONObject requested = new JSONObject();
                requested.put("duration", lastRun.request.duration);
                requested.put("rate", lastRun.request.rate);
                requested.put("temp", "absolute");
                deviceStatus.enacted.put("requested", requested);
            }

            deviceStatus.device = "openaps://" + getActivePump().deviceID();
            deviceStatus.pump = getActivePump().getJSONStatus();

            deviceStatus.created_at = DateUtil.toISOString(new Date());

            deviceStatus.sendToNSClient();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
