/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.digi.android.wva.R;
import com.digi.android.wva.adapters.EndpointsAdapter;
import com.digi.android.wva.model.EndpointConfiguration;

/**
 * {@link Fragment} used to display information about alarms and subscriptions
 * that have been set up. Currently the implementation simply displays
 * "No subscriptions." but in the future we will actually display useful
 * information.
 * @author mwadsten
 *
 */
public class EndpointsFragment extends ListFragment {
	private static final String DLG_TAG = "ept_cfg_dialog";
	
	public static EndpointsFragment newInstance() {
		return new EndpointsFragment();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		if (getListAdapter() == null)
			setListAdapter(EndpointsAdapter.getInstance());
	}
	
	// This Lint suppression is because .show() commits the transaction, but
	// Lint is not aware of that, so it warns that the fragment transaction is
	// not committed.
	@SuppressLint("CommitTransaction")
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		FragmentManager fm = getActivity().getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		// Look for pre-existing dialog and remove it if it exists.
		// This is needed because if you click an endpoint twice or more in
		// rapid succession, it WILL trigger this function more than once, and
		// without this check, more than one fragment dialog will appear.
		Fragment prev = fm.findFragmentByTag(DLG_TAG);
		if (prev != null)
			ft.remove(prev);
		ft.addToBackStack(null);
		
		EndpointConfiguration conf =
				(EndpointConfiguration) getListAdapter().getItem(position);
		
		new EndpointOptionsDialog().setConfig(conf).show(ft, DLG_TAG);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
//		Log.i("AlarmsFragment", "onCreateView");
		View v = inflater.inflate(R.layout.list_fragment_with_header, null);
        assert v != null;
        TextView header = (TextView)v.findViewById(R.id.log_header);
		if (header != null)
			header.setText(R.string.subscriptions_header);
		
		// Same as calling setEmptyText at whatever is the appropriate time to do that
        TextView empty = (TextView)v.findViewById(android.R.id.empty);
        if (empty != null)
            empty.setText("No endpoints.");
		return v;
	}
}
