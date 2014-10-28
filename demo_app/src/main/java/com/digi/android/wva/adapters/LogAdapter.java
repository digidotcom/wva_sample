/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.digi.android.wva.R;
import com.digi.android.wva.model.LogEvent;
import com.digi.android.wva.model.VehicleData;

/**
 * Adapter for {@link com.digi.android.wva.fragments.LogFragment LogFragment}
 * to use in displaying the event log for the demo app.
 *
 * <p>Use <b>add(LogEvent)</b> (part of the {@link ArrayAdapter} interface)
 * to add new events directly. Use {@link #alarmTriggered(com.digi.android.wva.model.VehicleData)}
 * to record that an alarm has gone off.</p>
 * 
 * @author mwadsten
 *
 */
public class LogAdapter extends ArrayAdapter<LogEvent> {
	private final int resourceId;
	private final Context context;
	private static LogAdapter instance; // singleton

    /**
     * Initialize the singleton LogAdapter instance
     * @param ctx the context to be used
     */
	public static void initInstance(Context ctx) {
		if (instance == null)
			instance = new LogAdapter(ctx);
	}

    /**
     * Fetch the singleton LogAdapter instance
     * @return the singleton instance, or null
     */
	public static LogAdapter getInstance() {
		return instance;
	}
	
	/** Constructor is private to enforce singleton model. */
	private LogAdapter(Context context) {
		super(context, R.layout.log_event_list_item);
		this.context = context;
		this.resourceId = R.layout.log_event_list_item;
		// Automatically call notifyDataSetChanged when the data set changes
		setNotifyOnChange(true);
	}

    /**
     * We override ArrayAdapter's add method to call insert(event, 0), i.e.
     * to put new log events at the top of the list.
     * @param object {@link LogEvent} to add to the logs
     */
    @Override
    public void add(LogEvent object) {
        insert(object, 0);
    }

    /**
     * Add a new log event, to record that an alarm went off, related to
     * a piece of {@link VehicleData}.
     * @param data {@link VehicleData} pertaining to the alarm
     */
	public void alarmTriggered(VehicleData data) {
		String message = "Alarm: " + data.name + " = " + data.value;
		LogEvent event = new LogEvent(message, data.timestamp.toString(), true);
		add(event);
	}
	
	@Override
	public View getView(int pos, View view, ViewGroup parent) {
		if (view == null)
			view = LayoutInflater.from(context).inflate(resourceId, null);

        assert view != null;
        TextView message = (TextView)view.findViewById(R.id.log_msg);
		TextView timestamp = (TextView)view.findViewById(R.id.log_time);
		
		LogEvent event = getItem(pos);
		message.setText(event.message);
		timestamp.setText(event.timestamp);
		
		if (event.isAlarm)
			message.setTextColor(Color.parseColor("#ffdd0000"));
		else
			message.setTextColor(Color.parseColor("#aa000000"));
		
		return view;
	}

}
