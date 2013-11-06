/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.device;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.digi.wva.async.WvaCallback;
import com.digi.wva.exc.EndpointUnknownException;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

public class Hardware {
	private static final DateTimeFormatter format = ISODateTimeFormat.dateTimeNoMillis();
	private static final String TAG = "com.digi.wva.client.Hardware";
	private static final String LED_BASE = "hw/leds/";
	private static final String BUTTON_BASE = "hw/buttons/";
	private static final String TIME_BASE = "hw/time/";
    private static final String LED_KEY = "leds";
    private static final String BUTTON_KEY = "buttons";
	@SuppressWarnings("UnusedDeclaration")
    private int initializeCount;
	
	private final WvaHttpClient httpClient;
	private final ConcurrentMap<String, Boolean> leds;
	private final ConcurrentMap<String, Boolean> buttons;
	@SuppressWarnings("UnusedDeclaration")
    private static final int MAPCOUNT = 2;
	
	public Hardware(WvaHttpClient httpClient) {
		this.httpClient = httpClient;
		this.leds = new ConcurrentHashMap<String, Boolean>();
		this.buttons = new ConcurrentHashMap<String, Boolean>();
	}
	
	private void validateName(String name, Map<String, Boolean> map) throws EndpointUnknownException {
		if (!map.keySet().contains(name)) {
			throw( new EndpointUnknownException("% is not a valid hardware identifier."));
		}
	}
	
	private class InitHandler extends JsonHttpResponseHandler {
		private final ConcurrentMap<String, Boolean> map;
		private final WvaCallback<Set<String>> cb;
        private final String key;
		
		public InitHandler(String key, ConcurrentMap<String, Boolean> setToAdd, WvaCallback<Set<String>> cb) {
            this.key = key;
			this.map = setToAdd;
			this.cb = cb;
		}
		
		@Override
		public void onSuccess(JSONObject retObj) {
			JSONArray uris;
			try {
                uris = retObj.getJSONArray(key);
                int l  = uris.length();
                String uri;
                for (int i = 0; i < l; i++) {

                    uri = uris.getString(i);
                    String endpoint = uri.substring(uri.lastIndexOf('/') + 1);
                    map.put(endpoint, false);
                    Log.v(TAG, String.format("adding hardware %s", endpoint));

                }

                cb.onResponse(null, map.keySet());
			} catch (JSONException e) {
				Log.w(TAG, "couldn't initialize hardware data correctly", e);
                cb.onResponse(e, null);
                return;
			}
		}
		
		@Override
		public void onFailure(Throwable error, String response) {
			Log.w(TAG, "Received error while initializing hardware", error);
            cb.onResponse(error, new HashSet<String>());
		}

        @Override
        public void onFailure(Throwable error, JSONObject response) {
            onFailure(error, "");
        }
	}

	public void initializeLeds(WvaCallback<Set<String>> onInitialized) {
		httpClient.get(LED_BASE, new InitHandler(LED_KEY, leds, onInitialized));
	}
	
	public void initializeButtons(WvaCallback<Set<String>> onInitialized) {
		httpClient.get(BUTTON_BASE, new InitHandler(BUTTON_KEY, buttons, onInitialized));
	}
	
	/**
	 * Returns the set of all known button names. Modifyng the returned set
	 * is safe, but useless.
	 */
	@SuppressWarnings("UnusedDeclaration")
    public Set<String> getButtonSet() {
		return buttons.keySet();
	}

	/**
	 * Returns the set of all known LED names. Modifying the returned set is
	 * safe, but useless.
	 */
	@SuppressWarnings("UnusedDeclaration")
    public Set<String> getLedSet() {
		return leds.keySet();
	}

