/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.digi.android.wva.adapters.EndpointsAdapter;
import com.digi.android.wva.adapters.LogAdapter;
import com.digi.android.wva.model.EndpointConfiguration;
import com.digi.android.wva.model.LogEvent;
import com.digi.android.wva.util.MessageCourier;
import com.digi.android.wva.util.NetworkUtils;
import com.digi.android.wva.util.VehicleEndpointComparator;
import com.digi.wva.WVA;
import com.digi.wva.async.EventChannelStateListener;
import com.digi.wva.async.WvaCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Set;
/**
 * VehicleInfoService is a self-contained service created to facilitate easy
 *   integration with the Digi Wi-Fi Vehicle Bus Adapter (WVA). It is intended
 *   to be a started service which provides constant information from the WVA
 *   web service.
 *
 *   <p>In this demonstration app, this service is started when the application
 *   is created (in {@link WvaApplication#onCreate}) and by calling startService
 *   with intents containing various commands, the service can be directed to
 *   connect or disconnect from devices. The idea is that the service is always
 *   running, just not necessarily always doing useful things.</p>
 *
 * @author awickert
 *
 */
public class VehicleInfoService extends Service {
	/* Constants to be added as extras in startService calls for the service
	 * to specify what "command" is being passed */
	// INTENT_* constants are the Intent extra names
    /** Intent extra key to indicate the "command" of the intent */
	public static final String INTENT_CMD = "command";
    /** Intent extra key to indicate the IP address to connect to, when the
     * command being used is {@link #CMD_CONNECT}.
     */
	public static final String INTENT_IP = "ip_addr";
	/** Intent extra key to give the basic-auth username to use with this device.
	 */
	public static final String INTENT_AUTH_USER = "auth_user";
	/** Intent extra key to give the basic-auth password to use with this device.
	 */
	public static final String INTENT_AUTH_PASS = "auth_pass";
	/** Intent extra key to indicate whether the HTTP connection should use HTTPS or not.
	 */
	public static final String INTENT_HTTPS = "https";

    // CMD_* are values for INTENT_CMD extras
    /** This command is only used in {@link com.digi.android.wva.WvaApplication#onCreate()},
     * to initialize VehicleInfoService.
     */
	public static final int CMD_APPCREATE = 0; // WvaApplication.onCreate only
    /** Directs VehicleInfoService to attempt to connect to a device at the
     * IP address given by the Intent extra whose key is {@link #INTENT_CMD}.
     */
	public static final int CMD_CONNECT = 1; // start listening to device
    /** Directs VehicleInfoService to disconnect from the device. */
	public static final int CMD_DISCONNECT = 2; // stop listening to anything

    private static final String TAG = "VehicleInfoService";

	private static final int NOTIF_ID = 98866843; // WVANOTIF

    private WVA mDevice;

    private boolean isConnected = false;

	private String connectIp; // IP address to connect to

    private Handler mHandler;

