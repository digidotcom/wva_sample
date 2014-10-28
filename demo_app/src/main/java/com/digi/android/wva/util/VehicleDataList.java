/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.util;

import com.digi.android.wva.adapters.LogAdapter;
import com.digi.android.wva.adapters.VariableAdapter;
import com.digi.android.wva.model.LogEvent;
import com.digi.android.wva.model.VehicleData;

import java.util.ArrayList;
import java.util.List;

/**
 * Backing data structure for {@link VariableAdapter}.
 * 
 * <p>To use, call {@link #getInstance()} for a handle to the static instance
 * of the class, and call {@link #update(VehicleData)} or
 * {@link #update(VehicleData, int)} as needed.</p>
 * 
 * @author mwadsten
 *
 */
public class VehicleDataList {
	private static VehicleDataList instance;
	private final List<VehicleData> backingList;

    /**
     * Initialize the singleton VehicleDataList instance
     */
	public static void initInstance() {
		if (instance == null)
			instance = new VehicleDataList();
	}

    /**
     * Fetch the singleton VehicleDataList instance
     * @return the singleton instance, or null if there is none
     */
	public static VehicleDataList getInstance() {
		return instance;
	}
	
	private VehicleDataList() {
		backingList = new ArrayList<VehicleData>();
	}

    /**
     * Get the backing list of {@link VehicleData} objects
     * @return backing vehicle data list
     */
	public List<VehicleData> getList() {
		return backingList;
	}
	
	/**
	 * Search through the data and update the VehicleData object
	 * corresponding to newData. If not found, newData will be
	 * appended to the list.
	 *
     * @param newData VehicleData object to use to update data
	 */
	public void update(VehicleData newData) {
		update(newData, backingList.size());
	}
	
	/**
	 * Does the same as {@link #update(VehicleData) update(VehicleData)},
	 * except that if no matching VehicleData is found, newData will be
	 * added to the backing list at the given index (location) rather than
	 * the end of the list
	 * @param newData VehicleData object to use to update data
	 * @param location index to insert newData at, if needed
	 */
	public void update(VehicleData newData, int location) {
		VehicleData toUpdate = null;
		for (VehicleData data : backingList) {
			if (newData.name.equals(data.name)) {
				toUpdate = data;
				break;
			}
		}

		// Push log event with variable update
		LogAdapter logs = LogAdapter.getInstance();
		String eventMsg = String.format("%s = %s",
								newData.name, newData.value);
		logs.add(new LogEvent(eventMsg, newData.timestamp.toString()));
		logs.notifyDataSetChanged();
		
		if (toUpdate == null) {
			// No matching data in list to update. Insert newData
			// into list at the given index.
			backingList.add(location, newData);
		}
		else {
			try {
				toUpdate.update(newData);
			} catch (Exception e) {
				// This shouldn't happen, unless toUpdate's name was
				// changed suddenly (which shouldn't happen)
				e.printStackTrace();
			}
		}
	}
}
