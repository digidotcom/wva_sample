/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.model;

import android.text.TextUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Representation of log events within the application. Used to log
 * such events as new vehicle data arriving and alarms being triggered.
 */
public class LogEvent {
    /**
     * Message of the log event (to be displayed in the {@link com.digi.android.wva.adapters.LogAdapter}
     */
	public final String message;
    /**
     * Timestamp, when the event occurred
     */
    public final String timestamp;
    /**
     * Indicated whether this log event is recording that an alarm went off or not
     */
	public final boolean isAlarm;

    /**
     * Calls {@link #LogEvent(String, String, boolean)} with the third argument
     * being <b>false</b>.
     * @param message message of the event
     * @param timestamp timestamp of the event
     */
	public LogEvent(String message, String timestamp) {
        this(message, timestamp, false);
	}

    /**
     * Create a new LogEvent.
     *
     * <p>If the timestamp passed in is null, the current time will be used.</p>
     * @param message message of the event
     * @param timestamp timestamp of the event
     * @param alarm true if the event is to record that an alarm went off
     */
	public LogEvent(String message, String timestamp, boolean alarm) {
        if (TextUtils.isEmpty(timestamp)) {
            timestamp = ISODateTimeFormat.dateTimeNoMillis().print(DateTime.now());
        }
        this.message = message;
        this.timestamp = timestamp;
		isAlarm = alarm;
	}
}
