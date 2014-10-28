/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test;

import android.app.Activity;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import com.digi.android.wva.*;
import com.digi.android.wva.R;
import com.digi.wva.WVA;

import static org.mockito.Mockito.mock;

public class DashboardActivityTest extends
		ActivityInstrumentationTestCase2<DashboardActivity> {
	WvaApplication mApp;

	public DashboardActivityTest() {
		super(DashboardActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		setActivityInitialTouchMode(false);

        // Inject IP address to connect to - guaranteed to fail.
        setActivityIntent(new Intent(getInstrumentation().getTargetContext(),
                            DashboardActivity.class).putExtra("ip_address", "256.256.256.1"));
		
		mApp = (WvaApplication) getActivity().getApplication();

        WVA dev = mock(WVA.class);
        mApp.setDevice(dev);
	}
	
	@Override
	protected void tearDown() throws Exception {
		// super doesn't destroy (i.e. finish) activity at end of any tests
		// (and by extension, doesn't end the activity at the end of all
		// these tests)
		getActivity().finish();
		
		super.tearDown();
	}
	
	public void testChartLaunch() {
		ActivityMonitor am = getInstrumentation().addMonitor(ChartActivity.class.getName(), null, false);
		Log.d("DashboardActivityTest", "Invoking Chart launch");
		getInstrumentation().invokeMenuActionSync(getActivity(), R.id.launch_chart, 0);
		Activity activity = getInstrumentation().waitForMonitorWithTimeout(am, 5000);
		assertTrue("Chart activity not launched!", getInstrumentation().checkMonitorHit(am, 1));

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
	
	public void testSettingsLaunch() {
		ActivityMonitor am = getInstrumentation().addMonitor(
				SettingsActivity.class.getName(), null, false);
		getInstrumentation().invokeMenuActionSync(getActivity(), R.id.action_settings, 0);
		
		Activity sa = getInstrumentation().waitForMonitorWithTimeout(am, 5000);
		assertTrue("Settings activity not launched",
				getInstrumentation().checkMonitorHit(am, 1));
        if (sa != null)
		    sa.finish();
	}
}