	/**
	 * Retrieve the current state of the button on the WVA device. 'true'
	 * represents that the button is currently being depressed. The sensor on
	 * the device has a margin of error, and this method is intended to be used
	 * in long-press situations such as "Hold the ___ button for five seconds".
	 * Furthermore, the hardware does not currently support subscriptions, so
	 * the button must be polled; it is not recommended to use this method in a
	 * fast loop except when the button state is actually needed.
	 * @param buttonName
	 * @param cb
	 * @throws EndpointUnknownException 
	 */
	public void fetchButtonState(final String buttonName, final WvaCallback<Boolean> cb)
			throws EndpointUnknownException {
		validateName(buttonName, buttons);
		httpClient.get(BUTTON_BASE + buttonName, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject btn) {
				String pressed;
				try {
					pressed = btn.getString("button");
					boolean upDown = pressed.equals("up");
					cb.onResponse(null, upDown);
					
				} catch (JSONException e) {
					Log.w(TAG, "unable to fetch " + buttonName);
                    cb.onResponse(e, null);
				}
			}

            @Override
            public void onFailure(Throwable error, String response) {
                cb.onResponse(error, null);
            }
		});
	}
	
	/**
	 * Retrieves the most recent state of the LED in question. This call has
	 * the same issues with polling as fetchButtonState.
	 * 
	 * @param ledName
	 * @param cb
	 * @throws EndpointUnknownException 
	 */
	public void fetchLedState(final String ledName, final WvaCallback<Boolean> cb)
			throws EndpointUnknownException {
		validateName(ledName, leds);
		httpClient.get(LED_BASE + ledName, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject led) {
				String state;
				try {
					state = led.getString("led");
					boolean onOff = state.equals("on");
					cb.onResponse(null, onOff);
				} catch (JSONException e) {
					Log.w(TAG, "unable to fetch " + ledName);
                    cb.onResponse(e, null);
				}

			}

            @Override
            public void onFailure(Throwable error, String response) {
                cb.onResponse(error, null);
            }
		});
	}
	
	/**
	 * Tells the WVA web service to turn on/off the given LED, depending on
	 * the value of the state parameter.
	 * @param ledName
	 * @param state
	 * @param cb
	 * @throws JSONException 
	 * @throws EndpointUnknownException 
	 */
	public void setLed(final String ledName, final boolean state, final WvaCallback<Boolean> cb)
			throws JSONException, EndpointUnknownException {
		
		validateName(ledName, leds);
		
		JSONObject led = new JSONObject();
		led.put("led", state ? "on" : "off");
		
		httpClient.put(LED_BASE + ledName, led, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response) {
				cb.onResponse(null, state);
			}

            @Override
            public void onFailure(Throwable error, String response) {
                cb.onResponse(error, null);
            }

		});
		
	}
	
	/**
	 * Receive the current time from the device. This is a network call, so
	 * the time received by the callback could be stale by a few seconds.
	 * 
	 * @param cb Called once the time has been successfully received
	 */
	public void fetchTime(final WvaCallback<DateTime> cb) {
		httpClient.get(TIME_BASE, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject timeObj) {
				String timeStr;
				try {
					timeStr = timeObj.getString("time");
					cb.onResponse(null, format.parseDateTime(timeStr));
				} catch (JSONException e) {
					Log.w(TAG, "unable to get time.");
					cb.onResponse(e, null);
				}
			}

            @Override
            public void onFailure(Throwable error, String response) {
                cb.onResponse(error, null);
            }

            @Override
            public void onFailure(Throwable error, JSONObject response) {
                onFailure(error, "");
            }
		});
	}
	
	/**
	 * Set the new time on the device. Note that this call does not complete
	 * instantaneously, so it isn't likely that the WVA device will ever have
	 * the exact same time as the device running this library.
	 * 
	 * @param newTime The time to send to the WVA device
	 * @param cb Executed once the time has been successfully sent to the device
	 * @throws JSONException 
	 */
	public void setTime(final DateTime newTime, final WvaCallback<DateTime> cb) throws JSONException {
		JSONObject time = new JSONObject();
		String timestamp = format.print(newTime);
		time.put("time", timestamp);
		
		httpClient.put(TIME_BASE, time, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response) {
				cb.onResponse(null, newTime);
			}

            @Override
            public void onFailure(Throwable error, String response) {
                cb.onResponse(error, null);
            }

		});
	}
}