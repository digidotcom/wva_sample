/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.device;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.digi.wva.async.WvaCallback;
import com.digi.wva.exc.EndpointUnknownException;
import com.loopj.android.http.JsonHttpResponseHandler;

/**
 * This object defines the interactions with the vehicle's Engine Control Units
 * (ECUs). These units provide useful information about a vehicle's physical
 * parts and attributes, such as VINs, serial numbers, make, and model. These
 * pieces of information are expected to be consistent throughout an entire
 * trip, if not for the entire lifetime of the vehicle. Therefore, polling
 * any of the information available through this object should not be necessary.
 * 
 * 
 * @author awickert
 *
 */
public class Ecu {
	private static final String TAG = "com.digi.wva.client.Ecu";
	private static final String ECU_BASE = "vehicle/ecus/";

    private final WvaHttpClient httpClient;
	private final ConcurrentMap<String, ConcurrentMap<String, String>> ecuMap;

    public Ecu(WvaHttpClient httpClient) {
		this.httpClient = httpClient;
		this.ecuMap = new ConcurrentHashMap<String, ConcurrentMap<String, String>>();
	}
	
	private class InitHandler extends JsonHttpResponseHandler {
		private final WvaCallback<Set<String>> cb;
		
		public InitHandler(WvaCallback<Set<String>> cb) {
			this.cb = cb;
		}
		@Override
		public void onSuccess(JSONObject ecus) {
			try {

				JSONArray uris;
				uris = (JSONArray) ecus.get("ecus");
                int ecuCount = uris.length();
				String uri;

				for (int i = 0; i < ecuCount; i++) {
					uri = uris.getString(i);
					String endpoint = uri.substring(uri.lastIndexOf('/') + 1);
					ecuMap.putIfAbsent(endpoint, new ConcurrentHashMap<String, String>());
					Log.v(TAG, "initializing " + endpoint);
				}

                if (cb != null) {
                    cb.onResponse(null, ecuMap.keySet());
                }

			} catch (JSONException e) {
				Log.w(TAG, "couldn't initialize ecus properly");
                if (cb != null) {
                    cb.onResponse(e, null);
                }
			}
		}

		@Override
		public void onFailure(Throwable error, String response) {
            if (cb != null) {
                cb.onResponse(error, null);
            }
		}

        @Override
        public void onFailure(Throwable error, JSONObject response) {
            onFailure(error, "");
        }
	}
	
	/**
	 * Caches all ECU names available. 
	 */
	public void initialize(final WvaCallback<Set<String>> onInitialized) {
		httpClient.get(ECU_BASE, new InitHandler(onInitialized));
	}
	
	/**
	 * Allows a given Engine Control Unit's endpoints to be queried. Calling
	 * fetchAllEndpoints before calling this function is valid, but no endpoints
	 * will be defined and fetchEcuEndpoints will have no effect.
	 * 
	 * @param ecuName Name of the ECU on which to query endpoints
	 * @throws EndpointUnknownException 
	 */
	public void defineEcuEndpoints(final String ecuName, final WvaCallback<Set<String>> cb) throws EndpointUnknownException {
		if (!ecuMap.keySet().contains(ecuName)) {
			throw(new EndpointUnknownException(
					String.format("ECU Name %s doesn't exist in cache. Try running defineEcus()", ecuName)));
		}
		httpClient.get(ECU_BASE + ecuName, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject ecuResp) {
				JSONArray uris;
				try {
					uris = (JSONArray) ecuResp.get(ecuName);
					int len  = uris.length();
					String uri;
					
					for (int i = 0; i < len; i++) {
						uri = uris.getString(i);
						String endpoint = uri.substring(uri.lastIndexOf('/') + 1);
						ecuMap.get(ecuName).put(endpoint, "");
						Log.v(TAG, String.format("creating %s endpoint", endpoint));
					}

                    if (cb != null) {
                        cb.onResponse(null, ecuMap.get(ecuName).keySet());
                    }
				} catch (JSONException e) {
					Log.w(TAG, "couldn't initialize ecu " + ecuName);
                    if (cb != null) {
                        cb.onResponse(e, null);
                    }
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
	 * Fetches the data for all available endpoints for a given ECU. Since ECU
	 * data should be relatively static for the life of the vehicle, this call
	 * should only need to be performed once. After that it is likely fine to
	 * query the cached data using getCachedData() and providing the appropriate
	 * ECU. If defineEcus() has not been called, WvaException will be thrown.
	 * If defineEcuEndpoints() has not yet been called, the ECU will have no 
	 * available endpoints and nothing will happen, but no exception will be 
	 * thrown because it isn't destructive behavior.
	 * 
	 * @param ecuName the name of the Engine Control Unit to update
	 * @param cb an action to be performed after fetching the data is complete
	 * @throws EndpointUnknownException on an invalid ECU name (including if none have been
         *         defined)
	 */
	public void fetchAllEndpoints(final String ecuName, final WvaCallback<Map<String, String>> cb)
			throws EndpointUnknownException {
		if (!ecuMap.keySet().contains(ecuName)) {
			throw(new EndpointUnknownException(
					String.format("ECU Name %s doesn't exist in cache. Try running initialize()", ecuName)));
		}
		final ConcurrentMap<String, String> endpointMap = ecuMap.get(ecuName);
		for (final String endpoint : endpointMap.keySet()) {
			httpClient.get(ECU_BASE + ecuName + "/" + endpoint, new JsonHttpResponseHandler() {
				@Override
				public void onSuccess(JSONObject keyVal) {
					String value;
					try {
						value = keyVal.get(endpoint).toString();
						endpointMap.put(endpoint, value);
					} catch (JSONException e) {
						Log.w(TAG, String.format("Unable to fetch %s for ECU %s.", endpoint, ecuName));
                        if (cb != null) {
                            cb.onResponse(e, null);
                        }
                        return;
					}
                    if (cb != null) {
                        Map<String,String> m = new HashMap<String,String>();
                        m.put(endpoint, value);
                        cb.onResponse(null, m);
                    }
				}

                @Override
                public void onFailure(Throwable error, String response) {
                    if (cb != null) {
                        cb.onResponse(error, null);
                    }
                }

                @Override
                public void onFailure(Throwable error, JSONObject response) {
                    onFailure(error, "");
                }
			});
		}
	}
	
    public Set<String> getCachedEcuNames() {
		return ecuMap.keySet();
	}
	
    public Set<String> getCachedEndpoints(String ecuName) {
		return ecuMap.get(ecuName).keySet();
		
	}
	
    public String getCachedData(String ecuName, String endpoint) {
		return ecuMap.get(ecuName).get(endpoint);
	}
}
