/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.TextUtils;
import android.util.Log;

import com.digi.addp.AddpClient;
import com.digi.android.wva.adapters.EndpointsAdapter;
import com.digi.android.wva.adapters.LogAdapter;
import com.digi.android.wva.adapters.VariableAdapter;
import com.digi.android.wva.model.EndpointConfiguration;
import com.digi.android.wva.model.EndpointConfiguration.AlarmConfig;
import com.digi.android.wva.model.EndpointConfiguration.SubscriptionConfig;
import com.digi.android.wva.model.VehicleData;
import com.digi.android.wva.util.MessageCourier;
import com.digi.android.wva.util.VehicleDataList;
import com.digi.connector.android.library.core.CloudConnectorManager;
import com.digi.connector.android.library.models.Sample;
import com.digi.wva.WVA;
import com.digi.wva.async.AlarmType;
import com.digi.wva.async.EventFactory;
import com.digi.wva.async.VehicleDataEvent;
import com.digi.wva.async.VehicleDataListener;
import com.digi.wva.async.VehicleDataResponse;
import com.digi.wva.async.WvaCallback;

import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.List;

/**
 * Custom {@link Application} object to provide global variables and
 * context across the app.
 * 
 * <p>First and foremost, {@link WvaApplication}
 * creates the singleton {@link LogAdapter}, {@link VehicleDataList},
 * {@link VariableAdapter}, and {@link EndpointsAdapter}
 * objects used across the application, and also starts up the
 * {@link VehicleInfoService} service.</p>
 * 
 * @author mwadsten
 *
 */
public class WvaApplication extends Application {
	/** Notification ID used by alarm notifications.
	 * <p><b>0x98225216</b> is how you would type "WVAALARM" on
	 * a keypad.</p> */
	private static final int ALARM_NOTIF_ID = 0x98225216; // WVAALARM
	private static final String TAG = "WvaApplication";
    private static String appVersion;
	
	private final Handler mHandler = new Handler(Looper.getMainLooper());
	
	private WVA mDevice;
	private AddpClient addpClient;
	
	private CloudConnectorManager mCloudConnectorManager;

    public Handler getHandler() {
        // This method seems to not work when running unit tests.
        if (isTesting())
            return new Handler(Looper.getMainLooper());
        return mHandler;
    }

    /**
     * Set the ADDP client to be used in the application for device
     * discovery.
     *
     * <p>(Useful in testing, as one can use this method to set the
     * ADDP client to be a mock implementation, and use that for testing.)</p>
     * @param client {@link AddpClient} to use
     */
	public void setAddpClient(AddpClient client) {
		this.addpClient = client;
	}

    /**
     * Fetch the ADDP client to be used for discovery
     * @return the current ADDP client to use, or null if it has not been set
     */
	public AddpClient getAddpClient() {
		return addpClient;
	}
	
	public CloudConnectorManager getCloudConnector() {
		return mCloudConnectorManager;
	}

    public String getApplicationVersion() {
        return appVersion;
    }
	
	// Every subscription listener will have the exact same behavior, so there's
	// no real reason to create new VehicleDataListener instances per subscription when
	// we can route all data through a single "static" listener.
	// (I say "static" because this listener is not static as far as the
	// keyword goes, but it is declared final...)
	//-----
	// Another benefit of routing all listener callbacks through here is that
	// we can easily tie receipt of subscription data to arbitrary actions,
	// like notifying the data chart activity of new data.
	private final VehicleDataListener dataListener = new VehicleDataListener() {
		// TODO get definite endpoints names for these
		private final List<String> graphingEndpoints = Arrays.asList("VehicleSpeed", "EngineSpeed");

        @Override
        public boolean runsOnUiThread() {
            // This override is not strictly necessary, but it's good to be explicit.
            return true;
        }

		@Override
		public void onEvent(VehicleDataEvent event) {
			// "endpoint" is made final so it can be used in the new Runnable below
            final String endpoint = event.getEndpoint();

//			Log.d("WVAApplication", "Listener cb for endpoint " + endpoint);
            VehicleDataResponse response = event.getResponse();

			Double value = response.getValue();
			DateTime time = response.getTime();
			
			final VehicleData newData =
					new VehicleData(endpoint, value, time);

            if (event.getType() == EventFactory.Type.SUBSCRIPTION) {
                Log.v(TAG, "New data: " + endpoint + "=" + value
                        + " @ " + time.toString());

                // Add the new data to the variable adapter.
                VariableAdapter.getInstance().add(newData);

                // If this newly received data point is one of the "graphable"
                // endpoints (i.e. one of those displayed when the user chooses
                // to see the graph), send the data out on the local broadcast
                // so the graph activity can pick it up, if it is live.
                if (graphingEndpoints.contains(endpoint)) {
                    MessageCourier.sendChartNewData(newData);
                }

                // Fetch EndpointConfiguration for this endpoint, check if it should be pushed
                // to Device Cloud, and send the sample if need be.
                EndpointConfiguration cfg = EndpointsAdapter.getInstance().findEndpointConfiguration(endpoint);

                // Skip sending to Device Cloud if no EndpointConfiguration exists, or if the user has not subscribed
                // to this endpoint.
                if (cfg == null || !cfg.isSubscribed()) {
                    return;
                }
                else if (cfg.shouldBePushedToDeviceCloud()) {
                    Sample s = new Sample("wva", endpoint, String.valueOf(value));
                    mCloudConnectorManager.sendSample("upload.xml", s);
                }
            } else if (event.getType() == EventFactory.Type.ALARM) {
                Log.v("WVAApplication", "Alarm triggered by " + endpoint);

                // Log an event saying the alarm went off
                LogAdapter.getInstance().alarmTriggered(newData);

                showAlarmNotification(endpoint, newData);
            }
		}
	};
	
