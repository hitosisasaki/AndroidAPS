package info.nightscout.androidaps.plugins.Loop;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;

import info.nightscout.androidaps.Constants;
import info.nightscout.androidaps.MainActivity;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.PumpEnactResult;
import info.nightscout.androidaps.events.EventNewBG;
import info.nightscout.androidaps.events.EventRefreshOpenLoop;
import info.nightscout.androidaps.events.EventTreatmentChange;
import info.nightscout.androidaps.interfaces.APSInterface;
import info.nightscout.androidaps.interfaces.ConstraintsInterface;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.interfaces.PumpInterface;

public class LoopFragment extends Fragment implements View.OnClickListener, PluginBase {
    private static Logger log = LoggerFactory.getLogger(LoopFragment.class);

    Button runNowButton;
    TextView lastRunView;
    TextView lastEnactView;
    TextView sourceView;
    TextView requestView;
    TextView constraintsProcessedView;
    TextView setByPumpView;

    public class LastRun implements Parcelable {
        public APSResult request = null;
        public APSResult constraintsProcessed = null;
        public PumpEnactResult setByPump = null;
        public String source = null;
        public Date lastAPSRun = null;
        public Date lastEnact = null;
        public Date lastOpenModeAccept = null;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(request, 0);
            dest.writeParcelable(constraintsProcessed, 0);
            dest.writeParcelable(setByPump, 0);
            dest.writeString(source);
            dest.writeLong(lastAPSRun.getTime());
            dest.writeLong(lastEnact != null ? lastEnact.getTime() : 0l);
            dest.writeLong(lastOpenModeAccept != null ? lastOpenModeAccept.getTime() : 0l);
        }

        public final Parcelable.Creator<LastRun> CREATOR = new Parcelable.Creator<LastRun>() {
            public LastRun createFromParcel(Parcel in) {
                return new LastRun(in);
            }

            public LastRun[] newArray(int size) {
                return new LastRun[size];
            }
        };

        private LastRun(Parcel in) {
            request = in.readParcelable(APSResult.class.getClassLoader());
            constraintsProcessed = in.readParcelable(APSResult.class.getClassLoader());
            setByPump = in.readParcelable(PumpEnactResult.class.getClassLoader());
            source = in.readString();
            lastAPSRun = new Date(in.readLong());
            lastEnact = new Date(in.readLong());
            lastOpenModeAccept = new Date(in.readLong());
        }

