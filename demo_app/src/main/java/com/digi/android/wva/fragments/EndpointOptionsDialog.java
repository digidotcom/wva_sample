/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.digi.android.wva.R;
import com.digi.android.wva.WvaApplication;
import com.digi.android.wva.adapters.EndpointsAdapter;
import com.digi.android.wva.adapters.LogAdapter;
import com.digi.android.wva.model.EndpointConfiguration;
import com.digi.android.wva.model.LogEvent;
import com.digi.wva.async.AlarmType;
import com.digi.wva.async.WvaCallback;

import java.util.Locale;

/**
 * A {@link DialogFragment} subclass which displays a small
 * form which is used to specify the desired subscriptions and alarms for
 * a given endpoint.
 *
 * <p>An important thing to note about the EndpointsOptionDialog, and
 * indeed, the entirety of the sample app's modeling of subscriptions and alarms,
 * is that the app's displayed status of alarms and subscriptions is NEVER
 * guaranteed to accurately reflect the state of alarms and subscriptions on
 * the device. That is to say, if you open an options dialog, attempt to
 * subscribe to an endpoint, and that subscription attempt fails, the options
 * dialog (and its underlying EndpointConfiguration) will still say
 * "subscribed", even though in reality you are not subscribed.</p>
 */
public class EndpointOptionsDialog extends DialogFragment {
	private static final String TAG = "EndpointOptionsDialog";
    private EndpointConfiguration mConfig;
    private Handler mHandler;
	
	private String[] alarmTypes;

    /**
     * Set the {@link EndpointConfiguration} whose status will be displayed
     * in this dialog
     * @param config the {@link EndpointConfiguration} to use
     * @return this, for chained method calls
     */
	public EndpointOptionsDialog setConfig(EndpointConfiguration config) {
		mConfig = config;
		return this;
	}

