/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved.
 */

package com.digi.android.wva.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.digi.android.wva.R;
import com.digi.android.wva.WvaApplication;
import com.digi.wva.async.FaultCodeCommon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FaultCodesAdapter extends BaseExpandableListAdapter {
    private final Context context;
    private final WvaApplication app;
    private List<String> groups;
    public final List<FaultCodeCommon.Bus> refreshingBuses;
    private HashMap<String, List<String>> ecus;
    private OnCanBusRefreshListener listener;

    public static interface OnCanBusRefreshListener {
        public void onRefresh(FaultCodeCommon.Bus bus, View headerView);
    }

    public FaultCodesAdapter(Context context, OnCanBusRefreshListener listener, WvaApplication app) {
        this.context = context;
        this.listener = listener;
        this.app = app;

        this.groups = new ArrayList<String>();
        this.refreshingBuses = new ArrayList<FaultCodeCommon.Bus>();
        this.ecus = new HashMap<String, List<String>>();
    }

    public List<String> putGroup(String group, List<String> ecus) {
        if (!this.groups.contains(group))
            this.groups.add(group);
        return this.ecus.put(group, ecus);
    }

    @Override
    public int getGroupCount() {
        return groups.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        int actual = getActualChildrenCount(groupPosition);
        return actual == 0 ? 1 : actual;
    }

    public int getActualChildrenCount(int groupPosition) {
        List<String> children = this.ecus.get(this.groups.get(groupPosition));
        return children == null ? 0 : children.size();
    }

    public FaultCodeCommon.Bus getBusFromGroupPosition(int groupPosition) {
        return FaultCodeCommon.Bus.valueOf(this.groups.get(groupPosition));
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.groups.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.ecus.get(this.groups.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    public void setRefreshState(final View groupView, boolean isRefreshing) {
        ProgressBar progress = (ProgressBar) groupView.findViewById(R.id.progressBar);
        ImageButton refreshBtn = (ImageButton) groupView.findViewById(R.id.refreshButton);

        // Hide the button, show the progress indicator
        refreshBtn.setVisibility(isRefreshing ? View.GONE : View.VISIBLE);
        progress.setVisibility(isRefreshing ? View.VISIBLE : View.GONE);
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup viewGroup) {
        final String headerTitle = (String) getGroup(groupPosition);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.fault_code_list_group, null);
        }

        int children = getActualChildrenCount(groupPosition);
        // Create the string to display the number of ECUs
        String ecuCount = (children > 0) ? context.getResources().getQuantityString(R.plurals.numberOfECUs, children, children) : "";

        // Update the CAN bus name
        final TextView header = (TextView) convertView.findViewById(R.id.groupHeader);
        header.setTypeface(null, Typeface.BOLD);
        header.setText(headerTitle);

        // Update the counter
        TextView count = (TextView) convertView.findViewById(R.id.ecuCount);
        count.setText(ecuCount);

        // Make the refresh button reload the list.
        ImageButton refreshBtn = (ImageButton) convertView.findViewById(R.id.refreshButton);
        refreshBtn.setFocusable(false);

        final FaultCodeCommon.Bus bus = getBusFromGroupPosition(groupPosition);
        // Make this header view appear to be refreshing, if it is doing so.
        setRefreshState(convertView, refreshingBuses.contains(bus));

        // Set the tag on the group view, so we can easily retrieve that from the enclosing fragment
        // in its onGroup[Expand/Collapse]Listeners.
        convertView.setTag(bus);

        final View groupView = convertView;

        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (listener != null)
                    listener.onRefresh(bus, groupView);
            }
        });

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isExpanded, View convertView, ViewGroup viewGroup) {
        boolean useStub = getActualChildrenCount(groupPosition) == 0;

        String ecuName = useStub ? "No ECUs. Hit the Refresh button." : (String) getChild(groupPosition, childPosition);
        if (useStub && refreshingBuses.contains(getBusFromGroupPosition(groupPosition))) {
            // We're already fetching, so change the text.
            ecuName = "Please wait, loading ECU name list...";
        }

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.fault_code_list_item, null);
        }

        TextView name = (TextView) convertView.findViewById(R.id.faultCodeListItem);
        name.setText(ecuName);

        convertView.setTag(ecuName);

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        // Make a child view (ECU item) clickable only if it is truly an ECU, and not the stub
        // view directing the user to refresh (or whatnot)
        return getActualChildrenCount(groupPosition) > 0;
    }
}