        public LastRun() {
        }
    }

    static public LastRun lastRun = null;

    private boolean fragmentEnabled = false;
    private boolean fragmentVisible = true;

    @Override
    public int getType() {
        return PluginBase.LOOP;
    }

    @Override
    public String getName() {
        return MainApp.instance().getString(R.string.loop);
    }

    @Override
    public boolean isEnabled() {
        return fragmentEnabled;
    }

    @Override
    public boolean isVisibleInTabs() {
        return fragmentVisible;
    }

    @Override
    public boolean canBeHidden() {
        return true;
    }

    @Override
    public void setFragmentEnabled(boolean fragmentEnabled) {
        this.fragmentEnabled = fragmentEnabled;
    }

    @Override
    public void setFragmentVisible(boolean fragmentVisible) {
        this.fragmentVisible = fragmentVisible;
    }

    public LoopFragment() {
        super();
        registerBus();
    }

    public static LoopFragment newInstance() {
        LoopFragment fragment = new LoopFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.loop_fragment, container, false);

        lastRunView = (TextView) view.findViewById(R.id.loop_lastrun);
        lastEnactView = (TextView) view.findViewById(R.id.loop_lastenact);
        sourceView = (TextView) view.findViewById(R.id.loop_source);
        requestView = (TextView) view.findViewById(R.id.loop_request);
        constraintsProcessedView = (TextView) view.findViewById(R.id.loop_constraintsprocessed);
        setByPumpView = (TextView) view.findViewById(R.id.loop_setbypump);
        runNowButton = (Button) view.findViewById(R.id.loop_run);
        runNowButton.setOnClickListener(this);

        //if (savedInstanceState != null) {
        //    lastRun = savedInstanceState.getParcelable("lastrun");
        //}
        updateGUI();
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("lastrun", lastRun);
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
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.loop_run:
                invoke(true);
                break;
        }

    }

    @Subscribe
    public void onStatusEvent(final EventTreatmentChange ev) {
        ConstraintsInterface constraintsInterface = MainApp.getConfigBuilder();
        invoke(true);
    }

    @Subscribe
    public void onStatusEvent(final EventNewBG ev) {
        invoke(true);
    }

    public void invoke(boolean allowNotification) {
        ConstraintsInterface constraintsInterface = MainApp.getConfigBuilder();
        if (!constraintsInterface.isLoopEnabled()) {
            clearGUI();
            final Activity activity = getActivity();
            if (activity != null)
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        lastRunView.setText(activity.getString(R.string.loopdisabled));
                    }
                });
            return;
        }
        PumpInterface pumpInterface = MainApp.getConfigBuilder().getActivePump();
        APSResult result = null;

        if (constraintsInterface == null || pumpInterface == null || !isEnabled())
            return;

        APSInterface usedAPS = null;
        ArrayList<PluginBase> apsPlugins = MainActivity.getSpecificPluginsList(PluginBase.APS);
        for (PluginBase p : apsPlugins) {
            APSInterface aps = (APSInterface) p;
            if (!p.isEnabled()) continue;
            aps.invoke();
            result = aps.getLastAPSResult();
            if (result == null) continue;
            if (result.changeRequested) {
                // APS plugin is requesting change, stop processing
                usedAPS = aps;
                break;
            }
        }

        // Check if we have any result
        if (result == null) {
            clearGUI();
            final Activity activity = getActivity();
            if (activity != null)
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        lastRunView.setText(activity.getString(R.string.noapsselected));
                    }
                });
            return;
        }

        // check rate for constrais
        APSResult resultAfterConstraints = result.clone();
        constraintsInterface.applyBasalConstraints(resultAfterConstraints);

        if (lastRun == null) lastRun = new LastRun();
        lastRun.request = result;
        lastRun.constraintsProcessed = resultAfterConstraints;
        lastRun.lastAPSRun = new Date();
        lastRun.source = usedAPS != null ? ((PluginBase) usedAPS).getName() : "";
        lastRun.setByPump = null;

        if (constraintsInterface.isClosedModeEnabled()) {
            if (result.changeRequested) {
                PumpEnactResult applyResult = pumpInterface.applyAPSRequest(resultAfterConstraints);
                if (applyResult.enacted) {
                    lastRun.setByPump = applyResult;
                    lastRun.lastEnact = lastRun.lastAPSRun;
                }
            } else {
                lastRun.setByPump = null;
                lastRun.source = null;
            }
        } else {
            if (result.changeRequested && allowNotification) {
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(MainApp.instance().getApplicationContext());
                builder.setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle(MainApp.resources.getString(R.string.openloop_newsuggestion))
                        .setContentText(resultAfterConstraints.toString())
                        .setAutoCancel(true)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setCategory(Notification.CATEGORY_ALARM)
                        .setVisibility(Notification.VISIBILITY_PUBLIC);

                // Creates an explicit intent for an Activity in your app
                Intent resultIntent = new Intent(MainApp.instance().getApplicationContext(), MainActivity.class);

                // The stack builder object will contain an artificial back stack for the
                // started Activity.
                // This ensures that navigating backward from the Activity leads out of
                // your application to the Home screen.
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(MainApp.instance().getApplicationContext());
                stackBuilder.addParentStack(MainActivity.class);
                // Adds the Intent that starts the Activity to the top of the stack
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(resultPendingIntent);
                builder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});
                NotificationManager mNotificationManager =
                        (NotificationManager) MainApp.instance().getSystemService(Context.NOTIFICATION_SERVICE);
                // mId allows you to update the notification later on.
                mNotificationManager.notify(Constants.notificationID, builder.build());
                MainApp.bus().post(new EventRefreshOpenLoop());
            }
        }

        updateGUI();
        MainApp.getConfigBuilder().uploadDeviceStatus();
    }

    void updateGUI() {
        Activity activity = getActivity();
        if (activity != null)
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (lastRun != null) {
                        requestView.setText(lastRun.request != null ? lastRun.request.toString() : "");
                        constraintsProcessedView.setText(lastRun.constraintsProcessed != null ? lastRun.constraintsProcessed.toString() : "");
                        setByPumpView.setText(lastRun.setByPump != null ? lastRun.setByPump.toString() : "");
                        sourceView.setText(lastRun.source != null ? lastRun.source.toString() : "");
                        lastRunView.setText(lastRun.lastAPSRun != null && lastRun.lastAPSRun.getTime() != 0 ? lastRun.lastAPSRun.toLocaleString() : "");
                        lastEnactView.setText(lastRun.lastEnact != null && lastRun.lastEnact.getTime() != 0 ? lastRun.lastEnact.toLocaleString() : "");
                    }
                }
            });
    }

    void clearGUI() {
        Activity activity = getActivity();
        if (activity != null)
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    requestView.setText("");
                    constraintsProcessedView.setText("");
                    setByPumpView.setText("");
                    sourceView.setText("");
                    lastRunView.setText("");
                    lastEnactView.setText("");
                }
            });
    }
}
