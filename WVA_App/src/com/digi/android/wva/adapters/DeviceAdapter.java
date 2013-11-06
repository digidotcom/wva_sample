/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.digi.addp.AddpDevice;
import com.digi.android.wva.R;

/**
 * An {@link ArrayAdapter} subclass which exists to list out and display
 * devices discovered via ADDP.
 *
 * <p>This class does not follow the singleton model exhibited by other
 * adapters in the application because the adapter only needs to live long
 * enough to display a list of devices and allow the user to select one
 * to move to the {@link com.digi.android.wva.DashboardActivity}. Plus, it
 * isn't all that expensive to set up a brand new instance each time.</p>
 */
public class DeviceAdapter extends ArrayAdapter<AddpDevice> {
	private final int resourceId;
	private final Context context;
	
	public DeviceAdapter(Context context) {
		super(context, R.layout.device_element_list_item);
		this.context = context;
		this.resourceId = R.layout.device_element_list_item;
	}
	
	@Override
	public View getView(int pos, View view, ViewGroup parent) {
		if (view == null)
			view = LayoutInflater.from(context).inflate(resourceId, null);

        // This suppresses Lint warnings, and can be useful in development to ensure
        // the proper layouts exist.
        assert view != null;
        TextView name = (TextView)view.findViewById(R.id.device_name);
		TextView ip = (TextView)view.findViewById(R.id.device_ip);
		
		AddpDevice devEl = getItem(pos);
		name.setText(devEl.getHardwareName());
		// Devices don't have to report device ID over ADDP. Handle that case.
		if (devEl.getDeviceID() != null)
			ip.setText(String.format("%s (ID: %s)", devEl.getIPAddress().toString(), devEl.getDeviceID()));
		else
			ip.setText(devEl.getIPAddress().toString());
		
		return view;
	}
}