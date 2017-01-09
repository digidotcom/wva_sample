/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
//import com.digi.connector.android.library.core.CloudConnectorManager;

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
	
	private final OnClickListener ccInstallListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// http://developer.android.com/distribute/googleplay/promote/linking.html#android-app
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("market://details?id=com.digi.connector.android"));
			startActivity(intent);
		}
	};

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_discovery);
        // We want the launcher icon to read "Digi WVA App" (or something
        // like that), but once we're in the app we'd rather the action bar
        // read something like "WVA Demo App". But, if we change the activity
        // title, that changes the launcher icon name. So, we'll just change
        // the action bar title here.
        getSupportActionBar().setTitle(R.string.app_name_sample_app);

        String versionName = ((WvaApplication)getApplication()).getApplicationVersion();
        getSupportActionBar().setSubtitle("Version " + versionName);
    }

    @Override
	protected void onResume() {
		super.onResume();
        
		// Check if the Cloud Connector is installed.
		// Do this in onResume so that the warning message can be cleared if the user
		// leaves, installs the Cloud Connector, and comes back.
        //CloudConnectorManager mCloudConnectorManager = ((WvaApplication)getApplication()).getCloudConnector();
		/*
		if (!mCloudConnectorManager.isCloudConnectorInstalled()) {
			TextView ccInstall = (TextView)findViewById(R.id.cloud_connector_install_warning);
			if (ccInstall != null) {
				// Tablet view. Make the warning visible.
				ccInstall.setVisibility(View.VISIBLE);
				
				ccInstall.setOnClickListener(ccInstallListener);
			}
		}
		*/
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
            startActivity(new Intent(this, DashboardActivity.class));
			return true;
		default:
			return false;
		}
	}
}
