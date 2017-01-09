/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.digi.android.wva.adapters.EndpointsAdapter;
import com.digi.android.wva.adapters.LogAdapter;
import com.digi.android.wva.adapters.VariableAdapter;
import com.digi.android.wva.fragments.ConnectionErrorDialog;
import com.digi.android.wva.fragments.ConnectionErrorDialog.ErrorDialogListener;
import com.digi.android.wva.fragments.EndpointsFragment;
import com.digi.android.wva.fragments.LogFragment;
import com.digi.android.wva.fragments.PreConnectionDialog;
import com.digi.android.wva.fragments.PreConnectionDialog.PreConnectionDialogListener;
import com.digi.android.wva.fragments.VariableListFragment;
import com.digi.android.wva.util.MessageCourier;
import com.digi.wva.async.WvaCallback;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;


/**
 * Activity to be launched when the user selects a device to connect to.
 *
 * <p>On tablets, the three content fragments
 * ({@link VariableListFragment}, {@link LogFragment}, {@link EndpointsFragment})
 * are displayed all at once (see layouts <b>res/layout-sw600dp/dashboard</b>
 * and <b>res/layout-sw600dp-port/dashboard</b>).</p>
 *
 * <p>On phones (and mid-sized tablets?) the fragments will be displayed in a
 * {@link TabsAdapter} which allows the fragments to be paged through.</p>
 *
 * @author mwadsten
 *
 */
