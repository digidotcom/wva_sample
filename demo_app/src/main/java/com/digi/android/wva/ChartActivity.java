/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva;

import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.digi.android.wva.fragments.ChartFragment;
import com.digi.android.wva.fragments.ConnectionErrorDialog;

/**
 * Activity which exists as a host for {@link ChartFragment}.
 */
public class ChartActivity extends SherlockFragmentActivity
                        implements ConnectionErrorDialog.ErrorDialogListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chart_activity);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);



	}

    @Override
    public void finish() {
        Log.d("ChartActivity", "Exiting graph view.");
        super.finish();
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return false;
	}

    /**
     * Implementation of the
     * {@link com.digi.android.wva.fragments.ConnectionErrorDialog.ErrorDialogListener#onOkay()} interface. Its
     * behavior here is to {@link #finish()} the activity.
     */
    @Override
    public void onOkay() {
        finish();
    }

    /**
     * Fetch the {@link ChartFragment} hosted by the activity. Useful in unit
     * testing.
     * @return the hosted {@link ChartFragment}
     */
    public ChartFragment getChartFragment() {
        return (ChartFragment)getSupportFragmentManager().findFragmentById(R.id.chart_fragment);
    }
}
