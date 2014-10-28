/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved.
 */

package com.digi.android.wva;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.digi.android.wva.fragments.FaultCodeBrowsingFragment;
import com.digi.android.wva.fragments.FaultCodeDetailsFragment;
import com.digi.wva.async.FaultCodeCommon;

/**
 * An activity for browsing fault codes.
 */
public class FaultCodeActivity extends SherlockFragmentActivity implements FaultCodeBrowsingFragment.FaultCodeEcuSelectedListener {
    /** Set to true in onCreate if the loaded layout has space for two fragments. */
    private boolean twoColumns = false;
    private static final String BROWSE_FRAG_TAG = "browse_fragment",
                                DETAIL_FRAG_TAG = "details_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fault_code_activity);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle("Fault Code Browser");

        // Figure out if the layout has space for two fragments.
        twoColumns = (findViewById(R.id.faultCodeDetailFragment) != null);

        FragmentManager fm = getSupportFragmentManager();

        // FaultCodeBrowsingFragment uses setRetainInstance, so that a single fragment instance
        // can persist between configuration changes (e.g. screen rotation)
        if (fm.findFragmentByTag(BROWSE_FRAG_TAG) == null) { // Fresh instance of the activity
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.replace(R.id.faultCodeFragment, new FaultCodeBrowsingFragment(), BROWSE_FRAG_TAG);
            transaction.commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Either pop the details fragment, or end the activity.
                if (!getSupportFragmentManager().popBackStackImmediate()) {
                    finish();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSelect(FaultCodeCommon.Bus bus, String ecu) {
        FaultCodeDetailsFragment newFrag = new FaultCodeDetailsFragment();
        Bundle args = new Bundle();
        args.putString("bus", bus.toString().toUpperCase());
        args.putString("ecu", ecu);
        newFrag.setArguments(args);

        FragmentManager fm = getSupportFragmentManager();

        if (twoColumns) {
            fm.beginTransaction().replace(R.id.faultCodeDetailFragment, newFrag, DETAIL_FRAG_TAG).commit();
        } else {
            FragmentTransaction tx = fm.beginTransaction();
            tx.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            tx.addToBackStack(null);
            tx.replace(R.id.faultCodeFragment, newFrag, DETAIL_FRAG_TAG).commit();
        }
    }
}
