/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.device;

import android.util.Log;
import com.digi.wva.async.*;
import com.digi.wva.exc.EndpointUnknownException;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This object allows users to access both raw and derived data about the
 * current operation of a vehicle. These endpoints offer a subscription model
 * for continuous updates, and an alarm model for handling/logging special
 * events.
 *
 */
public class Vehicle {
    private static final String TAG = "com.digi.wva.client.Vehicle";
	private static final String VEHICLE_BASE = "vehicle/data/";
	private static final String SUBSCRIPTION_BASE = "subscriptions/";
	private static final String ALARM_BASE = "alarms/";
	private static final boolean BUFFER_SUBSCRIPTIONS = true;
	private static final boolean BUFFER_ALARMS = true;
    public static final String SUB_SUFFIX = "~sub";
    private final ConcurrentHashMap<String, VehicleResponse> map;
	private Set<String> keyset;
	private final WvaHttpClient httpClient;
	
    private ConcurrentHashMap<String, WvaListener> listenerMap = new ConcurrentHashMap<String, WvaListener>();
	
	public Vehicle(WvaHttpClient client) {
		this.map = new ConcurrentHashMap<String, VehicleResponse>();
		this.httpClient = client;
	}
	
	public void initialize(final WvaCallback<Set<String>> onInitialized) {
		httpClient.get(VEHICLE_BASE, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject obj) {
				if (obj.has("data"))
					try {
						onSuccess(obj.getJSONArray("data"));
					} catch (JSONException e) {
						e.printStackTrace();
						onFailure(e, "");
					}
				else
					onFailure(new Exception("object received from device did not have 'data' field"), "");
			}

			@Override
			public void onSuccess(JSONArray uris) {
				int l  = uris.length();
				String uri;
				
				for (int i = 0; i < l; i++) {
					try {
						
						uri = uris.getString(i);
						String endpoint = uri.substring(uri.lastIndexOf('/') + 1);
						map.putIfAbsent(endpoint, new VehicleResponse());
//						Log.v(TAG, String.format("creating %s endpoint", endpoint));
						
					} catch (JSONException e) {
						Log.w(TAG, "couldn't initialize vehicle data correctly");
					}
				}
				keyset = map.keySet();
				onInitialized.onResponse(null, keyset);
			}
			
			@Override
			public void onFailure(Throwable error, String content) {
				Log.e(TAG, "initialize() failed");
				error.printStackTrace();
				onInitialized.onResponse(error, new HashSet<String>());
			}

            @Override
            public void onFailure(Throwable error, JSONObject response) {
                onFailure(error, "");
            }
		});
	}

    public Set<String> getKeySet() {
        if (this.keyset == null) {
            return new HashSet<String>();
        }
        else {
            return new HashSet<String>(this.keyset);
        }
    }
	
	/**
	 * Returns a set of all queryable endpoints
	 */
    public void validateEndpoint(String endpoint) throws EndpointUnknownException {
		if (!getKeySet().contains(endpoint)) {
			throw(new EndpointUnknownException(
					String.format("endpoint %s is not known.", endpoint)));
		}
	}
	
	public void notifyListeners(Event e) {
		VehicleResponse data = map.get(e.getEndpoint());
		if (data == null) {
			Log.i(TAG, "Endpoint " + e.getEndpoint() + " does not exist.");
			return;
		}

        if (listenerMap.containsKey(e.getShortName())) {
            listenerMap.get(e.getShortName()).onUpdate(e.getEndpoint(), e.getResponse());
        }
        else {
            Log.d(TAG, "Received event that had no listener");
        }
	}
	
	/**
	 * This method is used to update the cached value of an endpoint, which
	 * will also execute all callback objects associated with that endpoint.
	 * It is convenient for testing when no WVA and/or TCP connection is
	 * available.
	 * 
	 * @e an Event object
	 */
    void updateCached(Event e) {
        if (e != null && map.replace(e.getEndpoint(), e.getResponse()) != null) {
			notifyListeners(e);
        }
        else {
            Log.w(TAG, "received null/unknown event");
        }
	}

    public void updateCached(String endpoint, VehicleResponse response) {
        if(map.replace(endpoint, response) != null) {
            notifyListeners(new Event("subscription", endpoint, null, "shortname", response));
        }
    }

	private void addListener(String shortName, WvaListener listener) {
        listenerMap.put(shortName, listener);
	}

    /**
	 * Synchronously returns the last response received by this library for a
	 * given endpoint. No networking is involved in this request. To update this
	 * field to the latest value, use update() or create a subscription.
	 */
	public VehicleResponse getCached(String endpoint) {
		if (!getKeySet().contains(endpoint)) {
			return null;
		}
		return map.get(endpoint);
	}
	
	/**
	 * Asynchronously queries the WVA for the newest data at the given endpoint 
	 * and caches the result.
	 * 
	 * Note that this is a relatively resource-intensive request and intended
	 * to be an ad-hoc operation. Instead of using update() in a loop to query
	 * endpoints, create subscriptions to receive new information as it arrives.
	 * 
	 * @param endpoint 
	 * @param cb the callback to handle the response.
	 * @throws EndpointUnknownException if the endpoint given is not found
     *         in Vehicle.getEndpoints()
	 */
	public void fetchNew(final String endpoint, final WvaCallback<VehicleResponse> cb)
			throws EndpointUnknownException {
		
		validateEndpoint(endpoint);
		
		httpClient.get(VEHICLE_BASE + endpoint, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject jObj) {
				JSONObject valTimeObj;
				try {
					valTimeObj = jObj.getJSONObject(endpoint);
                    map.replace(endpoint, new VehicleResponse(valTimeObj));

					if (cb != null) {
						cb.onResponse(null, new VehicleResponse(valTimeObj));
					}

				} catch (JSONException e) {
					Log.e(TAG, "Data fetched from " + endpoint + " unreadable");
                    cb.onResponse(e, null);
				}
			}

            @Override
            public void onFailure(Throwable error, String response) {
                Log.e(TAG, "Unable to connect to device during fetchNew");
                cb.onResponse(error, null);
            }

            @Override
            public void onFailure(Throwable error, JSONObject response) {
                onFailure(error, "");
            }
		});
	}
	
	public void fetchNew(final String endpoint) throws EndpointUnknownException {
		this.fetchNew(endpoint, null);
	}
	
	/** 
	 * When a subscription is created for an endpoint, that endpoint will
	 * automatically update at regular intervals. This is the preferred method
	 * of receiving vehicle data from the WVA device because it does not have to
	 * create an HTTP connection for every piece of data received. Note that
	 * only one subscription can be created per endpoint.
	 * 
	 * If a callback is given, it will be executed every time the data is
	 * updated.
	 * 
	 * @param endpoint The type of information. Must be in this.endpoints()
	 * @param seconds The interval of time between updates
	 * @param listener A callback to execute every time the endpoint updates
	 * @throws EndpointUnknownException If the endpoint does not exist
	 * @throws JSONException If an error occurs while creating the request
	 */
	public void subscribe(final String endpoint, int seconds, final WvaListener listener, final WvaCallback<Void> cb)
			throws JSONException, EndpointUnknownException {
		
		validateEndpoint(endpoint);
		
		JSONObject parameters = new JSONObject();
		JSONObject subscription = new JSONObject();
		parameters.put("interval", seconds);
		parameters.put("uri", VEHICLE_BASE + endpoint);
		parameters.put("buffer",  BUFFER_SUBSCRIPTIONS ? "queue" : "discard");
		subscription.put("subscription",  parameters);

        // The url at which the subscription will be available
        final String shortName = endpoint + SUB_SUFFIX;

		httpClient.put(SUBSCRIPTION_BASE + shortName, subscription, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String responseString) {
				if (listener != null) {
					addListener(shortName, listener);
				}
                if (cb != null) {
                    cb.onResponse(null, null);
                }
            }

            @Override
            public void onFailure(Throwable error, String response) {
                if (cb != null) {
                    cb.onResponse(error, null);
                }
            }
		});
	}
	
	/**
	 * Ends the constant updating of the given endpoint. if deleteCallbacks is
	 * true, all subscription listeners for that endpoint will be disassociated
     * as well.
	 * @param endpoint
	 * @param deleteCallbacks
	 */
	public void unsubscribe(final String endpoint, final boolean deleteCallbacks, final WvaCallback<Void> cb) {

        final String shortName = endpoint + SUB_SUFFIX;

		httpClient.delete(SUBSCRIPTION_BASE + shortName,  new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response) {
				if (deleteCallbacks) {
                    listenerMap.remove(endpoint + SUB_SUFFIX);
				}
                if (cb != null) {
                    cb.onResponse(null, null);
                }
			}

			@Override
			public void onFailure(Throwable error, String response) {
				Log.e(TAG, "unable to unsubscribe from " + endpoint, error);
                if (cb != null) {
                    cb.onResponse(error, null);
                }
			}
		});
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
	 * @param endpoint
	 * @param type the type of alarm to create. One endpoint can't have two
	 * alarms of the same type
	 * @param seconds the minimum number of seconds before two alarms of the same
	 * type will be generated (for instance, only send an alarm for speeding
	 * once in a five-minute period)
	 * @param threshold changes meaning depending on AlarmType
	 * @param listener The method to handle the alarm event
     * @param cb Executed when the HTTP response is received with the
     *           short_name of the alarm
	 * @throws EndpointUnknownException if the alarm data given is not valid (such as
     * supplying an invalid endpoint)
	 */
	public void createAlarm(final String endpoint, AlarmType type, int seconds, 
			double threshold, final WvaListener listener, final WvaCallback<Void> cb)
            throws JSONException, EndpointUnknownException {

		validateEndpoint(endpoint);
		
		JSONObject parameters = new JSONObject();
		JSONObject alarm = new JSONObject();
		parameters.put("interval", seconds);
		parameters.put("uri", VEHICLE_BASE + endpoint);
		parameters.put("type", AlarmType.makeString(type));
		parameters.put("threshold", threshold);
		parameters.put("buffer", BUFFER_ALARMS ? "queue" : "discard");
		alarm.put("alarm",  parameters);

        // The resource at which the alarm will be available
        final String shortname = endpoint + "~" + AlarmType.makeString(type);

		httpClient.put(ALARM_BASE + shortname, alarm,
                new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                if (listener != null) {
                    addListener(shortname, listener);
                }
                if (cb != null) {
                    cb.onResponse(null, null);
                }
            }

            @Override
            public void onFailure(Throwable error, String response) {
                Log.e(TAG, "Unable to add alarm to " + endpoint);
                if (cb != null) {
                    cb.onResponse(error, null);
                }
            }
        });
    }

	/**
	 * Removes the alarm attached to the given endpoint. If deleteCallbacks
	 * is true, the alarm listeners attached to that endpoint will also be
     * removed.
	 * @param endpoint
	 * @param type
	 * @param deleteCallbacks
	 */
	public void deleteAlarm(final String endpoint, final AlarmType type,
                            final boolean deleteCallbacks, final WvaCallback<Void> cb) {

        final String shortname = endpoint + "~" + AlarmType.makeString(type);

		httpClient.delete(ALARM_BASE + shortname,
				new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response) {
				if (deleteCallbacks) {
                    listenerMap.remove(shortname);
				}
                if (cb != null) {
                    cb.onResponse(null, null);
                }
			}
			
			@Override
			public void onFailure(Throwable error, String body) {
                Log.e(TAG, "Unable to remove alarm from " + endpoint);
                if (cb != null) {
                    cb.onResponse(error, null);
                }
			}
		});
	}
	
	/**
	 * Removes all listeners attached to all endpoints. This does not remove the
     * subscriptions or alarms at the device level.
	 */
	public void removeAllListeners() {
        listenerMap.clear();
	}
}