    /**
     * Builds a new EventChannelStateListener specialized for use by the demo app.
     *
     * <p>This method is protected, rather than private, due to a bug between JaCoCo and
     * the Android build tools which causes the instrumented bytecode to be invalid when this
     * method is private:
     * <a href="http://stackoverflow.com/questions/17603192/dalvik-transformation-using-wrong-invoke-opcode" target="_blank">see StackOverflow question.</a>
     * </p>
     * @return a new event channel state listener to use
     */
    protected EventChannelStateListener makeStateListener() {
        return new EventChannelStateListener() {
            @Override
            public boolean runsOnUiThread() {
                return true;
            }

            private void log(final LogEvent event) {
                LogAdapter.getInstance().add(event);
            }

            @Override
            public void onConnected(WVA device) {
                Log.d(TAG, "connectionListener -- onConnected");
                MessageCourier.sendDashConnected(connectIp);

                log(new LogEvent("Connected to device.", null));

                // Ensure the service-running notification goes up.
                isConnected = true;

                showNotificationIfRunning();
            }

            @Override
            public void onError(WVA device, IOException error) {
                Log.e(TAG, "Device connection error", error);

                device.disconnectEventChannel(true);

                log(new LogEvent("An error occurred. Disconnecting...", null));

                String msg;
                if (error == null) {
                    msg = "Connection with the WVA device encountered some error.";
                }
                else if (!NetworkUtils.shouldBeAllowedToConnect(getApplicationContext())) {
                    Log.d(TAG, "Connection error is because the network went away.");
                    msg = "Your network connection has gone away.";
                } else {
                    msg = "Connection with the WVA device encountered an error: " +
                            error.getMessage();
                }
                MessageCourier.sendError(msg);
            }

            @Override
            public void onRemoteClose(WVA device, int port) {
                Log.d(TAG, "connectionListener -- onRemoteClose");

                MessageCourier.sendReconnecting(connectIp);

                log(new LogEvent("Reconnecting...", null));

                // this will interrupt() the EventChannel thread, but since we're
                // inside that thread currently, and we're not doing any thread-blocking
                // calls here, execution will continue until we leave this method.
                // Then, execution returns to EventChannel, and off we go.
                super.onRemoteClose(device, port);
            }

            @Override
            public void onFailedConnection(WVA device, int port) {
                Log.d(TAG, "connectionListener -- onFailedConnection");
                MessageCourier.sendReconnecting(connectIp);
                log(new LogEvent("Retrying connection...", null));
                reconnectAfter(device, 15000, port);
            }
        };
    }

    public VehicleInfoService() {
        connectIp = null;
    }

    @Override
	public void onCreate() {
		Log.d(TAG, "Vehicle service created.");

        WvaApplication app = (WvaApplication)getApplication();
        if (app == null) {
            // This shouldn't happen in any reasonable scenario.
            throw new NullPointerException("Couldn't get application in service!");
        } else {
            mHandler = app.getHandler();
        }
		super.onCreate();
	}

	/**
	 * Depending on if the service is currently set as 'connected'
	 * (check value of {@code isConnected} boolean), either call
	 * {@link #startForeground(int, Notification)} to put up the
	 * "service is running" notification, or call
	 * {@link #stopForeground(boolean) stopForeground(true)} to remove the
	 * notification (because the service isn't listening).
     *
     * <p>This method is protected, rather than private, due to a bug between JaCoCo and
     * the Android build tools which causes the instrumented bytecode to be invalid when this
     * method is private:
     * http://stackoverflow.com/questions/17603192/dalvik-transformation-using-wrong-invoke-opcode
     * </p>
	 */
	protected void showNotificationIfRunning() {
        // First, c
        WvaApplication app = (WvaApplication) getApplication();
        if (app != null && app.isTesting())
            return;

		if (isConnected) {
			NotificationCompat.Builder builder;
			builder = new NotificationCompat.Builder(getApplicationContext());
			PendingIntent contentIntent;
			Intent intent = new Intent(VehicleInfoService.this, DashboardActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
							Intent.FLAG_ACTIVITY_SINGLE_TOP);
			contentIntent = PendingIntent.getActivity(VehicleInfoService.this,
														0, intent, 0);
			builder.setContentTitle("Digi WVA Service")
				   .setContentText("Connected to " + (TextUtils.isEmpty(connectIp) ? "(null)" : connectIp))
				   .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
				   .setSmallIcon(R.drawable.notif_small)
				   .setOngoing(true)
				   .setContentIntent(contentIntent);

			startForeground(NOTIF_ID, builder.build());
		} else {
			try {
				stopForeground(true);
			} catch (Exception e) {
				// Might happen if startForeground not called before
				e.printStackTrace();
			}
		}
	}

