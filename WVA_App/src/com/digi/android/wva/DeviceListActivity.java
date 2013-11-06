/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.digi.android.wva.util.NetworkUtils;

/**
 * The starting activity for the demo application. Its main task is to
 * display the
 * {@link com.digi.android.wva.fragments.DeviceDiscoveryFragment DeviceFragment}
 * (which is responsible for discovering and displaying devices on the network)
 * and creating the options menu for the main screen.
 * 
 * @author mwadsten
 *
 */
public class DeviceListActivity extends SherlockFragmentActivity {
	private static final String TAG = "DeviceListActivity";

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_discovery);
        // We want the launcher icon to read "Digi WVA App" (or something
        // like that), but once we're in the app we'd rather the action bar
        // read something like "WVA Sample App". But, if we change the activity
        // title, that changes the launcher icon name. So, we'll just change
        // the action bar title here.
        getSupportActionBar().setTitle(R.string.app_name_sample_app);

        String versionName = ((WvaApplication)getApplication()).getApplicationVersion();
        getSupportActionBar().setSubtitle("Version " + versionName);
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	// Inflate "About" and "Manual" into the menu
    	getSupportMenuInflater().inflate(R.menu.devices, menu);
        return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			startActivityForResult(new Intent(this, SettingsActivity.class), 0);
			return true;
		case R.id.manual_view:
			if (NetworkUtils.shouldBeAllowedToConnect(getApplicationContext()))
				startActivity(new Intent(this, DashboardActivity.class));
			else {
				Log.e(TAG, "Not switching to manual mode - " +
											"Must be connected to WiFi or " +
											"serving hotspot to work.");

				Toast.makeText(DeviceListActivity.this,
						"Must be on Wi-Fi or serving as a hotspot "+
						"to use this application.", Toast.LENGTH_SHORT).show();
			}
			return true;
		default:
			return false;
		}
	}
}
