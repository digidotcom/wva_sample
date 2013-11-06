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
import com.digi.android.wva.R;
import com.digi.android.wva.model.EndpointConfiguration;
import com.digi.android.wva.model.VehicleData;
import com.digi.android.wva.util.VehicleDataList;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;

/**
 * Adapter used by the
 * {@link com.digi.android.wva.fragments.VariableListFragment
 * 			VariableListFragment}
 * to display the vehicle data which has been received.
 * 
 * @author mwadsten
 *
 */
public class VariableAdapter extends ArrayAdapter<VehicleData> {
	private final int resourceId;
	private final Context ctx;
	private final VehicleDataList data;
	private static VariableAdapter instance;

    /**
     * Initialize the singleton VariableAdapter instance
     * @param context the context to use
     * @param list the backing {@link VehicleDataList} to use
     */
	public static void initInstance(Context context, VehicleDataList list) {
		if (instance == null)
			instance = new VariableAdapter(context, list);
	}

    /**
     * Fetch the singleton VariableAdapter instance, if there is one
     * @return the singleton instance, or null if none exists
     */
	public static VariableAdapter getInstance() {
		return instance;
	}

	/** Constructor is private to enforce singleton model. */
	private VariableAdapter(Context context,
                            VehicleDataList list) {
		super(context, R.layout.variable_layout);
		this.ctx = context;
		this.resourceId = R.layout.variable_layout;
		data = list;
	}
	
	// Overrides of ArrayAdapter methods to interact with the backing
	// VehicleDataList
	
	/**
	 * Updates the VehicleData object inside the adapter's backing
	 * list if found, otherwise adds `object` to the end of the list.
	 */
	@Override
	public void add(VehicleData object) {
		// If 'object' can correspond to something already in the
		// backing list, update its data rather than blindly inserting
		// it at the end of the list.
		data.update(object);
		notifyDataSetChanged();
	}

    /**
     * Iterates over the backing data list to find any data whose name
     * matches that passed in to this method, and removes that data from
     * the list
     * @param endpoint endpoint name whose corresponding {@link VehicleData}
     *                 shall be removed
     */
	public void removeEndpoint(String endpoint) {
		List<VehicleData> lis = data.getList();
		VehicleData toRemove = null;
		for (VehicleData d : lis)
			if (d.name.equals(endpoint)) {
				toRemove = d;
				break;
			}
		if (toRemove != null)
			data.getList().remove(toRemove);
		notifyDataSetChanged();
	}
	
	/**
	 * Calls {@link #add(VehicleData) add()} for each VehicleData in
	 * collection.
	 */
	@Override
	public void addAll(Collection<? extends VehicleData> collection) {
		for(VehicleData object : collection) {
			add(object);
		}
	}
	
	/**
	 * Does nothing. Don't use this.
	 */
	@Override
	public void addAll(VehicleData... objects) {
	}
	
	/**
	 * Clears the backing {@link VehicleDataList} instance.
	 */
	@Override
	public void clear() {
		data.getList().clear();
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return data.getList().size();
	}
	
	@Override
	public VehicleData getItem(int position) {
		return data.getList().get(position);
	}
	
	@Override
	public int getPosition(VehicleData item) {
		int pos = -1;
		for(int i = 0; i < getCount(); i++) {
			if (item.name.equals(data.getList().get(i).name))
				pos = i;
		}
		return pos;
	}
	
	@Override
	public void insert(VehicleData object, int index) {
		data.update(object, index);
		notifyDataSetChanged();
	}
	
	@Override
	public void remove(VehicleData object) {
		VehicleData toRemove = null;
		for(VehicleData o : data.getList()) {
			if (object.name.equals(o.name)) {
				toRemove = o;
				break;
			}
		}
		if (toRemove != null) {
			data.getList().remove(toRemove);
			notifyDataSetChanged();
		}
	}
	
	@Override
	public View getView(int pos, View view, ViewGroup parent) {
		if (view == null)
			view = LayoutInflater.from(ctx).inflate(resourceId, null);

        assert view != null;
        TextView name = (TextView)view.findViewById(R.id.var_name);
		TextView val = (TextView)view.findViewById(R.id.var_value);
		
		VehicleData i = getItem(pos);
		String n = i.name,
			   v = roundToThree(i.value);
		name.setText(n != null ? n : "Blah");
		val.setText(v != null ? v : "50");

        // Gray out the entry if we're not subscribed to the endpoint.
        name.setEnabled(true);
        val.setEnabled(true);
        EndpointsAdapter endpoints = EndpointsAdapter.getInstance();
        if (endpoints != null) { // a sanity check
            EndpointConfiguration config = endpoints.findEndpointConfiguration(n);
            if (config == null || !config.isSubscribed()) {
                name.setEnabled(false);
                val.setEnabled(false);
            }
        }
		return view;
	}
	
	private String roundToThree(Double value) {
		return new DecimalFormat("#.###").format(value);
	}
}