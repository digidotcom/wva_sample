/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.digi.android.wva.R;
import com.digi.android.wva.model.EndpointConfiguration;
import com.digi.android.wva.model.EndpointConfiguration.SubscriptionConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link ArrayAdapter} subclass which lists out vehicle data endpoints
 * and provides an at-a-glance summary of endpoint configurations.
 */
public class EndpointsAdapter extends ArrayAdapter<EndpointConfiguration> {
	private static EndpointsAdapter instance;
	private final Context ctx;
	private final int resourceId;
	private final List<EndpointConfiguration> data;

    /**
     * Initialize the singleton EndpointsAdapter instance
     * @param context the context to be used
     */
	public static void initInstance(Context context) {
		if (instance == null)
			instance = new EndpointsAdapter(context);
	}

    /**
     * Fetch the singleton EndpointsAdapter instance, if there is one
     * @return the singleton instance, or null
     */
	public static EndpointsAdapter getInstance() {
		return instance;
	}
	
	private EndpointsAdapter(Context context) {
		super(context, R.layout.endpoints_list_item);
		this.ctx = context;
		this.resourceId = R.layout.endpoints_list_item;
		this.data = new ArrayList<EndpointConfiguration>();
	}
	
	@Override
	public View getView(int pos, View convertView, ViewGroup parent) {
		if (convertView == null)
			convertView = LayoutInflater.from(ctx).inflate(resourceId, null);
        assert convertView != null;
        TextView ept = (TextView)convertView.findViewById(R.id.endpoint_name);
		TextView alarm =
				(TextView)convertView.findViewById(R.id.endpoint_alarm_summary);
		
		EndpointConfiguration item = getItem(pos);
		SubscriptionConfig sc = item.getSubscriptionConfig();
        boolean subscribed = (sc != null && sc.isSubscribed());
		ept.setEnabled(subscribed);
		
		ept.setText(item.getTitleString());
		
		String alarmSummary = item.getAlarmSummary();
		if (alarmSummary == null) {
			alarm.setVisibility(View.GONE);
		}
		else {
			alarm.setVisibility(View.VISIBLE);
			alarm.setText(alarmSummary);
		}
		
		return convertView;
	}

    /**
     * Attempt to look up an {@link EndpointConfiguration} in this
     * adapter, by endpoint name
     * @param endpoint endpoint name to look up
     * @return matching EndpointConfiguration, or null if there is none
     */
	public EndpointConfiguration findEndpointConfiguration(String endpoint) {
		for (EndpointConfiguration c : data) {
			if (c.getEndpoint().equals(endpoint))
				return c;
		}
		return null;
	}

	public void add(EndpointConfiguration newEntry, boolean notify) {
		data.add(newEntry);
		if (notify) {
			notifyDataSetChanged();
		}
	}
	
	// ArrayAdapter overrides
	
	@Override
	public void add(EndpointConfiguration newEntry) {
		add(newEntry, true);
	}
	
	@Override
	public void clear() {
		data.clear();
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return data.size();
	}
	
	@Override
	public EndpointConfiguration getItem(int position) {
		return data.get(position);
	}
	
	@Override
	public int getPosition(EndpointConfiguration item) {
		return data.indexOf(item);
	}
	
	@Override
	public void insert(EndpointConfiguration item, int index) {
		data.add(index, item);
		notifyDataSetChanged();
	}
	
	@Override
	public void remove(EndpointConfiguration item) {
		if (data.remove(item))
			notifyDataSetChanged();
		else {
			Log.e("EndpointsAdapter", "data.remove() came back false");
		}
	}

}