    /**
     * Add a log message to the log adapter.
     *
     * <p>This method is protected, rather than private, due to a bug between JaCoCo and
     * the Android build tools which causes the instrumented bytecode to be invalid when this
     * method is private:
     * <a href="http://stackoverflow.com/questions/17603192/dalvik-transformation-using-wrong-invoke-opcode" target="_blank">see StackOverflow question.</a>
     * </p>
     * @param message the message to log
     */
    protected void simpleLog(final String message) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                LogAdapter.getInstance().add(new LogEvent(message, null));
            }
        });
    }

    /**
     * <p>This method is protected, rather than private, due to a bug between JaCoCo and
     * the Android build tools which causes the instrumented bytecode to be invalid when this
     * method is private:
     * <a href="http://stackoverflow.com/questions/17603192/dalvik-transformation-using-wrong-invoke-opcode" target="_blank">see StackOverflow question.</a>
     * </p>
     */
    protected WvaCallback<Void> makeWsCallback(final Context context, final String succeedText,
                                               final String failText) {
        return new WvaCallback<Void>() {
            @Override
            public void onResponse(Throwable error, Void response) {
                if (error != null) {
                    Log.e(TAG, failText, error);
                    Toast.makeText(context, failText + ": " + error, Toast.LENGTH_SHORT).show();
                    simpleLog(failText);
                } else {
                    Log.d(TAG, succeedText);
                    Toast.makeText(context, succeedText, Toast.LENGTH_SHORT).show();
                    simpleLog(succeedText);
                }
            }
        };
    }

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// getResources() must be called after the fragment has been
		// attached to an activity.
		alarmTypes = getResources().getStringArray(R.array.alarm_types);

        WvaApplication app = (WvaApplication)activity.getApplication();
        mHandler = app.getHandler();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		if (mConfig == null) {
			// Why this would happen, I don't know. But okay...
			return;
		}
		
		outState.putParcelable("config", mConfig);
	}
	
	protected void subscribe(String endpoint, int interval) {
        ((WvaApplication) getActivity().getApplication())
                .subscribeToEndpoint(endpoint, interval,
                        makeWsCallback(getActivity().getApplicationContext(), "Subscribed to " + endpoint,
                                "Failed to subscribe to " + endpoint));
    }
	
	protected void unsubscribe(String endpoint) {
        ((WvaApplication)getActivity().getApplication())
                .unsubscribe(endpoint,
                        makeWsCallback(getActivity().getApplicationContext(), "Unsubscribed from " + endpoint,
                                "Failed to unsubscribe from " + endpoint));
	}
	
	protected void createAlarm(String endpoint, AlarmType type, double threshold) {
		((WvaApplication)getActivity().getApplication())
                .createAlarm(endpoint, type, threshold, 10,
                        makeWsCallback(getActivity().getApplicationContext(), "Created alarm for " + endpoint,
                                "Failed to create alarm for " + endpoint));
	}
	
	protected void removeAlarm(String endpoint, AlarmType type) {
        ((WvaApplication)getActivity().getApplication())
                .removeAlarm(endpoint, type,
                        makeWsCallback(getActivity().getApplicationContext(), "Removed alarm from " + endpoint,
                                "Failed to remove alarm from " + endpoint));
	}
	
	protected boolean shouldDisableAlarmThreshold(int typePos) {
        return alarmTypes != null && (typePos < 0 || alarmTypes[typePos].equals("Change"));
    }

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (mConfig == null && savedInstanceState == null) {
			Log.e(TAG, "mConfig is null, not showing dialog!");
			return null;
		}
		
		LayoutInflater inf = getActivity().getLayoutInflater();
		View v = inf.inflate(R.layout.dialog_endpoint_options, null);

        // Suppresses warnings, and ensures the layout exists.
        assert v != null;
        final TextView subIntervalTV = (TextView)v.findViewById(R.id.textView_interval);
        final TextView alarmInfoTV = (TextView) v.findViewById(R.id.alarm_info);
        final CheckBox subscribedCB = (CheckBox)v.findViewById(R.id.subscribedCheckbox);
		final CheckBox alarmCB = (CheckBox)v.findViewById(R.id.alarmCheckbox);
		final EditText subInterval = (EditText)v.findViewById(R.id.subscriptionInterval);
		final EditText alarmThreshold = (EditText)v.findViewById(R.id.alarmThreshold);
		final Spinner typeSpinner = (Spinner)v.findViewById(R.id.alarmTypeSpinner);
        final LinearLayout makeAlarmSection = (LinearLayout)v.findViewById(R.id.section_make_alarm);
        final LinearLayout showAlarmSection = (LinearLayout) v.findViewById(R.id.section_show_alarm);
		//final CheckBox dcSendCB = (CheckBox)v.findViewById(R.id.dcPushCheckbox);

        String alarmInfo = "No alarm yet";
        boolean isSubscribed = false;
		String endpointName = "UNKNOWN";
		int sinterval = 10;
		boolean alarmCreated = false;
		double threshold = 0;
		int alarmtypeidx = 0;
		
		boolean isSendingToDC = false;
		
		if (savedInstanceState != null && savedInstanceState.containsKey("config")) {
			mConfig = savedInstanceState.getParcelable("config");
		}
		
		if (mConfig != null) {
			endpointName = mConfig.getEndpoint();
            alarmInfo = mConfig.getAlarmSummary();

			if (mConfig.getSubscriptionConfig() != null) {
				isSubscribed = mConfig.getSubscriptionConfig().isSubscribed();
				sinterval = mConfig.getSubscriptionConfig().getInterval();
				isSendingToDC = mConfig.shouldBePushedToDeviceCloud();
			} else {
                // Not subscribed; default interval value from preferences.
                String i = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("pref_default_interval", "0");
                try {
                    sinterval = Integer.parseInt(i);
                } catch (NumberFormatException e) {
                    Log.d(TAG, "Failed to parse default interval from preferences: " + i);
                    sinterval = 0;
                }
            }

			if (mConfig.getAlarmConfig() != null) {
				alarmCreated = mConfig.getAlarmConfig().isCreated();
				threshold = mConfig.getAlarmConfig().getThreshold();
				String typestr = AlarmType.makeString(mConfig.getAlarmConfig().getType());
				for (int i = 0; i < alarmTypes.length; i++) {
					if (alarmTypes[i].toLowerCase(Locale.US).equals(typestr))
						alarmtypeidx = i;
				}
			}
		}

		// Set up event listeners on EditText and CheckBox items
		
		subscribedCB.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				subInterval.setEnabled(isChecked);
                subIntervalTV.setEnabled(isChecked);
			}
		});

		alarmCB.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				typeSpinner.setEnabled(isChecked);
				alarmThreshold.setEnabled(false);
				// If type spinner is set to Change, we want threshold disabled again
				if (isChecked) {
					alarmThreshold.setEnabled(
							!shouldDisableAlarmThreshold(
									typeSpinner.getSelectedItemPosition()));
				}
			}
		});
		
		typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long id) {
				if (alarmCB.isChecked() && shouldDisableAlarmThreshold(position))
					alarmThreshold.setEnabled(false);
				else if (!alarmCB.isChecked())
					alarmThreshold.setEnabled(false);
				else
					alarmThreshold.setEnabled(true);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

        subIntervalTV.setEnabled(false);
		subInterval.setEnabled(false);
		alarmThreshold.setEnabled(false);
		typeSpinner.setEnabled(false);
        alarmInfoTV.setText(alarmInfo);

		// Click checkboxes, show data depending on if subscription or alarm
		// has been added already
		if (isSubscribed)
			subscribedCB.performClick();
		if (alarmCreated) {
            showAlarmSection.setVisibility(View.VISIBLE);
            makeAlarmSection.setVisibility(View.GONE);
            alarmCB.setText("Remove alarm");
        } else {
            makeAlarmSection.setVisibility(View.VISIBLE);
            showAlarmSection.setVisibility(View.GONE);
            alarmCB.setText("Create alarm");
        }
		
		//dcSendCB.setChecked(isSendingToDC);
		
		subInterval.setText(Integer.toString(sinterval));
		
		alarmThreshold.setText(Double.toString(threshold));

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
										getActivity(), R.array.alarm_types,
										android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		typeSpinner.setAdapter(adapter);
		typeSpinner.setSelection(alarmtypeidx);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
            	// Fetch the EndpointsAdapter's configuration for this endpoint.
            	// (We might have gotten mConfig from the saved instance bundle)
            	EndpointConfiguration cfg = EndpointsAdapter.getInstance().findEndpointConfiguration(mConfig.getEndpoint());
            	
            	// Set whether this endpoint's data should be pushed to Device Cloud

                if (cfg != null) {
                    //cfg.setPushToDeviceCloud(dcSendCB.isChecked());
                    cfg.setPushToDeviceCloud(false);
                }

            	// Handle (un)subscribing
            	
                if (isUnsubscribing(subscribedCB.isChecked())) {
                    unsubscribe(mConfig.getEndpoint());
                } else if (subscribedCB.isChecked()) {
                    if (handleMakingSubscription(subInterval)) {
                        // Subscription was successful... most likely.
                        Log.d(TAG, "Probably subscribed to endpoint.");
                    } else {
                        // Invalid interval.
                        Toast.makeText(getActivity(),
                                getString(R.string.configure_endpoints_toast_invalid_sub_interval),
                                Toast.LENGTH_SHORT).show();
                    }
                }
                
                // Handle adding/removing alarm as necessary

                if (isRemovingAlarm(alarmCB.isChecked())) {
                    removeAlarm(mConfig.getEndpoint(), mConfig.getAlarmConfig().getType());
                } else if (alarmCB.isChecked()) {
                    Editable thresholdText = alarmThreshold.getText();
                    String thresholdString;
                    if (thresholdText == null)
                        thresholdString = "";
                    else
                        thresholdString = thresholdText.toString();

                    double threshold;
                    try {
                        threshold = Double.parseDouble(thresholdString);
                    }catch (NumberFormatException e) {
                        Toast.makeText(getActivity(),
                                getString(R.string.configure_endpoints_invalid_threshold),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int alarmidx = typeSpinner.getSelectedItemPosition();
                    if (alarmidx == -1) {
                        // But... how?
                        Log.wtf(TAG, "alarm type index -1 ?");
                        return;
                    }
                    String type = alarmTypes[alarmidx];
                    AlarmType atype = AlarmType.fromString(type);

                    createAlarm(mConfig.getEndpoint(), atype, threshold);
                }

                dialog.dismiss();
            }
        };

        return new AlertDialog.Builder(getActivity())
                            .setView(v)
                            .setTitle("Endpoint: " + endpointName)
                            .setPositiveButton("Save", clickListener)
                            .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Cancel means just dismiss the dialog.
                                        dialog.dismiss();
                                    }
                                }
                            ).create();
	}
	
	protected boolean isUnsubscribing(boolean checked) {
		return (mConfig.isSubscribed() && !checked);
	}
	
	protected boolean isRemovingAlarm(boolean checked) {
		return (mConfig.hasCreatedAlarm() && checked);
	}
	
	protected boolean isSubscribing() {
		return (!mConfig.isSubscribed());
	}
	
	protected boolean subscriptionIntervalChanged(int newinterval) {
		return mConfig.isSubscribed() &&
				(mConfig.getSubscriptionConfig().getInterval() != newinterval);
	}

    /**
     * @param subInterval EditText holding user input for interval
     * @return true if subscription was valid (and probably worked), false
     * if the interval is invalid
     */
    protected boolean handleMakingSubscription(EditText subInterval) {
        // Shouldn't need to worry about NumberFormatException -
        // the EditText is set to type numeric
        Editable intervalText = subInterval.getText();
        String interval;
        if (intervalText == null)
            interval = "";
        else
            interval = intervalText.toString();
        if (TextUtils.isEmpty(interval) || !TextUtils.isDigitsOnly(interval)) {
            return false;
        }

        int iinterval;
        try {
            iinterval = Integer.valueOf(interval);
        } catch (NumberFormatException e) {
            return false;
        }

        if (isSubscribing() || subscriptionIntervalChanged(iinterval)) {
            subscribe(mConfig.getEndpoint(), iinterval);
        }
        return true;
    }
}
