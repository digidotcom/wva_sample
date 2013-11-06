/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.async;

/**
 * <ol>
 * <li>
 * ABOVE:  Alarm triggered if tracked value raises above threshold
 *            Ex: Engine RPMs rise above 4500
 * </li>
 * <li>
 * BELOW:  Alarm triggered if tracked value falls below threshold
 *            Ex: Speed drops below 65mi/h
 * </li>
 * <li>
 * CHANGE: Alarm triggered if the tracked value changes
 *            Ex: Gear changes
 * </li>
 * <li>
 * DELTA:  Alarm triggered if the tracked value differs `threshold` amount between readings
 *            Ex: Gear changes from 6 to 3 since last reading, threshold == 2
 * </li>
 * </ol>
 *
 */
public enum AlarmType {
	ABOVE, BELOW, CHANGE, DELTA;

    /**
     * Takes an AlarmType enum value and produces the string used by the
     * WVA web services to represent that type. Use this as opposed to toString()
     *
     * @param t An AlarmType instance
     * @return The string corresponding to the type, or the empty string.
     */
	public static String makeString(AlarmType t) {
        if (t == null) {
            return "";
        }
		switch(t) {
		case ABOVE:
			return "above";
		case BELOW:
			return "below";
		case CHANGE:
			return "change";
		case DELTA:
			return "delta";
		default:
			return "";
		}
	}

    /**
     * Takes a string and creates the corresponding AlarmType, or null if the
     * string does not correspond to an AlarmType.
     * @param s The input string ("above", "below", "change", "delta")
     * @return an AlarmType instance
     */
	public static AlarmType fromString(String s) {
		if ("above".equalsIgnoreCase(s)) {
			return ABOVE;
		}
		else if ("below".equalsIgnoreCase(s)) {
			return BELOW;
		}
		else if ("change".equalsIgnoreCase(s)) {
			return CHANGE;
		}
		else if ("delta".equalsIgnoreCase(s)) {
			return DELTA;
		}
		else {
			return null;
		}
	}
	
}
