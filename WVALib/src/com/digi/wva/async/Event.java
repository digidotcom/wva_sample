/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.async;

import android.util.Log;
import com.digi.wva.util.WvaUtil;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;


public class Event {
    public static final String TAG = "com.digi.wva.Event";
    private String type;
	private String endpoint;
    private DateTime sent;
    private String shortName;
	private VehicleResponse resp;

    public Event(String et, String endpoint, DateTime sent, String shortName,
                 VehicleResponse resp) {
        this.type = et;
        this.endpoint = endpoint;
        this.resp = resp;
        this.shortName = shortName;
        this.sent = sent;
    }

    protected Event() {
        type = null;
        endpoint = null;
        sent = null;
        this.shortName = null;
        resp = null;
    }

	/**
	 * This method is the preferred way of creating an Event. It takes
	 * a JSON object from the TCPReceiver's object queue and transforms it
	 * into an event consumable by anything registered to the client.
	 * 
	 * Any JSONObject can be sent through this method, but only objects
	 * of the correct format will produce a non-null object
	 * @param obj JSONObject to be used in constructing a new Event object
	 * @return a new Event object constructed from 'obj'
	 */
	public static Event fromTCP(JSONObject obj) {
        Event e = new Event();
		JSONObject innerObj = null;
		try {
			innerObj = obj.getJSONObject("alarm");
			e.type = "alarm";
		} catch (JSONException ignored) { }
		
		try {
			innerObj = obj.getJSONObject("data");
			e.type = "subscription";
		} catch (JSONException ignored) { }
		
		if (innerObj == null) {
			Log.i("Event", "Inner object is null...");
			return null;
		}

        if (e.type.equals("subscription")) try {

            String uri = innerObj.getString("uri");
            int endpointStart = uri.lastIndexOf('/');
            e.endpoint = uri.substring(endpointStart + 1, uri.length());
            e.sent = WvaUtil.dateTimeFromString(innerObj.getString("timestamp"));
            e.shortName = innerObj.getString("short_name");

        } catch (JSONException exc) {
            Log.i("WVALib/event", "Unable to create subscription event: malformed object");
            return null;
        }

        if (e.type.equals("alarm")) try {

            String uri = innerObj.getString("uri");
            int endpointStart = uri.lastIndexOf('/');
            e.endpoint = uri.substring(endpointStart + 1, uri.length());
            e.sent = WvaUtil.dateTimeFromString(innerObj.getString("timestamp"));
            e.shortName = innerObj.getString("short_name");

        } catch (JSONException exc) {
            Log.i("WVALib/event", "Unable to create alarm event: malformed object");
            return null;
        }

        try {
            e.resp = new VehicleResponse(innerObj.getJSONObject(e.endpoint));
        } catch (JSONException exc) {
            Log.e(TAG, "VehicleResponse JSONException", exc);
            return null;
        }

		return e;
	}
	
    @SuppressWarnings("UnusedDeclaration")
    public String getType() {
		return this.type;
	}
	
	public VehicleResponse getResponse() {
		return this.resp;
	}
	
	public String getEndpoint() {
		return this.endpoint;
	}

    public DateTime getSent() {
        return sent;
    }

    public String getShortName() {
        return shortName;
    }
}
