/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.fragments;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.digi.android.wva.R;
import com.digi.android.wva.adapters.LogAdapter;

/**
 * {@link ListFragment Fragment} used to display the contents of the
 * {@link LogAdapter} instance for this application -- that is, this is
 * the event log fragment for the demo app.
 * 
 * @author mwadsten
 *
 */
public class LogFragment extends ListFragment {
	private LogAdapter mAdapter;
    private boolean autoscroll;

    private final DataSetObserver observer = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            try {
                if (autoscroll)
                    getListView().smoothScrollToPosition(0);
            } catch (Exception e) {
                Log.w("LogFragment", "Caught exception in DataSetObserver: " + e);
            }
        }
    };

	public static LogFragment newInstance() {
//		Log.i("LogFragment", "newInstance");
        return new LogFragment();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
//		Log.i("LogFragment", "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
		setListAdapter(mAdapter);
	}

	@Override
	public void onDestroyView() {
//		Log.i("LogFragment", "onDestroyView");
		super.onDestroyView();
		setListAdapter(null);
	}

	// Override onResume (called when the containing activity
	// is resumed) so that the auto-scroll option can be changed
	// while connected to a device and will be reflected immediately
	// upon return to the activity.
	@Override
	public void onResume() {
		super.onResume();

        autoscroll = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getBoolean("pref_log_autoscroll", true);

        if (getListAdapter() != null)
            getListAdapter().registerDataSetObserver(observer);
	}

    @Override
    public void onPause() {
        super.onPause();

        if (getListAdapter() != null)
            getListAdapter().unregisterDataSetObserver(observer);
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
//		Log.i("LogFragment", "onCreate");
		super.onCreate(savedInstanceState);
		// Don't destroy fragment, or something.
		// stackoverflow.com/q/5704478
		setRetainInstance(true);
		
		mAdapter = (LogAdapter) getListAdapter();
		if (mAdapter == null)
			mAdapter = LogAdapter.getInstance();
		
		setListAdapter(mAdapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
//		Log.i("LogFragment", "onCreateView");
		View v = inflater.inflate(R.layout.list_fragment_with_header, null);
        assert v != null;
        TextView header = (TextView) v.findViewById(R.id.log_header);
		if (header != null) { // We're on a tablet
			header.setText(R.string.log_header);
		}

        TextView empty = (TextView)v.findViewById(android.R.id.empty);
        if (empty != null)
            empty.setText(R.string.empty_log_message);
		
		return v;
	}

}
