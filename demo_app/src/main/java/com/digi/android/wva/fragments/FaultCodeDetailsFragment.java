/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved.
 */

package com.digi.android.wva.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.digi.android.wva.R;
import com.digi.android.wva.WvaApplication;
import com.digi.wva.async.FaultCodeResponse;
import com.digi.wva.async.WvaCallback;
import com.digi.wva.async.FaultCodeCommon;
import com.digi.wva.WVA;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class FaultCodeDetailsFragment extends SherlockFragment {
    private static final DateTimeFormatter format = ISODateTimeFormat.dateTimeNoMillis();

    private FaultCodeCommon.Bus bus;
    private String ecuName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fault_code_details, null);

        Bundle arguments = getArguments();
        FaultCodeCommon.Bus bus = FaultCodeCommon.Bus.valueOf(arguments.getString("bus"));
        this.bus = bus;
        String ecuName = arguments.getString("ecu");
        this.ecuName = ecuName;

        TextView b = (TextView) v.findViewById(R.id.faultCodeDetail_bus);
        b.setText(bus.toString().toUpperCase());

        TextView ecu = (TextView) v.findViewById(R.id.faultCodeDetail_ecuName);
        ecu.setText(ecuName);

        TextView activeUri = (TextView) v.findViewById(R.id.active_uri);
        activeUri.setText(String.format("ws/vehicle/dtc/%s_active/%s", bus.toString(), ecuName));

        TextView inactiveUri = (TextView) v.findViewById(R.id.inactive_uri);
        inactiveUri.setText(String.format("ws/vehicle/dtc/%s_inactive/%s", bus.toString(), ecuName));

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final WVA device = ((WvaApplication) getActivity().getApplication()).getDevice();

        final TextView logs = (TextView) getView().findViewById(R.id.faultCodeLogs);
        final ScrollView scroll = (ScrollView) getView().findViewById(R.id.faultCodeScroller);

        ImageButton fetchActive = (ImageButton) getView().findViewById(R.id.fetchActive);
        ImageButton fetchInactive = (ImageButton) getView().findViewById(R.id.fetchInactive);

        final FaultCodeCommon.Bus bus = this.bus;
        final String ecuName = this.ecuName;

        fetchActive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "Fetching active fault code...", Toast.LENGTH_SHORT).show();

                device.fetchFaultCode(bus, FaultCodeCommon.FaultCodeType.ACTIVE, ecuName, new WvaCallback<FaultCodeResponse>() {
                    @Override
                    public void onResponse(Throwable error, FaultCodeResponse response) {
                        if (error != null) {
                            error.printStackTrace();
                            Toast.makeText(getActivity(), "Error fetching active fault code: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        } else if (response == null) {
                            logs.append("No active fault codes have been reported.\n");
                            scroll.fullScroll(View.FOCUS_DOWN);
                        } else {
                            logs.append(String.format("Active: %s at %s%n", response.getValue(), format.print(response.getTime())));
                            scroll.fullScroll(View.FOCUS_DOWN);
                        }
                    }
                });
            }
        });
        fetchInactive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "Fetching inactive fault code...", Toast.LENGTH_SHORT).show();

                device.fetchFaultCode(bus, FaultCodeCommon.FaultCodeType.INACTIVE, ecuName, new WvaCallback<FaultCodeResponse>() {
                    @Override
                    public void onResponse(Throwable error, FaultCodeResponse response) {
                        if (error != null) {
                            error.printStackTrace();
                            Toast.makeText(getActivity(), "Error fetching inactive fault code: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        } else if (response == null) {
                            logs.append("No inactive fault codes have been reported.\n");
                            scroll.fullScroll(View.FOCUS_DOWN);
                        } else {
                            logs.append(String.format("Inactive: %s at %s%n", response.getValue(), format.print(response.getTime())));
                            scroll.fullScroll(View.FOCUS_DOWN);
                        }
                    }
                });
            }
        });
    }
}