	/**
	 * Factory function to create the intent used when startService is called
	 * in {@link WvaApplication#onCreate onCreate}
	 *
	 * @param context Application context (use {@link #getApplicationContext()})
	 * @return intent to be used in startService call
	 */
	public static Intent buildCreateIntent(Context context) {
		// Make new intent for VehicleInfoService with command CMD_APPCREATE
        return new Intent(context, VehicleInfoService.class)
                            .putExtra(INTENT_CMD, CMD_APPCREATE);
	}

	/**
	 * Factory function to create the intent used in a startService call to
	 * tell the {@link VehicleInfoService} to "connect" to the device at
	 * the given IP address.
	 *
	 * @param context Application context (use {@link #getApplicationContext()})
	 * @param ip_addr IP address of device to connect to
	 * @return intent to be used in startService call
	 */
	public static Intent buildConnectIntent(Context context, String ip_addr) {
		return buildConnectIntent(context, ip_addr, null, null, true);
	}
	
	public static Intent buildConnectIntent(Context context, String ip_addr, String auth_user, String auth_pass, boolean useHttps) {
		// Make new intent with command CMD_CONNECT and the IP given
		Intent intent = new Intent(context, VehicleInfoService.class);
		// Add command and the ip address
		intent
			.putExtra(INTENT_CMD, CMD_CONNECT)
			.putExtra(INTENT_IP, ip_addr)
			.putExtra(INTENT_AUTH_USER, auth_user)
			.putExtra(INTENT_AUTH_PASS, auth_pass)
			.putExtra(INTENT_HTTPS, useHttps);

		return intent;
	}

	/**
	 * Factory function to create the intent used in a startService call to
	 * tell the {@link VehicleInfoService} to "disconnect" from whatever
	 * device it's connected to currently
	 *
	 * @param context Application context (use {@link #getApplicationContext()})
	 * @return intent to be used in startService call
	 */
	public static Intent buildDisconnectIntent(Context context) {
        return new Intent(context, VehicleInfoService.class)
                                .putExtra(INTENT_CMD, CMD_DISCONNECT);
	}

