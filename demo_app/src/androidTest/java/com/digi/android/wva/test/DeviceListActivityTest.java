/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test;

import android.app.Activity;
import android.app.Instrumentation.ActivityMonitor;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.actionbarsherlock.view.MenuItem;
import com.digi.addp.AddpClient;
import com.digi.addp.AddpDevice;
import com.digi.android.wva.DashboardActivity;
import com.digi.android.wva.DeviceListActivity;
import com.digi.android.wva.R;
import com.digi.android.wva.SettingsActivity;
import com.digi.android.wva.WvaApplication;
import com.digi.android.wva.adapters.EndpointsAdapter;
import com.digi.android.wva.adapters.LogAdapter;
import com.digi.android.wva.adapters.VariableAdapter;
import com.digi.android.wva.fragments.DeviceDiscoveryFragment;
import com.digi.android.wva.util.NetworkUtils;
import com.digi.android.wva.util.RefreshManager;
import com.digi.android.wva.util.VehicleDataList;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeviceListActivityTest
	extends ActivityInstrumentationTestCase2<DeviceListActivity> {
	private DeviceListActivity mActivity;
	private VariableAdapter mAdapter;
    private AddpClient client;
	
	private final List<AddpDevice> devices;

	public DeviceListActivityTest() throws Exception {
		super(DeviceListActivity.class);
		
		String[] mockNames =
				new String[] {"Mock Device 1", "Mock Device 2", "Mock Device 3", "Mock Device 4"};
		String[] mockIps =
				new String[] {"192.168.1.1", "192.168.2.1", "192.168.3.1", "192.168.4.1"};
		String[] mockDevIds =
				new String[] {null, null, null, null};
		// Set up mock devices to be returned in device discovery.
		devices = new ArrayList<AddpDevice>();
		for (int i = 0; i < mockNames.length; i++) {
			AddpDevice dev = mock(AddpDevice.class);
			when(dev.getDeviceID()).thenReturn(mockDevIds[i]);
			when(dev.getHardwareName()).thenReturn(mockNames[i]);
			when(dev.getIPAddress()).thenReturn(Inet4Address.getByName(mockIps[i]));
			devices.add(dev);
		}
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		setActivityInitialTouchMode(false);
		
		mActivity = getActivity();
		
		mAdapter = VariableAdapter.getInstance();
		
		// Mock the ADDP client, mocking all the methods called on it
		// and its returned objects so that we use our mock AddpDevice objects
		// added to 'devices'.
		client = mock(AddpClient.class);
		when(client.searchForDevices()).thenReturn(true);
		((WvaApplication)getActivity().getApplication()).setAddpClient(client);
		Map<String, AddpDevice> devMap = new Hashtable<String, AddpDevice>();
        for (AddpDevice d : devices) {
            // The actual getDevices return value maps MAC address to device, but
            // we just need a String->AddpDevice mapping here.
            devMap.put(d.getIPAddress().getHostName(), d);
        }
		when(client.getDevices()).thenReturn(devMap);
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		
		// super doesn't destroy (i.e. finish) activity
		getActivity().finish();
	}

    private void failWithoutNetwork() {
        if (!NetworkUtils.shouldBeAllowedToConnect(getActivity())) {
            fail("Not connected to Wi-Fi, so why bother testing?");
        }
    }
	
	public void testActivityOkay() {
        failWithoutNetwork();
		assertNotNull("Activity is null!", mActivity);
		assertNotNull("VariableAdapter not initialized", mAdapter);
	}
	
	public void testExistSingletons() {
        failWithoutNetwork();
		assertNotNull("Log adapter uninitialized",
								LogAdapter.getInstance());
		assertNotNull("Data list uninitialized",
								VehicleDataList.getInstance());
		assertNotNull("Variable adapter uninitialized", 
								VariableAdapter.getInstance());
		assertNotNull("Endpoints adapter uninitialized",
								EndpointsAdapter.getInstance());
	}

	public void testPressRefresh() {
        failWithoutNetwork();
        WvaApplication app = (WvaApplication) getActivity().getApplication();

        // Get handle to device list fragment
		final DeviceDiscoveryFragment f = (DeviceDiscoveryFragment) mActivity
									.getSupportFragmentManager()
									.findFragmentById(R.id.device_fragment);
		// Check that the fragment is there (why wouldn't it be?)
		assertNotNull("No R.id.device_fragment", f);

        // Make sure our mock ADDP client is being used.
        // It seems like the lifecycle of native fragments vs. support fragments
        // (i.e. fragments on 3.0/4.0+ vs. lower versions of Android) are
        // different... fragments are not created in these test cases until
        // after setUp() code has completed?
        // So we set the AddpClient for certain, here.
        if (app.getAddpClient() != client)
            app.setAddpClient(client);
		
		RefreshManager rm = f.getRefreshManager();
		// First, check the refresh manager exists (as it should)
		assertNotNull("Null refresh manager on fragment", rm);
		
		// Hit "Refresh"
        MenuItem refresher = f.getRefreshItem();
        while (refresher == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
            refresher = f.getRefreshItem();
        }
        final MenuItem theItem = refresher;

        // Execute onOptionsItemSelected from the main thread, so that
        // it can do what it normally does.
        app.getHandler().post(new Runnable() {
            public void run() {
                f.onOptionsItemSelected(theItem);
            }
        });

        // Sleep long enough for the async task (wherein the AddpClient search
        // will return immediately, because we mocked the AddpClient) to
        // complete and set the refresh manager back to not refreshing.

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {}

        assertTrue("Refresh manager is still refreshing. This may be expected, if the test device is slow.", rm.isNotRefreshing());

        verify(client).searchForDevices();
        verify(client).getDevices();
	}
	
	/**
	 *  Only exists for testing in-development app. Once the device discovery
	 *  and connection is all in place, manual device-info activity launch
	 *  will be removed.
	 */
	public void testManualLaunch() {
        failWithoutNetwork();
		// ala http://stackoverflow.com/q/5209154
		
		ActivityMonitor am = getInstrumentation().addMonitor(
				DashboardActivity.class.getName(), null, false);
		// send menu item invocation
		getInstrumentation().invokeMenuActionSync(mActivity, R.id.manual_view, 0);
		Log.d("DeviceListActivityTest", "menu action invoked");
		// Check that the dashboard activity was launched
		// note: waitForMonitorWithTimeout documentation is incorrect...
		// docs says timeout is in seconds, it is actually milliseconds
		Activity activity = getInstrumentation().waitForMonitorWithTimeout(am, 1000);
		assertTrue("Dashboard activity not launched",
				getInstrumentation().checkMonitorHit(am, 1));
		
		Log.d("DeviceListActivityTest", "Finishing dashboard activity");
		// finish dashboard activity
        if (activity != null)
		    activity.finish();
		try {
			// Seems like the code below gets called before onDestroy changes
			// propagate through the system if no delay like this is introduced.
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Check that hitting 'Settings' launches the Settings activity
	 */
	public void testSettingsLaunch() {
		ActivityMonitor am = getInstrumentation().addMonitor(
				SettingsActivity.class.getName(), null, false);
		getInstrumentation().invokeMenuActionSync(mActivity, R.id.action_settings, 0);
		
		Activity sa = getInstrumentation().waitForMonitorWithTimeout(am, 1000);
		assertTrue("Settings activity not launched",
				getInstrumentation().checkMonitorHit(am, 1));
        if (sa != null)
		    sa.finish();
	}
}
