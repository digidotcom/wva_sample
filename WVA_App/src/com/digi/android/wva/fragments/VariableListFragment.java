/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.fragments;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.digi.android.wva.R;
import com.digi.android.wva.adapters.VariableAdapter;

/**
 * {@link ListFragment Fragment} used to display the contents of the
 * {@link VariableAdapter} instance for the application -- that is,
 * this is the fragment which shows the vehicle data in the demo app.
 * 
 * @author mwadsten
 *
 */
public class VariableListFragment extends ListFragment {
	private VariableAdapter mAdapter;
	
	public static VariableListFragment newInstance() {
		return new VariableListFragment();
	}
	
	@Override
	public void onActivityCreated(Bundle savedState) {
		super.onActivityCreated(savedState);
		setListAdapter(mAdapter);
	}
	
	@Override
	public void onDestroyView() {
//		Log.i("VariableListFragment", "onDestroyView");
		super.onDestroyView();
		setListAdapter(null);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
//		Log.i("VariableListFragment", "onCreateView");
		View v = inflater.inflate(R.layout.list_fragment_with_header, null);
        assert v != null;
        TextView header = (TextView)v.findViewById(R.id.log_header);
		if (header != null)
			header.setText(R.string.variables_header);
		TextView empty = (TextView)v.findViewById(android.R.id.empty);
		if (empty != null)
			empty.setText("No vehicle data.");

		return v;
	}
	
	@Override
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		setRetainInstance(true);
		
		mAdapter = (VariableAdapter)getListAdapter();
		if (mAdapter == null)
			mAdapter = VariableAdapter.getInstance();

		setListAdapter(mAdapter);
	}

}