	/**
	 * Take the Intent that was used to call startService (i.e. the
	 * intent used to give a command to the service) and the command
	 * that it had as an extra and act on it accordingly.
     *
     * <p>This method is protected, rather than private, due to a bug between JaCoCo and
     * the Android build tools which causes the instrumented bytecode to be invalid when this
     * method is private:
     * http://stackoverflow.com/questions/17603192/dalvik-transformation-using-wrong-invoke-opcode
     * </p>
	 *
	 * @param intent the intent used to call startService
	 * @param command the command that the intent had as an extra
	 */
	protected synchronized void parseIntent(Intent intent, int command) {
		boolean isConnect = false;
        final WvaApplication app = (WvaApplication)getApplication();
        if (app == null) {
            // Based on Android sources, this would only happen if the service is
            // not attached to an application... In this case, we can't know what
            // is a valid thing to do here.
            Log.e(TAG, "getApplication() returned null!");
            return;
        }
        switch (command) {
		case CMD_APPCREATE:
			Log.i(TAG, "startService - CMD_APPCREATE");
			isConnected = false;
			break;
		case CMD_CONNECT:
			Log.i(TAG, "startService - CMD_CONNECT");
			isConnect = true;
			break;
		case CMD_DISCONNECT:
			Log.i(TAG, "startService - CMD_DISCONNECT");
			isConnected = false;
			if (mDevice != null) {
				mDevice.disconnectEventChannel(true);
				mDevice = null;
				app.setDevice(null);
			} else {
				Log.d(TAG, "Got CMD_DISCONNECT but mDevice is null");
			}
			break;
		default:
			Log.i(TAG, "startService - unknown command " + command);
			isConnected = false;
		}

		if (isConnect) {
			String ip = intent.getStringExtra(INTENT_IP);
			String username = intent.getStringExtra(INTENT_AUTH_USER);
			String password = intent.getStringExtra(INTENT_AUTH_PASS);
			boolean useHttps = intent.getBooleanExtra(INTENT_HTTPS, true);
			if (TextUtils.isEmpty(ip)) {
				Log.e(TAG, "startService given connect command with empty IP!");
				isConnected = false;
			}
			else {
				final int port = Integer.valueOf(
						PreferenceManager.getDefaultSharedPreferences(this)
								.getString("pref_device_port", "5000"));
				connectIp = ip;
				isConnected = false;

                boolean autoSubscribe = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_auto_subscribe", false);
				final int autosub = autoSubscribe
                        ? Integer.valueOf(PreferenceManager
                                .getDefaultSharedPreferences(this)
                                .getString("pref_default_interval", "-1"))
                        : -1;
				
				Log.i(TAG, "Initiating connection to " + connectIp);

                // Structuring the code like this allows us to
                // inject Device instances for testing. Otherwise,
                // it would become difficult to unit-test
                // VehicleInfoService.

                mDevice = app.getDevice();
                if (mDevice == null) {
                    mDevice = new WVA(connectIp);
                    
                    // Set up authentication and HTTP(S) configuration.
                    // The demo app assumes default HTTP and HTTPS ports.
                    mDevice.useBasicAuth(username, password)
                    		.useSecureHttp(useHttps)
                    		.setHttpPort(80)
                    		.setHttpsPort(443);
                    
                    app.setDevice(mDevice);
                }

                mDevice.fetchVehicleDataEndpoints(new WvaCallback<Set<String>>() {
					@Override
					public void onResponse(Throwable error, Set<String> endpoints) {
                        Log.d(TAG, "initVehicleData onResponse...");
                        if (error != null) {
                            Log.e(TAG, "Got error starting Vehicle", error);
                            // Stop WVA inner threads (TCPReceiver, MessageHandler)
                            synchronized (VehicleInfoService.this) {
                                mDevice.disconnectEventChannel();
                                mDevice = null;
                                app.setDevice(null);
                            }
                            String err = error.getMessage();
                            if (TextUtils.isEmpty(err)) {
                                Throwable cause = error.getCause();
                                if (cause != null)
                                    err = cause.getMessage();
                                else
                                    err = error.toString();
                            }
                            MessageCourier.sendError(err);
                            return;
                        }
                        
                        // Sort the endpoints set
                        final List<String> sortedEndpoints = VehicleEndpointComparator.asSortedList(endpoints);
                        
                        Log.d(TAG, "Beginning endpoint handling");
                        
                        // First, add them all to the adapter.
                        for (String e : sortedEndpoints) {
                            // To improve performance, we add the endpoint to the endpoints list here,
                            // but do not notify it that the data set has changed. We then notify only
                            // after adding all endpoints.
                        	EndpointsAdapter.getInstance().add(new EndpointConfiguration(e), false);
                        }
                        
                        // Update the endpoints adapter.
                        mHandler.postAtFrontOfQueue(new Runnable() {
							@Override
							public void run() {
								Log.d("VIS", "Updating endpoints adapter");
								EndpointsAdapter.getInstance().notifyDataSetChanged();
							}
						});

                        // Handle subscribing/unsubscribing on a separate thread.
                        if (autosub > 0) {
                            Runnable doSubscriptions = new Runnable() {
                                @Override
                                public void run() {
                                    for (String e : sortedEndpoints) {
                                        // Add a bit of sleep between subscribing
                                        // to each endpoint, so as not to overload
                                        // the main thread as it tries to keep up.
                                        // This is happening as soon as the
                                        // DashboardActivity is launched, after all.
                                        try {
                                            Thread.sleep(25);
                                        } catch (InterruptedException ignored) {
                                        }
            
                                        if (app.getDevice() == null) {
                                            // User backed out of DashboardActivity
                                            // We should stop these subscriptions...
                                            Log.d(TAG, "app.getDevice() returned null. " +
                                                    "Stopping subscriptions...");
                                            app.clearDevice();
                                            return;
                                        }
            
                                        final String ep = e;

                                        boolean isPressurePro = false;
                                        for (String s : VehicleEndpointComparator.PRESSURE_PRO_PREFIXES) {
                                            if (ep.startsWith(s)) {
                                                isPressurePro = true;
                                                break;
                                            }
                                        }

                                        if (!isPressurePro) {
                                            // (Try to) subscribe to the endpoint
                                            app.subscribeToEndpointFromService(e, autosub,
                                            new WvaCallback<Void>() {
                                                @Override
                                                public void onResponse(Throwable error, Void response) {
                                                    String msg;
                                                    if (error != null) {
                                                        msg = "Failed to subscribe to " + ep;
                                                        Log.e(TAG, "Failed to subscribe to " + ep, error);
                                                        final LogEvent evt = new LogEvent(msg, null);
                                                        mHandler.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                LogAdapter.getInstance().add(evt);
                                                            }
                                                        });
                                                    }
                                                }
                                            });
                                        }
                                    }
                                }
                            };
                        
                            // Do subscriptions on this thread when running unit tests,
                            // but on a separate thread when actually being used. This allows
                            // us to test the code in the runnable.
                            if (app.isTesting()) {
                                doSubscriptions.run();
                            } else {
                                new Thread(doSubscriptions).start();
                            }
                        }
                    }
                });

