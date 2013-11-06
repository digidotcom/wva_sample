/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.device;

import android.util.Log;
import com.digi.wva.async.AlarmType;
import com.digi.wva.async.VehicleResponse;
import com.digi.wva.async.WvaCallback;
import com.digi.wva.async.WvaListener;
import com.digi.wva.exc.EndpointUnknownException;
import com.loopj.android.http.AsyncHttpResponseHandler;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Device {
	private static final String TAG = "com.digi.wva.device.Device";
	private String hostname;
	private int port;

	private Vehicle vehicle;
	private Hardware hardware;
	private Ecu ecu;

	private WvaHttpClient httpClient;
	private TCPReceiver receiver;
	private MessageHandler msgHandler;

    protected Device() {

    }

    /**
     * Device Constructor
     */
	public Device(String hostname, int subscriptionPort) {
		this.hostname = hostname;
		this.port = subscriptionPort;
        this.httpClient = new WvaHttpClient(hostname);
		this.vehicle = new Vehicle(httpClient);
		this.ecu = new Ecu(httpClient);
		this.hardware = new Hardware(httpClient);
	}

    /**
     * Device Factory
     *
     * @return Device
     */
    public static Device getDevice(String hostname, int subscriptionPort,
            WvaHttpClient client, Vehicle vehicle, Ecu ecu, Hardware hw) {
        Device dev = new Device();
        dev.hostname = hostname;
        dev.port = subscriptionPort;

        dev.httpClient = ((client != null)  ? client  : new WvaHttpClient(hostname));
        dev.vehicle    = ((vehicle != null) ? vehicle : new Vehicle(client));
        dev.ecu        = ((ecu != null)     ? ecu     : new Ecu(client));
        dev.hardware   = ((hw != null)      ? hw      : new Hardware(client));

        return dev;
    }

    /**
     * Returns true if the Device's TCPReceiver is currently connected and
     * attempting to receive data.
     */

	public boolean isDataStreamDisconnected() {
		return ((this.receiver == null || this.receiver.isStopped()));
	}


	/**
	 * Turns on the TCP stream which conveys subscription and alarm data.
	 * Subscriptions and alarms can be created without the stream, but no
	 * data will be sent.
	 */
	public void connectDataStream(final int port, final DeviceConnectionListener listener) {
        receiver = new TCPReceiver(this, hostname, port, listener);
        msgHandler = new MessageHandler(receiver, vehicle);
        receiver.start();
        msgHandler.start();
	}

    /**
     * Sets the port on the WVA device which should be used to convey the data
     * stream. This call may make the data stream unavailable for multiple
     * seconds as the internal state of the device is reset.
     * @param port The port on the device which should be used.
     * @param callback Executed when the device responds
     */
    public void configurePort(final int port, final WvaCallback<Integer> callback) {
        JSONObject confObj = new JSONObject();
        JSONObject wsObj = new JSONObject();
        try {
            confObj.put("enable", "on");
            confObj.put("port", port);
            wsObj.put("ws_events", confObj);
        } catch (JSONException e) {
            Log.wtf(TAG, "unable to create JSONObject in configurePort");
        }

        httpClient.put("config/ws_events/", wsObj, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String s) {
                callback.onResponse(null, port);
            }

            @Override
            public void onFailure(Throwable throwable, String s) {
                throwable.printStackTrace();
                callback.onResponse(throwable, null);
            }
        });

    }

    /**
     * Sets the baud rate for the connection between the WVA device and the CAN
     * bus interface. This call may make the data stream unavailable for
     * multiple seconds as the internal state of the device is reset.
     *
     * @param baudRate The intended baud rate for the canbus connection
     * @param callback Executed when the device responds
     */
    public void configureBaudRate(final int baudRate, final WvaCallback<Integer> callback) {
        JSONObject confObj = new JSONObject();
        JSONObject canbusObj = new JSONObject();
        try {
            confObj.put("enable", "on");
            confObj.put("rate", 250000);
            canbusObj.put("canbus", confObj);
        } catch (JSONException e) {
            Log.wtf(TAG, "unable to create JSONObject in setBaudRate");
            return;
        }

        httpClient.put("config/canbus/1", canbusObj, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                callback.onResponse(null, baudRate);
            }

            @Override
            public void onFailure(Throwable error, String response) {
                Log.e(TAG, "Unable to set Baud Rate for WVA device");
                callback.onResponse(error, null);
            }

        });
    }

	/**
	 * Disconnects the TCPReceiver conveying subscription and alarm data. This
	 * does not remove the subscriptions or alarms at the device level.
     * Reconnecting will resume the previous subscriptions and alarms.
     *
	 */
	public void disconnectDataStream() {
		if (this.receiver != null)
			this.receiver.stopThread();
		if (this.msgHandler != null)
			this.msgHandler.stopThread();

		this.receiver = null;
		this.msgHandler = null;
	}

	/**
	 * Initializes the cache for real-time readings from the vehicle. This
     * method returns a list of endpoints available for subscriptions and
     * alarms, and this method must be called before making them.
	 * Initializes the cache for subscriptions and alarms. These are
	 * real-time readings from the vehicle.
	 */
	public void initVehicleData(final WvaCallback<Set<String>> onInitialized) {
        vehicle.initialize(onInitialized);
	}

	/**
	 * Initializes the LED information cache to allow interaction with
     * the LED lights on the device.
	 */
	public void initLeds(final WvaCallback<Set<String>> onInitialized) {
		hardware.initializeLeds(onInitialized);
	}

    /**
     * Initializes the button information cache to allow interaction with
     * buttons on the device.
     */
	public void initButtons(final WvaCallback<Set<String>> onInitialized) {
		hardware.initializeButtons(onInitialized);
	}

	/**
	 * Initializes the Engine Control Unit data cache, which contains
	 * information about the hardware controlling the engine.
	 */
	public void initECUs(final WvaCallback<Set<String>> onInitialized) {
		ecu.initialize(onInitialized);
	}

    /**
     * Asynchronously queries the WVA for the newest data at the given endpoint
     * and caches the result.
     *
     * Note that this is a relatively resource-intensive request and intended
     * to be an ad-hoc operation. Instead of using this method in a loop to
     * query endpoints, create subscriptions to receive new information as it
     * arrives.
     *
     * @param endpoint The data endpoint to query
     * @param callback The callback to handle the response.
     */
    public void fetchSubscribable(final String endpoint, final WvaCallback<VehicleResponse> callback) {
        try {
            vehicle.fetchNew(endpoint, callback);
        } catch (EndpointUnknownException e) {
            callback.onResponse(e, null);
        }
    }

    /**
     * When a subscription is created for an endpoint, that endpoint will
     * automatically update at regular intervals. This is the preferred method
     * of receiving vehicle data from the WVA device because it does not have to
     * create an HTTP connection for every piece of data received. Note that
     * only one subscription can be created per endpoint.
     *
     * If a listener is given, it will be executed every time the data is
     * updated. If no listener is desired, use null.
     *
     * @param endpoint The type of information. Must be in the set of endpoints
     *                 returned from initVehicleData
     * @param interval The interval of time between updates
     * @param listener The listener's onUpdate method will be called every time
     *                 new information is received from the device
     * @throws EndpointUnknownException If the endpoint does not exist or has not
     *                 yet been initialized
     */
	public void subscribe(final String endpoint, final int interval, final WvaListener listener)
        throws EndpointUnknownException {
        subscribe(endpoint, interval, listener, null);
	}

    /**
     * When a subscription is created for an endpoint, that endpoint will
     * automatically update at regular intervals. This is the preferred method
     * of receiving vehicle data from the WVA device because it does not have to
     * create an HTTP connection for every piece of data received. Note that
     * only one subscription can be created per endpoint.
     *
     * If a listener is given, it will be executed every time the data is
     * updated. If no listener is desired, use null.
     *
     * @param endpoint The type of information. Must be in the set of endpoints
     *                 returned from initVehicleData
     * @param interval The interval of time between updates
     * @param listener The listener's onUpdate method will be called every time
     *                 new information is received from the device
     * @param callback Executed when the HTTP response has been received
     * @throws EndpointUnknownException If the endpoint does not exist or has not
     *                 yet been initialized
     */
    public void subscribe(final String endpoint, final int interval,
                          final WvaListener listener, final WvaCallback<Void> callback)
        throws EndpointUnknownException {
            try {
                vehicle.subscribe(endpoint, interval, listener, callback);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

    /**
     * Ends the constant updating of the given endpoint. if deleteCallbacks is
     * true, *all* listeners for that endpoint will be disassociated as well.
     * @param endpoint The name of the data endpoint for which to unsubscribe
     * @param removeListeners If true, unregister all listeners at the endpoint
     * @param callback Executed when the HTTP reponse is received
     */
    public void unsubscribe(final String endpoint, final boolean removeListeners,
                            final WvaCallback<Void> callback) {
        vehicle.unsubscribe(endpoint, removeListeners, callback);
    }

    /**
     * Ends the constant updating of the given endpoint. if deleteCallbacks is
     * true, *all* listeners for that endpoint will be disassociated as well.
     * @param endpoint The name of the data endpoint for which to unsubscribe
     * @param removeListeners If true, unregister all listeners at the endpoint
     */
    public void unsubscribe(final String endpoint, final boolean removeListeners) {
        vehicle.unsubscribe(endpoint, removeListeners, null);
    }

    /**
     * Alarm are similar to subscriptions, but they do not occur at regular
     * intervals. Instead, alarms produce data when special conditions occur;
     * see the JavaDoc for AlarmType for more information about the capabilities
     * of alarms. Because alarms can be associated with near-instantaneous events,
     * it is best-practice to always associate a callback with an alarm to make
     * sure it is handled. If ALARM_UPDATE is set to false in this file, alarms
     * will not update the cached endpoints.
     *
     * There can only be one alarm of each type per endpoint (i.e. 4 alarms per
     * endpoint)
     *
     * @param endpoint The name of the data endpoint to add an alarm to
     * @param type The type of alarm to create. One endpoint can't have two
     *             alarms of the same type
     * @param seconds The minimum number of seconds before two alarms of the same
     *             type will be generated (for instance, only send an alarm for
     *             speeding once in a five-minute period)
     * @param threshold Changes meaning depending on AlarmType
     * @param listener The method to handle the alarm event
     * @param callback Executed when the HTTP response is received
     * @throws EndpointUnknownException if the alarm data given is not valid (such as
     * supplying an invalid endpoint)
     */
	public void addAlarm(final String endpoint, final AlarmType type, final int seconds,
			final float threshold, final WvaListener listener, final WvaCallback<Void> callback)
            throws EndpointUnknownException {
		try {
			vehicle.createAlarm(endpoint, type, seconds, threshold, listener, callback);
		}catch(JSONException e) {
			Log.e(TAG, "Incorrect formatting in Device.createAlarm", e);
		}
	}

    /**
     * Alarm are similar to subscriptions, but they do not occur at regular
     * intervals. Instead, alarms produce data when special conditions occur;
     * see the JavaDoc for AlarmType for more information about the capabilities
     * of alarms. Because alarms can be associated with near-instantaneous events,
     * it is best-practice to always associate a callback with an alarm to make
     * sure it is handled. If ALARM_UPDATE is set to false in this file, alarms
     * will not update the cached endpoints.
     *
     * There can only be one alarm of each type per endpoint (i.e. 4 alarms per
     * endpoint)
     *
     * @param endpoint The name of the data endpoint to add an alarm to
     * @param type The type of alarm to create. One endpoint can't have two
     *             alarms of the same type
     * @param seconds The minimum number of seconds before two alarms of the same
     *             type will be generated (for instance, only send an alarm for
     *             speeding once in a five-minute period)
     * @param threshold Changes meaning depending on AlarmType
     * @param listener The method to handle the alarm event
     * @throws EndpointUnknownException if the alarm data given is not valid (such as
     * supplying an invalid endpoint)
     */
    public void addAlarm(final String endpoint, final AlarmType type, final int seconds,
                         final float threshold, final WvaListener listener)
            throws EndpointUnknownException {
        addAlarm(endpoint, type, seconds, threshold, listener, null);
    }

    /**
     * Removes the alarm attached to the given endpoint. If removeListeners
     * is true, the callbacks attached to that endpoint will also be removed.
     * @param endpoint The name of the data endpoint to remove an alarm from
     * @param type The type of alarm which should be removed
     * @param removeListeners If true, unregister all listeners at the endpoint
     * @param callback Executed when the HTTP response is received
     */
	public void removeAlarm(final String endpoint, final AlarmType type,
                            final boolean removeListeners, WvaCallback<Void> callback) {
		vehicle.deleteAlarm(endpoint, type, removeListeners, callback);
	}

    /**
     * Removes the alarm attached to the given endpoint. If removeListeners
     * is true, the callbacks attached to that endpoint will also be removed.
     * @param endpoint The name of the data endpoint to remove an alarm from
     * @param type The type of alarm which should be removed
     * @param removeListeners If true, unregister all listeners at the endpoint
     */
    public void removeAlarm(final String endpoint, final AlarmType type,
                            final boolean removeListeners) {
        removeAlarm(endpoint, type, removeListeners, null);
    }

	public VehicleResponse lastReceived(final String endpoint) {
		return vehicle.getCached(endpoint);
	}

    /**
     * Set the new time on the device. Note that this call does not complete
     * instantaneously, so it isn't likely that the WVA device will ever have
     * the exact same time as the device running this library.
     *
     * @param time The time to send to the WVA device
     * @param callback Executed once the time has been successfully sent to the
     *                 device
     */
	public void setTime(final DateTime time, final WvaCallback<DateTime> callback) {
		try {
			hardware.setTime(time, callback);
		} catch (JSONException e) {
			callback.onResponse(e, null);
		}
	}

    /**
     * Receive the current time from the device. This is a network call, so
     * the time received by the callback could be stale by a few seconds.
     *
     * @param callback Called once the time has been successfully received
     */
	public void getTime(final WvaCallback<DateTime> callback) {
		hardware.fetchTime(callback);
	}

    /**
     * Tells the WVA web service to turn on/off the given LED, depending on
     * the value of the state parameter.
     * @param ledName The name of the LED to modify
     * @param state Whether or not the LED should be turned on (true == 'on')
     * @param callback Executed once the LED has been shut down
     */
	public void setLed(final String ledName, final boolean state, final WvaCallback<Boolean> callback) {
		try {
			hardware.setLed(ledName, state, callback);
		} catch (JSONException e) {
			callback.onResponse(e, null);
		} catch (EndpointUnknownException e) {
			e.printStackTrace();
		}
	}

    /**
     * Retrieves the most recent state of the LED in question. This call has
     * the same issues with polling as getButton.
     *
     * @param ledName The name of the LED to query
     * @param callback Executed once the LED has been queried
     */
	public void getLed(final String ledName, final WvaCallback<Boolean> callback) {
		try {
			hardware.fetchLedState(ledName, callback);
		} catch (EndpointUnknownException e) {
			callback.onResponse(e, null);
		}
	}

    /**
     * Retrieve the current state of the button on the WVA device. 'true'
     * represents that the button is currently being depressed. The sensor on
     * the device has a margin of error, and this method is intended to be used
     * in long-press situations such as "Hold the ___ button for five seconds".
     * Furthermore, the hardware does not currently support subscriptions, so
     * the button must be polled; it is not recommended to use this method in a
     * fast loop except when the button state is actually needed.
     * @param buttonName The name of the Button to query
     * @param callback Executed once the button has been queried
     */
	public void getButton(final String buttonName, final WvaCallback<Boolean> callback) {
		try {
			hardware.fetchButtonState(buttonName, callback);
		} catch (EndpointUnknownException e) {
			callback.onResponse(e, null);
		}
	}

    /**
     * Allows a given Engine Control Unit's endpoints to be queried. Calling
     * getAllEcuData before calling this function is valid, but no endpoints
     * will be defined and getAllEcuData will have no effect.
     *
     * @param ecuName Name of the ECU on which to query endpoints
     */
	public void getEcuEndpoints(final String ecuName, final WvaCallback<Set<String>> callback) {
		try {
			ecu.defineEcuEndpoints(ecuName, callback);
		} catch (EndpointUnknownException e) {
			callback.onResponse(e, null);
		}
	}

	/**
	 * Fetches all endpoints from a single ECU, calling the provided callback's
	 * onResponse method with the data from every endpoint.
	 *
	 * This is a relatively resource-intensive call, but it should only have to
	 * be performed once per ECU; the data is completely static and can just be
	 * accessed from the local cache.
	 */
	public void getAllEcuData(String ecuName, WvaCallback<Map<String, String>> callback) {
		try {
			ecu.fetchAllEndpoints(ecuName, callback);
		} catch (EndpointUnknownException e) {
			callback.onResponse(e, null);
		}
	}

    /**
     *
     * @param ecuName The name of the ECU
     * @param endpoint The endpoint on that ECU to query
     * @return The string representation of the data for the given ECU and
     *         endpoint. Depending on the endpoint, the actual data is
     *         semantically decimal, or hexadecimal, but it is received as a
     *         String for type simplicity.
     */
    public String cachedEcuData(String ecuName, String endpoint) {
        return ecu.getCachedData(ecuName, endpoint);
    }
}

