/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved.
 */

package com.digi.android.wva.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.digi.android.wva.R;
import com.digi.android.wva.WvaApplication;
import com.digi.android.wva.adapters.FaultCodesAdapter;
import com.digi.wva.WVA;
import com.digi.wva.async.FaultCodeCommon;
import com.digi.wva.async.WvaCallback;
import com.digi.wva.exc.WvaHttpException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FaultCodeBrowsingFragment extends SherlockFragment implements FaultCodesAdapter.OnCanBusRefreshListener {
    private ExpandableListView listView;
    private FaultCodesAdapter adapter;

    private WVA device;

    private static final String TAG = "FaultCodeBrowsingFragment";

    public static interface FaultCodeEcuSelectedListener {
        public void onSelect(FaultCodeCommon.Bus bus, String ecu);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hold onto this instance of the fragment
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fault_code_browse_fragment, null);

        listView = (ExpandableListView) v.findViewById(R.id.expandableList);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (adapter == null) {
            // The fragment is being created for the first time, so we need to instantiate an
            // adapter as well.
            Activity holdingActivity = getActivity();
            adapter = new FaultCodesAdapter(holdingActivity, this, (WvaApplication)holdingActivity.getApplication());

            // Add each bus known to the library into the adapter.
            for (FaultCodeCommon.Bus canbus : FaultCodeCommon.Bus.values()) {
                adapter.putGroup(canbus.toString().toUpperCase(), new ArrayList<String>());
            }
        }

        this.device = ((WvaApplication) getActivity().getApplication()).getDevice();

        listView.setAdapter(adapter);

        // Call back into the activity when the user picks an ECU to look at
        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int groupPosition, int childPosition, long id) {
                assert getActivity() instanceof FaultCodeEcuSelectedListener;

                ((FaultCodeEcuSelectedListener)getActivity()).onSelect(adapter.getBusFromGroupPosition(groupPosition), (String)view.getTag());

                return true;
            }
        });

        // Load the ECU name list when the group is expanded for the first time (or there were no
        // ECUs the last time we checked)
        listView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int groupPosition, long id) {
                // isGroupExpanded will return the state of the group BEFORE taking this click into account.
                boolean isClosing = listView.isGroupExpanded(groupPosition);

                if (isClosing) {
                    return false;
                }

                if (adapter.getActualChildrenCount(groupPosition) == 0) {
                    // No ECUs currently. Let's automatically refresh that list.
                    onRefresh((FaultCodeCommon.Bus) view.getTag(), view);
                }

                return false;
            }
        });
    }

    @Override
    public void onRefresh(final FaultCodeCommon.Bus bus, final View headerView) {
        final String busName = bus.toString().toUpperCase();

        if (adapter.refreshingBuses.contains(bus)) {
            Log.d(TAG, "Already refreshing " + busName);
            return;
        }

        // Keep track that we're refreshing this bus's ECU list, so that the adapter can represent it
        // correctly on screen rotation, etc.
        adapter.refreshingBuses.add(bus);

        // Hide the button, show the progress indicator
        adapter.setRefreshState(headerView, true);

        device.fetchFaultCodeEcuNames(bus, new WvaCallback<Set<String>>() {
            @Override
            public void onResponse(Throwable error, Set<String> names) {
                if (error != null) {
                    String err;
                    if (error instanceof WvaHttpException.WvaHttpNotFound) {
                        Log.w(TAG, "404 error in fetchFaultCodeEcuNames");
                        err = "404 error fetching ECUs. Ensure your device is running firmware with support for fault codes.";
                    } else {
                        error.printStackTrace();
                        err = String.format("Error fetching %s ECUs: %s", busName, error.getMessage());
                    }
                    Toast.makeText(getActivity(), err, Toast.LENGTH_SHORT).show();
                } else {
                    List<String> nameList = new ArrayList<String>(names);
                    Collections.sort(nameList);
                    adapter.putGroup(busName, nameList);

                    Log.i("FaultCodeBrowsingFragment", String.format("Found %d ECUs on %s", names.size(), busName));
                }

                adapter.setRefreshState(headerView, false);
                adapter.notifyDataSetChanged();

                adapter.refreshingBuses.remove(bus);
            }
        });
    }
}