                if (mDevice == null)
                    return;

                // Ensure that the correct state listener is used.
                mDevice.setEventChannelStateListener(makeStateListener());
                mDevice.connectEventChannel(port);

                if (mDevice == null)
                    return;

                // Android Studio warns that getApplicationContext() might
                // return null. So we will check for null in the callbacks.
                final Context toastContext = getApplicationContext();

                JSONObject portJson = new JSONObject();
                try {
                    portJson.put("port", port);
                    portJson.put("enable", "on");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }

                mDevice.configure("ws_events", portJson, new WvaCallback<Void>() {
                    @Override
                    public void onResponse(Throwable error, Void response) {
                        if (error == null) {
                            Log.d(TAG, "Successfully configured port.");
                        } else {
                            Log.d(TAG, "Failed to configure port", error);
                            if (toastContext != null) {
                                Toast.makeText(toastContext, "Failed to set port to " + port,
                                               Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }
		}
	}

	@Override
	public int onStartCommand(Intent intent, int code, int startid){

//		Log.d(TAG, "VIS onStartCommand, got intent? " + (intent != null));
		if (intent == null) {
            // Process was killed and is being restarted, and we previously
            // returned START_STICKY, so this method is getting a null Intent.
            // Since the app was killed we don't know who to talk to, so
            // don't talk to anyone.
            Log.e(TAG, "onStartCommand - null intent");
            isConnected = false;
        } else {
			int command = intent.getIntExtra(INTENT_CMD, -1);
			if (command == -1) {
				Log.e(TAG, "startService called without command");
				isConnected = false;
			} else {
                parseIntent(intent, command);
            }
		}

		showNotificationIfRunning();

		return START_STICKY;
	}

    /**
     * Useful for unit testing, to be able to access the field and
     * find out what it is set to at any given instant.
     *
     * @return the service's current Device reference
     */
    public synchronized WVA getDevice() {
        return mDevice;
    }

    /**
     * Indicate if the VehicleInfoService is currently connected to a
     * WVA device
     * @return true if connected to a WVA device, false otherwise
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Fetch the IP address we last attempted to connect to (whether that attempt
     * was successful or not)
     * @return the last IP address we tried to connect to
     */
    public String getConnectionIpAddress() {
        return connectIp;
    }

	@Override
	public void onDestroy() {
//		Log.d(TAG, "onDestroy");

		// Remove the notification
        WvaApplication app = (WvaApplication)getApplication();
        if (app != null && !app.isTesting())
		    stopForeground(true);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}
