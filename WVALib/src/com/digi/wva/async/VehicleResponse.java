/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.async;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * VehicleResponse objects are messages received from the data stream connection
 * with the device.
 */
public class VehicleResponse {
	private static final DateTimeFormatter formatMillis = ISODateTimeFormat.dateTime();
	private static final DateTimeFormatter format = ISODateTimeFormat.dateTimeNoMillis();
	public Double value;
	public DateTime time;

    /**
     * Creates an empty VehicleResponse object
     */
	public VehicleResponse() {
		value = null;
		time = null;
	}

    /**
     * Creates a new VehicleResponse object from a JSONObject.
     * @throws JSONException When the JSONObject received is not of the form
     *         {"value":<float>, "timestamp":<ISO8601 timestamp>}
     */
	public VehicleResponse(JSONObject jObj) throws JSONException {
		ISODateTimeFormat.dateTimeParser();
		this.value = jObj.getDouble("value");
		try {
			this.time = format.parseDateTime(jObj.getString("timestamp"));
		} catch (IllegalArgumentException e) {
			// Real WVA sends timestamps without milliseconds. If not connected
			// to "real" WVA (i.e. spoofer), we might be sending out timestamps
			// with milliseconds. In that case, parse it out here.
			this.time = formatMillis.parseDateTime(jObj.getString("timestamp"));
		}
	}
}

