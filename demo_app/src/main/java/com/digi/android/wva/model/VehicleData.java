/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.model;

import org.joda.time.DateTime;

/**
 * Simple model for representing new vehicle data from the WVA.
 */
public class VehicleData {
    /**
     * Endpoint name for this data
     */
	public final String name;
    /**
     * Value associated with this data point
     */
	public double value;
    /**
     * Timestamp of this data point
     */
	public DateTime timestamp;

    /**
     * Create a new piece of vehicle data
     * @param name endpoint name
     * @param value data value
     * @param timestamp timestamp of data --- if null, the current time will be used
     */
	public VehicleData(String name, double value, DateTime timestamp) {
        if (name == null)
            throw new NullPointerException("Can't create VehicleData with null name!");
        this.name = name;
        this.value = value;
        if (timestamp == null)
            timestamp = DateTime.now();
        this.timestamp = timestamp;
    }

    /**
     * Override the value and timestamp of this vehicle data to be those of
     * <b>newData</b>
     * @param newData vehicle data whose values will be used to update this
     * @throws Exception if the endpoint names don't match (and therefore we
     * shouldn't try to update the data)
     */
	public void update(VehicleData newData) throws Exception {
		if (this.name.equals(newData.name)) {
			this.value = newData.value;
			this.timestamp = newData.timestamp;
        }
		else {
			// If newData has a different name, updating this data
			// would be a bad idea.
			throw new Exception("Trying to update with different name! " + this.name + " vs. " + newData.name);
		}
	}
}
