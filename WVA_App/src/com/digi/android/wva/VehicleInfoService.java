/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
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
import com.digi.android.wva.adapters.LogAdapter;
import com.digi.android.wva.model.LogEvent;
import com.digi.android.wva.util.MessageCourier;
import com.digi.android.wva.util.NetworkUtils;
import com.digi.wva.async.WvaCallback;
import com.digi.wva.device.Device;
import com.digi.wva.device.DeviceConnectionListener;

import java.io.IOException;
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
     * nError
     * j
     */
	public static final String INTENT_IP = "ip_addr";
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

    private Device mDevice;

    private boolean isConnected = false;

	private String connectIp; // IP address to connect to

    private Handler mHandler;

    private final DeviceConnectionListener connectionListener = new DeviceConnectionListener() {
        private void log(final LogEvent event) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    LogAdapter.getInstance().add(event);
                }
            });
        }

        @Override
        public void onConnected(Device device) {
            Log.d(TAG, "connectionListener -- onConnected");
            MessageCourier.sendDashConnected(connectIp);

            log(new LogEvent("Connected to device.", null));

            // Ensure the service-running notification goes up.
            isConnected = true;

            showNotificationIfRunning();
        }

        @Override
        public void onError(Device device, IOException error) {
            Log.e(TAG, "Device connection error", error);

            device.disconnectDataStream();

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
        public void onRemoteClose(Device device, int port) {
            Log.d(TAG, "connectionListener -- onRemoteClose");

            MessageCourier.sendReconnecting(connectIp);

            log(new LogEvent("Reconnecting...", null));

            // this will interrupt() the TCPReceiver thread, but since we're
            // inside that thread currently, and we're not doing any thread-blocking
            // calls here, execution will continue until we leave this method.
            // Then, execution returns to TCPReceiver, and off we go.
            super.onRemoteClose(device, port);
        }

        @Override
        public void onFailedConnection(Device device, int port) {
            Log.d(TAG, "connectionListener -- onFailedConnection");
            MessageCourier.sendReconnecting(connectIp);
            log(new LogEvent("Retrying connection...", null));
            reconnectAfter(device, 15000, port);
        }
    };

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
	 * (check value of <code>isConnected</code> boolean), either call
	 * {@link #startForeground(int, Notification)} to put up the
	 * "service is running" notification, or call
	 * {@link #stopForeground(boolean) stopForeground(true)} to remove the
	 * notification (because the service isn't listening).
	 */
	private void showNotificationIfRunning() {
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
		// Make new intent with command CMD_CONNECT and the IP given
		Intent intent = new Intent(context, VehicleInfoService.class);
		// Add command and the ip address
		intent.putExtra(INTENT_CMD, CMD_CONNECT).putExtra(INTENT_IP, ip_addr);
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
	 * @param intent the intent used to call startService
	 * @param command the command that the intent had as an extra
	 */
	private void parseIntent(Intent intent, int command) {
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
				mDevice.disconnectDataStream();
				mDevice = null;
				app.setDevice(null);
			}
			break;
		default:
			Log.i(TAG, "startService - unknown command " + command);
			isConnected = false;
		}

		if (isConnect) {
			String ip = intent.getStringExtra(INTENT_IP);
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
                    mDevice = new Device(connectIp, port);
                    app.setDevice(mDevice);
                }

                mDevice.initVehicleData(new WvaCallback<Set<String>>() {
					@Override
					public void onResponse(Throwable error, Set<String> endpoints) {
                        Log.d(TAG, "initVehicleData onResponse...");
                        if (error != null) {
                            Log.e(TAG, "Got error starting Vehicle", error);
//                            error.printStackTrace();
                            mDevice = null;
                            app.setDevice(null);
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

                        for (String e : endpoints) {
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

                            if (autosub > 0)
                                // (Try to) subscribe to the endpoint
                                app.subscribeToEndpoint(e, autosub,
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
                            else {
                                // Just display the endpoint in the list.
                                app.listNewEndpoint(ep);
                                mDevice.unsubscribe(ep, true);
                            }
                        }
                    }
                });

                if (mDevice == null)
                    return;

                mDevice.connectDataStream(port, connectionListener);

                if (mDevice == null)
                    return;

                // Android Studio warns that getApplicationContext() might
                // return null. So we will check for null in the callbacks.
                final Context toastContext = getApplicationContext();

                mDevice.configurePort(port, new WvaCallback<Integer>() {
                    @Override
                    public void onResponse(Throwable error, Integer response) {
                        if (error == null) {
                            Log.d(TAG, "Successfully configured port.");
                        }
                        else {
                            Log.d(TAG, "Failed to configure port", error);
                            if (toastContext == null)
                                return;
                            Toast.makeText(toastContext,
                                    "Failed to set port to " + port,
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });

                if (mDevice == null)
                    return;

                mDevice.configureBaudRate(250000, new WvaCallback<Integer>() {
                    @Override
                    public void onResponse(Throwable error, Integer response) {
                        if (error == null) {
                            Log.d(TAG, "Failed to configure baud rate.");
                        }
                        else {
                            Log.d(TAG, "Failed to configure baud rate", error);
                            if (toastContext == null)
                                return;
                            Toast.makeText(toastContext,
                                    "Failed to configure baud rate",
                                    Toast.LENGTH_SHORT).show();
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
    public Device getDevice() {
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
//		Log.d("VehicleInfoService", "onDestroy");

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
