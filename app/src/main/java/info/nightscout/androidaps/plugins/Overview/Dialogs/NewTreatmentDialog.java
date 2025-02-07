package info.nightscout.androidaps.plugins.Overview.Dialogs;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;

import info.nightscout.androidaps.Constants;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.PumpEnactResult;
import info.nightscout.androidaps.interfaces.PumpInterface;
import info.nightscout.utils.PlusMinusEditText;
import info.nightscout.utils.SafeParse;

public class NewTreatmentDialog extends DialogFragment implements OnClickListener {

    Button deliverButton;
    TextView insulin;
    TextView carbs;

    PlusMinusEditText editCarbs;
    PlusMinusEditText editInsulin;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.overview_newtreatment_fragment, null, false);

        deliverButton = (Button) view.findViewById(R.id.treatments_newtreatment_deliverbutton);

        deliverButton.setOnClickListener(this);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        insulin = (TextView) view.findViewById(R.id.treatments_newtreatment_insulinamount);
        carbs = (TextView) view.findViewById(R.id.treatments_newtreatment_carbsamount);

        Integer maxCarbs = MainApp.getConfigBuilder().applyCarbsConstraints(Constants.carbsOnlyForCheckLimit);
        Double maxInsulin = MainApp.getConfigBuilder().applyBolusConstraints(Constants.bolusOnlyForCheckLimit);

        editCarbs = new PlusMinusEditText(view, R.id.treatments_newtreatment_carbsamount, R.id.treatments_newtreatment_carbsamount_plus, R.id.treatments_newtreatment_carbsamount_minus, 0d, 0d, (double) maxCarbs, 1d, new DecimalFormat("0"), false);
        editInsulin = new PlusMinusEditText(view, R.id.treatments_newtreatment_insulinamount, R.id.treatments_newtreatment_insulinamount_plus, R.id.treatments_newtreatment_insulinamount_minus, 0d, 0d, maxInsulin, 0.05d, new DecimalFormat("0.00"), false);

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.treatments_newtreatment_deliverbutton:

                try {
                    Double insulin = SafeParse.stringToDouble(this.insulin.getText().toString());
                    Integer carbs = SafeParse.stringToInt(this.carbs.getText().toString());

                    String confirmMessage = getString(R.string.entertreatmentquestion) + "\n";

                    Double insulinAfterConstraints = MainApp.getConfigBuilder().applyBolusConstraints(insulin);
                    Integer carbsAfterConstraints = MainApp.getConfigBuilder().applyCarbsConstraints(carbs);

                    confirmMessage += getString(R.string.bolus) + ": " + insulinAfterConstraints + "U";
                    confirmMessage += "\n" + getString(R.string.carbs) + ": " + carbsAfterConstraints + "g";
                    if (insulinAfterConstraints - insulin  != 0 || carbsAfterConstraints != carbs)
                        confirmMessage += "\n" + getString(R.string.constraintapllied);

                    final Double finalInsulinAfterConstraints = insulinAfterConstraints;
                    final Integer finalCarbsAfterConstraints = carbsAfterConstraints;

                    AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
                    builder.setTitle(this.getContext().getString(R.string.confirmation));
                    builder.setMessage(confirmMessage);
                    builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (finalInsulinAfterConstraints > 0 || finalCarbsAfterConstraints > 0) {
                                PumpInterface pump = MainApp.getConfigBuilder().getActivePump();
                                PumpEnactResult result = pump.deliverTreatment(finalInsulinAfterConstraints, finalCarbsAfterConstraints);
                                if (!result.success) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                    builder.setTitle(getContext().getString(R.string.treatmentdeliveryerror));
                                    builder.setMessage(result.comment);
                                    builder.setPositiveButton(getContext().getString(R.string.ok), null);
                                    builder.show();
                                }
                            }
                        }
                    });
                    builder.setNegativeButton(getString(R.string.cancel), null);
                    builder.show();
                    dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }

    }

}