	//==========================================================================
	// Methods not directly related to WVALib interactivity.
	
	@Override
	public void onCreate() {
		super.onCreate();

        // Get app version from package manager, hold onto it locally
        final String UNAVAILABLE = "N/A";

        String pkgName = getPackageName();
        String versionName = UNAVAILABLE;

        try {
            PackageManager pm = getPackageManager();
            if (pm == null)
                Log.e(TAG, "Can't get app version; package manager is null");
            else {
                PackageInfo pi = pm.getPackageInfo(pkgName, 0);
                if (pi != null)
                    versionName = pi.versionName;
                else
                    Log.e(TAG, "Couldn't get app version: no package info");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Couldn't get app version: NameNotFoundException");
        }

        appVersion = versionName;
		
		// Initialize global singleton objects.
		createSingletons();

        // "Start" the VehicleInfoService
		startService(VehicleInfoService.buildCreateIntent(this));
		
		mCloudConnectorManager = new CloudConnectorManager(this);
	}
	
	void createSingletons() {
		LogAdapter.initInstance(this);
		
		VehicleDataList.initInstance();
		
		VariableAdapter.initInstance(this,
                VehicleDataList.getInstance());
		
		EndpointsAdapter.initInstance(this);
	}
	
	//==========================================================================
	// Methods related to WVALib interactivity (manipulating the WVA object,
	// handling alarms, subscribing to endpoints, etc.)

    /**
     * Fetch the {@link WVA} object currently used for connection to a
     * WVA device
     * @return the currently-used WVA, or null if no device connection is
     * active
     */
	public WVA getDevice() {
		return mDevice;
	}

    /**
     * Set a reference to the currently active {@link WVA} object
     * @param dev the WVA object in use
     */
	public void setDevice(WVA dev) {
		mDevice = dev;
	}

    /**
     * (For testing.) Fetch the {@link VehicleDataListener} used as a listener for
     * new subscription data by the application.
     * @return the subscriptions listener for the app
     */
    public VehicleDataListener getDataListener() {
        return dataListener;
    }

    /**
     * Ensures that the {@link WVA} object reference held by the app is nullified.
     */
    public void clearDevice() {
		if (mDevice == null) return;
//		mDevice.vehicle.removeAllCallbacks();
		mDevice = null;  // drop reference to the vehicle
	}

    /**
     * Subscribe (asynchronously) to the given endpoint with the given subscription interval.
     * Same as {@link #subscribeToEndpoint(String, int, WvaCallback)}, except that it doesn't
     * notify the EndpointsAdapter, to improve performance on initial load.
     * 
     * @see {@link #subscribeToEndpoint(String, int, WvaCallback)}.
     */
    public void subscribeToEndpointFromService(String endpoint, int interval, WvaCallback<Void> callback) {
    	this.subscribeToEndpoint(endpoint, interval, callback, false);
    }
    
    /**
     * Subscribe (asynchronously) to the given endpoint with the given
     * subscription interval.
     *
     * <p><b>Note:</b> This method has no way of giving direct feedback to
     * the caller that the subscription call succeeded or failed.</p>
     * @param endpoint name of the endpoint to subscribe to
     * @param interval time interval to receive subscription data
     * @param callback {@link WvaCallback} to be invoked when the subscription
     *                 web-services call goes through (or fails)
     */
    public void subscribeToEndpoint(final String endpoint,
    								final int interval,
    								final WvaCallback<Void> callback) {
    	this.subscribeToEndpoint(endpoint, interval, callback, true);
    }

    /**
     * Implementation behind {@link #subscribeToEndpoint(String, int, WvaCallback)} and
     * {@link #subscribeToEndpointFromService(String, int, WvaCallback)}.
     *
     * <p>This method is protected, rather than private, due to a bug between JaCoCo and
     * the Android build tools which causes the instrumented bytecode to be invalid when this
     * method is private:
     * http://stackoverflow.com/questions/17603192/dalvik-transformation-using-wrong-invoke-opcode
     * </p>
     */
	protected void subscribeToEndpoint(final String endpoint,
                                      final int interval,
                                      final WvaCallback<Void> callback,
                                      final boolean notify) {
		if (mDevice == null) {
			Log.e(TAG, "addSubscriptionToEndpoint - mDevice is null");
            callback.onResponse(new NullPointerException("No device."), null);
            return;
		}

        // Ensure that the correct vehicle data listener is being used.
        mDevice.setVehicleDataListener(dataListener);

        mDevice.subscribeToVehicleData(endpoint, interval, callback);
		
		boolean needsToBeAdded = false;
		SubscriptionConfig subconf = new SubscriptionConfig(interval);
		subconf.setSubscribed(true);
		EndpointConfiguration ept = EndpointsAdapter.getInstance().findEndpointConfiguration(endpoint);
		if (ept == null) {
			needsToBeAdded = true;
			ept = new EndpointConfiguration(endpoint);
		}
		ept.setSubscriptionConfig(subconf);
		final EndpointConfiguration ec = ept;
		final boolean needToAdd = needsToBeAdded;
		
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				EndpointsAdapter epts = EndpointsAdapter.getInstance();
				if (needToAdd) {
					// This will call notifyDataSetChanged once the new
					// configuration is added to the adapter.
					epts.add(ec, notify);
				} else if (notify) {
					// We modified the subscription setup, so we need to
					// call notifyDataSetChanged to ensure the new information
					// is reflected in the list view.
					epts.notifyDataSetChanged();
				}
			}
		});

    }

    /**
     * Add a new, empty {@link EndpointConfiguration} to the
     * {@link EndpointsAdapter}.
     * @param endpoint name of the endpoint to add to the list
     */
	public void listNewEndpoint(String endpoint) {
		listNewEndpoint(endpoint, false);
	}

    /**
     * Add a new, empty {@link EndpointConfiguration} to the
     * {@link EndpointsAdapter}.
     * @param endpoint name of the endpoint to add to the list
     * @param notify set <b>true</b> to notify the adapter that the data set has changed.
     * This is set to <b>false</b> in {@link VehicleInfoService} to improve performance on initial
     * endpoint list loading.
     */
	public void listNewEndpoint(String endpoint, final boolean notify) {
		final EndpointConfiguration conf = new EndpointConfiguration(endpoint);
		
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				EndpointsAdapter.getInstance().add(conf, notify);
			}
		});
	}
	
	/**
	 * Wrapper around calling EndpointsAdapter.notifyDataSetChanged
	 * on the main thread.
	 */
    void notifyEndpointsChanged() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				EndpointsAdapter.getInstance().notifyDataSetChanged();
			}
		});
	}

    /**
     * Unsubscribe (asynchronously) from the given endpoint.
     *
     * <p><b>Note:</b> This method has no way of giving direct feedback to
     * the caller that the subscription call succeeded or failed.</p>
     * @param endpoint endpoint to unsubscribe from
     * @param callback {@link WvaCallback} to be invoked when the unsubscribe
     *                 web-services call goes through (or fails)
     */
	public void unsubscribe(final String endpoint, final WvaCallback<Void> callback) {
		if (mDevice == null) {
			Log.e(TAG, "unsubscribe called when device was null");
            callback.onResponse(new NullPointerException("No device"), null);
			return;
		}
        mDevice.unsubscribeFromVehicleData(endpoint, callback);
		
		final EndpointConfiguration conf =
				EndpointsAdapter.getInstance().findEndpointConfiguration(endpoint);
		if (conf != null) {
			conf.setSubscriptionConfig(null);
			notifyEndpointsChanged();
		}

        // Refresh vehicle data list
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                VariableAdapter.getInstance().notifyDataSetChanged();
            }
        });
	}

    /**
     * Create a new alarm for data from the given endpoint, with the alarm
     * specified by the other parameters.
     *
     * @param endpoint data endpoint to set up an alarm for
     * @param type type of alarm (above, below, etc. See {@link AlarmType})
     * @param interval alarm interval
     * @param threshold threshold for alarm
     * @param callback {@link WvaCallback} to be invoked when the alarm creation
     *                 web-services call goes through (or fails)
     */
	public void createAlarm(final String endpoint, AlarmType type,
                            double threshold, int interval,
                            final WvaCallback<Void> callback) {
		if (mDevice == null) {
			Log.e(TAG, "Could not create alarm; no associated device!");
            callback.onResponse(new NullPointerException("No device"), null);
            return;
		}
		
//		Log.i("WVAApplication", "Creating alarm on " + endpoint);

        // Ensure that the correct vehicle data listener is being used.
        mDevice.setVehicleDataListener(dataListener);

        mDevice.createVehicleDataAlarm(endpoint, type, (float) threshold, interval, callback);
		
		boolean needToAdd = false;
		EndpointConfiguration c = EndpointsAdapter.getInstance().findEndpointConfiguration(endpoint);
		if (c == null) {
			Log.d(TAG, "Creating new endpoint configuration");
			c = new EndpointConfiguration(endpoint);
			needToAdd = true;
		}

		AlarmConfig ac = new AlarmConfig(type, threshold, interval);
		ac.setCreated(true);
		c.setAlarmConfig(ac);

		final EndpointConfiguration conf = c;
		final boolean needToAddFinal = needToAdd;
		
		// Only if vehicle.createAlarm worked
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				EndpointsAdapter adapter = EndpointsAdapter.getInstance();
				if (!needToAddFinal) {
					// We still need to call notifyDataSetChanged
					adapter.notifyDataSetChanged();
				}
				else {
					// This calls notifyDataSetChanged
					adapter.add(conf);
				}
			}
		});

    }

    /**
     * Delete any alarms matching the arguments (e.g. "EngineSpeed" and
     * ABOVE) from the WVA.
     * @param endpoint endpoint name whose data is evaluated for alarms
     * @param type type of alarm that needs to be deleted
     * @param callback {@link WvaCallback} to be invoked when the alarm removal
     *                 web-services call goes through (or fails)
     */
	public void removeAlarm(String endpoint, AlarmType type, final WvaCallback<Void> callback) {
		Log.d(TAG, "removeAlarm " + endpoint + AlarmType.makeString(type));
		if (mDevice == null) {
			Log.e(TAG, "Cannot remove alarm; no associated device!");
            callback.onResponse(new NullPointerException("No device"), null);
			return;
		}
		mDevice.deleteVehicleDataAlarm(endpoint, type, callback);
		
		final EndpointConfiguration conf =
				EndpointsAdapter.getInstance().findEndpointConfiguration(endpoint);
		
		if (conf != null) {
			// We have an alarm configuration to "forget about"
			conf.setAlarmConfig(null);
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					EndpointsAdapter.getInstance().notifyDataSetChanged();
				}
			});
		}
	}
	
	/**
	 * Display a status bar notification about the alarm.
	 * 
	 * @param alarmName name of alarm
	 * @param data VehicleData with data that triggered the alarm
	 */
    void showAlarmNotification(String alarmName, VehicleData data) {
		NotificationManager nm = (NotificationManager)
				getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationCompat.Builder builder = new Builder(this);
		Intent activityIntent = new Intent(this, DashboardActivity.class);
		activityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				 				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		// Get alarm sound out of preferences
		SharedPreferences sp = PreferenceManager
								.getDefaultSharedPreferences(this);
		String ringtoneStr = sp.getString("pref_key_alarm_tone", null);
		
		// Make items passed in to builder methods
		Bitmap largeIcon = BitmapFactory.decodeResource(
							getResources(), R.drawable.ic_launcher);
		Uri ringtone = TextUtils.isEmpty(ringtoneStr) ? null : Uri.parse(ringtoneStr);
		PendingIntent pend = PendingIntent.getActivity(
									this, 0, activityIntent, 0);
		
		// Build the notification and show it
		builder.setLargeIcon(largeIcon)
				.setSmallIcon(R.drawable.notif_small)
				.setContentTitle("WVA Alarm: " + alarmName)
				.setContentText("Value: " + data.value)
				.setWhen(System.currentTimeMillis())
				.setAutoCancel(true)
				.setSound(ringtone)
				.setContentIntent(pend)
				.setTicker("WVA Alarm: " + alarmName);
		nm.notify(ALARM_NOTIF_ID, builder.build());
	}
	
	/**
	 * Dismiss the alarm notification from the status bar, if it's there.
	 * Essentially the opposite of calling
	 * {@link #showAlarmNotification(String, VehicleData)}
	 */
	public void dismissAlarmNotification() {
		NotificationManager nm = (NotificationManager)
				getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(ALARM_NOTIF_ID);
	}

    /**
     * Indicated whether the WvaApplication is being used as part of
     * unit testing, or not. The main usage of this information is
     * to prevent things like notifications from popping up (as they
     * are not going to be tested and only complicate things. Also,
     * calling start/stopForeground from VehicleInfoService under
     * unit testing causes NullPointerExceptions inside Android.
     *
     * <p>This method is meant to be mocked to return true when testing.</p>
     * @return true if the application is being unit-tested as opposed to
     * running in a production environment
     */
    public boolean isTesting() {
        return false;
    }
}