public class DashboardActivity extends SherlockFragmentActivity
								implements ErrorDialogListener, PreConnectionDialogListener {
	public static final String INTENT_IP = "ip_address";

	private static final String TAG = "DashboardActivity";
    private ViewPager mViewPager;

    private String mActionBarTitle;
	private String mActionBarSubtitle;

    private static final int MESSAGE_LOOP_INTERVAL = 2000;

    private class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            DashboardActivity.this.processMessages();
        }

        public void sleep() {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), MESSAGE_LOOP_INTERVAL);
        }
    }
    private final MessageHandler mHandler = new MessageHandler();
    private boolean isPaused = false;
    private boolean showIndeterminateProgress = true;

    protected void processMessages() {
        MessageCourier.DashboardMessage[] messages = MessageCourier.getDashboardMessages();
        for (MessageCourier.DashboardMessage message : messages) {
            if (message.isError()) {
                setIsConnecting(false);
                Log.d(TAG, "processMessages -- got error");
                showErrorDialog(message.getContents());
                return; // stop processing and handler loop
            } else if (message.isReconnecting() && !isPaused) {
                // Reconnecting to device, and we're not paused.
                setIsConnecting(true);
                Log.d(TAG, "processMessages -- got reconnecting");
                Toast.makeText(this, "Reconnecting...", Toast.LENGTH_SHORT).show();
            } else {
                Log.i(TAG, "Successfully connected to device.");
                setIsConnecting(false);
                Toast.makeText(DashboardActivity.this,
                        getString(R.string.connected_toast_contents),
                        Toast.LENGTH_SHORT).show();
                mActionBarTitle = getString(R.string.connected_dashboard_title);
                mActionBarSubtitle = message.getContents();
                setActionBarText();
            }
        }
        if (isPaused) {
            return;
        }
        mHandler.sleep();
    }

    @Override
	protected void onPause() {
        // Ensure messages stop being processed.
        mHandler.removeMessages(0);
        isPaused = true;
//		Log.i(TAG, "onPause");
		super.onPause();
	}

	@Override
	protected void onResume() {
//		Log.i(TAG, "onResume");
		super.onResume();

		WvaApplication app = (WvaApplication)getApplication();
		app.dismissAlarmNotification();

        isPaused = false;
        processMessages();
	}

    /**
     * <b>finish()</b> the activity, while also sending a
     * "disconnect" intent to the {@link VehicleInfoService} and
     * dismissing any alarm notifications
     */
	@Override
	public void finish() {
		// Tell VehicleInfoService to disconnect
		startService(VehicleInfoService.buildDisconnectIntent(
				getApplicationContext()));
		((WvaApplication)getApplication()).dismissAlarmNotification();
		super.finish();
	}

	@SuppressLint("CommitTransaction")
	protected void showErrorDialog(String error) {
		ConnectionErrorDialog dialog =
				ConnectionErrorDialog.newInstance(
                        getString(R.string.dashboard_error_dialog_title), error);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.addToBackStack(null);

		dialog.show(ft, "error_dialog");
	}

	/**
	 * Implementation of the ErrorDialogListener interface defined in
	 * ConnectionErrorDialog class. When the user acknowledges the
	 * error message, the dialog will automatically go away, and we
	 * want to finish the current dashboard activity and return to
	 * the device discovery activity.
	 */
	@Override
	public void onOkay() {
		navigateBackToDevices();
	}

	/**
	 * Implementation of the PreConnectionDialogListener interface defined
	 * in PreConnectionDialog class. When the user presses the "Okay" button
	 * in that dialog, the dialog will automatically go away, and we need to
	 * use the input from that dialog to configure the connection with the
	 * device (username, password, whether we need to use HTTPS, etc.).
	 */
    @Override
	public void onOkay(String ipAddress, String username, String password,
			boolean useHttps) {
    	// Use VehicleInfoService to connect to the device.
        startService(VehicleInfoService.buildConnectIntent(
        		getApplicationContext(), ipAddress, username, password, useHttps));
	}

    /**
     * Implementation of {@link PreConnectionDialogListener#onCancelConnection()}.
     * When the user presses the "Cancel" button in that dialog, the dialog will
     * automatically go away, and we want to respond by {@link #finish}ing the
     * DashboardActivity. (The user apparently wishes to cancel the attempted connection
     * with this device.)
     */
    @Override
    public void onCancelConnection() {
    	Toast.makeText(this, "Cancelled connection to " + getConnectionIp(),
    					Toast.LENGTH_SHORT).show();
    	navigateBackToDevices();
    }

	protected void setIsConnecting(boolean is) {
        setSupportProgressBarIndeterminateVisibility(is);
        showIndeterminateProgress = is;
    }

	protected String getConnectionIp() {
    	String ipAddr = getIntent().getStringExtra(INTENT_IP);

    	if (ipAddr == null) {
//    		Log.e(TAG, "Got intent with null ip address!");
    		ipAddr = PreferenceManager.getDefaultSharedPreferences(this)
    				.getString("pref_device_manual_ip", getString(R.string.default_ip));
    	}

    	return ipAddr;
	}

	@Override
    protected void onCreate(Bundle savedInstanceState) {
//		Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.dashboard);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // We want to wipe out variable and log data if this is a
        // fresh launch into this activity.
        if (savedInstanceState == null) {
//        	Log.i(TAG, "Clearing data in onCreate");
        	clearData();

            mActionBarTitle = getString(R.string.pre_connected_dashboard_title);

            // Send connect command to service...
        	String ipAddr = getConnectionIp();

        	FragmentManager fm = getSupportFragmentManager();
        	FragmentTransaction ft = fm.beginTransaction();
        	PreConnectionDialog dlg = PreConnectionDialog.newInstance(ipAddr);
            ft.addToBackStack(null);
            dlg.show(ft, "pre_connect");
        }
        else { // there is a saved instance state
            mActionBarTitle = savedInstanceState.getString("title");
            if (TextUtils.isEmpty(mActionBarTitle))
            	mActionBarTitle = getString(R.string.pre_connected_dashboard_title);
            mActionBarSubtitle = savedInstanceState.getString("subtitle");
            // If subtitle text is null, no subtitle will be added
            showIndeterminateProgress = savedInstanceState.getBoolean("indeterminate");
        }

        setActionBarText();

        // Set up the view pager if we are running on a device which will
        // display the view pager.
        mViewPager = (ViewPager)findViewById(R.id.pager);
        if (mViewPager != null) { // Running on a phone.
            TabsAdapter mTabsAdapter = new TabsAdapter(getSupportFragmentManager());
        	mViewPager.setAdapter(mTabsAdapter);

        	if (savedInstanceState != null)
        		mViewPager.setCurrentItem(savedInstanceState.getInt("page", 2));
        	else
        		mViewPager.setCurrentItem(2);
        }

        setIsConnecting(showIndeterminateProgress);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
//    	Log.i(TAG, "onSaveInstanceState");
    	super.onSaveInstanceState(outState);
        // Save view pager current page.
    	if (mViewPager != null)
    		outState.putInt("page", mViewPager.getCurrentItem());
    	outState.putString("title", mActionBarTitle);
    	outState.putString("subtitle", mActionBarSubtitle);
        outState.putBoolean("indeterminate", showIndeterminateProgress);
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	getSupportMenuInflater().inflate(R.menu.dashboard, menu);
    	return true;
	}

	protected static void clearData() {
    	VariableAdapter.getInstance().clear();
    	LogAdapter.getInstance().clear();
    	EndpointsAdapter.getInstance().clear();
        MessageCourier.clear();
    }

	/**
	 * Title and subtitle are stored in instance variables so that
	 * screen rotation, etc. can end with their contents restored.
	 */
	protected void setActionBarText() {
		getSupportActionBar().setTitle(mActionBarTitle);
		getSupportActionBar().setSubtitle(mActionBarSubtitle);
	}

	/**
	 * Uses NavUtils task stack builder to help make it so that we can leave
	 * the dashboard activity and go back to the device list.
	 */
	protected void navigateBackToDevices() {
//		Log.d(TAG, "navigateBackToDevices");

		Log.d(TAG, "Exiting dashboard, returning to device discovery.");
		((WvaApplication)getApplication()).clearDevice();

		// developer.android.com/training/implementing-navigation/ancestral.html
		Intent upIntent = new Intent(this, DeviceListActivity.class);
		if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
			// Create new task with synthesized back stack.
			TaskStackBuilder.create(this).addNextIntent(upIntent).startActivities();
			finish();
		}
		else {
			// Navigate up to the parent activity (DevicesActivity)
			// -- this is exactly the support library's implementation
			// of NavUtils.navigateUpTo(this, upIntent)... finish() wasn't
			// being called for some reason (at least on 4.2.2) and this
			// works better.
			upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(upIntent);
			finish();
		}
	}

	/**
	 * Make it so that when the user hits the back button, we explicitly
	 * return to the device list activity.
	 */
	@Override
	public void onBackPressed() {
		navigateBackToDevices();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			navigateBackToDevices();
			return true;
        case R.id.sync_time:
            Log.d(TAG, "Executing time sync.");
            WvaApplication app = (WvaApplication)getApplication();
            if (app.getDevice() != null)
                app.getDevice().setTime(DateTime.now(DateTimeZone.UTC), new WvaCallback<DateTime>() {
                    @Override
                    public void onResponse(Throwable error, DateTime response) {
                        Log.d(TAG, "Time sync error: " + error);
                        String message;
                        if (error == null)
                            message = "Time sync successful.";
                        else
                            message = "Time sync: " + error;
                        Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            else
                Log.d(TAG, "Can't execute time sync: no Device in WvaApplication");
            return true;
		// startActivityForResult should make it so that backing out (i.e. finish()ing)
		// from the launched activities returns us here to DashboardActivity
		case R.id.action_settings:
			startActivityForResult(new Intent(this, SettingsActivity.class), 0);
			return true;
		case R.id.launch_chart:
			startActivityForResult(new Intent(this, ChartActivity.class), 0);
			return true;
        case R.id.fault_codes:
            startActivity(new Intent(this, FaultCodeActivity.class));
            return true;
		}
		return false;
	}

	/**
	 * {@link FragmentPagerAdapter} implementation which is used when creating
	 * the dashboard activity on small screens. Allows the user to swipe between
	 * the variable data, log, and alarms/subscriptions fragments.
	 * @author mwadsten
	 *
	 */
	public class TabsAdapter extends FragmentPagerAdapter {
		public TabsAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				return VariableListFragment.newInstance();
			case 1:
				return LogFragment.newInstance();
			default:
				return EndpointsFragment.newInstance();
			}
		}

        /**
         * Get the title to put above a given page
         * @param pos page position
         * @return page title for the given page.
         */
		@Override
		public CharSequence getPageTitle(int pos) {
			switch (pos) {
			case 0:
				return getString(R.string.variables_header);
			case 1:
				return getString(R.string.log_header);
			default: // 2
				return getString(R.string.subscriptions_header);
			}
		}

        /**
         * Get the number of pages in the adapter.
         * @return 3 ({@link VariableListFragment}, {@link LogFragment},
         * {@link EndpointsFragment})
         */
		@Override
		public int getCount() {
			return 3;
		}

	}
}
