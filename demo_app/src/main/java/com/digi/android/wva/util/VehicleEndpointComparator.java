/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved.
 */

package com.digi.android.wva.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VehicleEndpointComparator implements Comparator<String>, Serializable {
	/**
	 * The string prefixing any Pressure Pro endpoint.
	 */
	public static final String[] PRESSURE_PRO_PREFIXES = {"CTI", "TirePressure", "TireTemperature"};

	@Override
	public int compare(String ep1, String ep2) {
		boolean prefixed1 = false, prefixed2 = false;
		for (String s : PRESSURE_PRO_PREFIXES) {
			if (ep1.startsWith(s)) {
				prefixed1 = true;
			}
			if (ep2.startsWith(s)) {
				prefixed2 = true;
			}
		}
		
		if (prefixed1 && prefixed2) {
			// Sort lexicographically.
			return ep1.compareTo(ep2);
		} else if (prefixed1) {
			// ep2 is not prefixed, so push ep1 to follow ep2
			return 1;
		} else if (prefixed2) {
			// ep1 is not prefixed, so push ep2 to follow ep1
			return -1;
		} else {
			// Neither is prefixed - sort lexicographically
			return ep1.compareTo(ep2);
		}
	}
	
	/**
	 * Sort a collection of strings according to the VehicleEndpointComparator
	 * comparison algorithm.
	 * @param c a Collection of strings to be sorted
	 * @return a list, containing all elements of c, sorted by VehicleEndpointComparator
	 */
	public static List<String> asSortedList(Collection<String> c) {
		List<String> list = new ArrayList<String>(c);
		Collections.sort(list, new VehicleEndpointComparator());
		return list;
	}
}
