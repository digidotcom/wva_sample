/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.fragments;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.digi.addp.AddpClient;
import com.digi.addp.AddpDevice;
import com.digi.addp.AddpDeviceList;
import com.digi.android.wva.DashboardActivity;
import com.digi.android.wva.R;
import com.digi.android.wva.WvaApplication;
import com.digi.android.wva.adapters.DeviceAdapter;
import com.digi.android.wva.util.NetworkUtils;
import com.digi.android.wva.util.RefreshManager;

/**
 * {@link SherlockListFragment Fragment} used specifically in
 * {@link com.digi.android.wva.DeviceListActivity DeviceListActivity} to
 * use ADDP to discover devices on the network and display information about
 * them, as well as allow the user to select one of these devices to launch
 * {@link DashboardActivity} and connect to that device.
 * @author mwadsten
 *
 */
public class DeviceDiscoveryFragment extends SherlockListFragment {
	private static final String TAG = "DeviceDiscoveryFragment";
	private DeviceAdapter mAdapter;
	private RefreshManager mRefresh;
    private MenuItem mRefreshItem;
	private boolean isRefreshing;

    @SuppressWarnings("UnusedDeclaration")
	public static DeviceDiscoveryFragment newInstance() {
        return new DeviceDiscoveryFragment();
	}

    /**
     * Get the {@link RefreshManager} in use
     * @return the {@link RefreshManager} instance in use.
     */
	public RefreshManager getRefreshManager() {
		return mRefresh;
	}

    /**
     * Get the MenuItem used by the RefreshManager -- this is necessary
     * because pre-4.0 action bar menu items cannot be invoked by the
     * instrumentation invokeMenuActionSync, so we need to call
     * onOptionsItemSelected manually.
     * @return the {@link MenuItem} tied to the {@link RefreshManager}
     */
    public MenuItem getRefreshItem() {
        return mRefreshItem;
    }

    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
//		Log.i(TAG, "onActivityCreated, " + savedInstanceState);
		super.onActivityCreated(savedInstanceState);
		setListAdapter(mAdapter);
		
		setEmptyText(getString(R.string.empty_dev_message));
		
		// Don't reload devices if we already found some.
		// (Eventually loading devices will be controllable with
		// a refresh button in the menu so don't worry.)
		if (mAdapter.getCount() > 0) {
			return;
		}
	}

    @Override
	public void onDestroyView() {
//		Log.i(TAG, "onDestroyView");
		super.onDestroyView();
		setListAdapter(null);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
//		Log.i(TAG, "onCreate, " + savedInstanceState);
		// Need to create mRefresh here because on tablets (seemingly) the
		// action bar gets built during super.onCreate, so onCreate- and
		// onPrepareOptionsMenu are called before mRefresh is instantiated,
		// and the refresh action view won't appear until onPrepareOptionsMenu
		// is called again, if ever.
		mRefresh = new RefreshManager();
		super.onCreate(savedInstanceState);
		// Don't destroy fragment, or something.
		// stackoverflow.com/q/5704478
		setRetainInstance(true);
		
		mAdapter = (DeviceAdapter) getListAdapter();
		if (mAdapter == null)
			mAdapter = new DeviceAdapter(getActivity());
		
		setListAdapter(mAdapter);
		
		setHasOptionsMenu(true);

        // Set the application ADDP client.
        ((WvaApplication)getActivity().getApplication()).setAddpClient(new AddpClient());
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//		Log.i(TAG, "onCreateOptionsMenu");
		inflater.inflate(R.menu.devices_refresh, menu);
		mRefreshItem = menu.findItem(R.id.refresh);
		if (mRefresh != null)
			mRefresh.setIcon(mRefreshItem);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
//		Log.i(TAG, "onPrepareOptionsMenu");
		if (mRefresh == null)
			// We can't do much here if this is the case.
			return;
		
		MenuItem refresh = menu.findItem(R.id.refresh);
		mRefresh.setIcon(refresh);
		
		if (isRefreshing && mRefresh.isNotRefreshing()) {
			// RefreshManager not set to refreshing.
			// Most likely because options menu was not created
			// when startDiscovery was called.
//			Log.i("DeviceFragment", "calling setRefreshing again");
			mRefresh.setRefreshing(true);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if (!NetworkUtils.shouldBeAllowedToConnect(getActivity())) {
			complainAboutConnectivity();
			return;
		}
		AddpDevice device = (AddpDevice) l.getItemAtPosition(position);
        if (device == null) {
            // There is no item at position 'position'...
            return;
        }
        
        // Launch the DashboardActivity.
		Intent intent = new Intent(getActivity(), DashboardActivity.class);
		intent.putExtra(DashboardActivity.INTENT_IP, device.getIPAddress().toString());

		startActivity(intent);
	}
	
	private void complainAboutConnectivity() {
		Log.e(TAG, "Not on WiFi or acting as hotspot.");
		Toast.makeText(getActivity(),
				"Must be on Wi-Fi or serving as a hotspot to use this application.",
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			startDiscovery();
			return true;
		default:
			return false;
		}
	}
	
	private void startDiscovery() {
		if (NetworkUtils.shouldBeAllowedToConnect(getActivity())) {
			isRefreshing = true;
			mRefresh.setRefreshing(true);
            Log.d(TAG, "Starting discovery");
			new GetDevicesTask().execute();
			try {
				setListShown(false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			complainAboutConnectivity();
		}
	}
	
	private void endDiscovery(List<AddpDevice> devices) {
		mRefresh.setRefreshing(false);
		isRefreshing = false;
		// Clear adapter and set its contents.
		mAdapter.clear();
        for (AddpDevice el : devices)
            mAdapter.add(el);
		mAdapter.notifyDataSetChanged();
		try {
			setListShown(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private class GetDevicesTask extends AsyncTask<Void, Void, Enumeration<AddpDevice>> {
		protected Enumeration<AddpDevice> doInBackground(Void... nothings) {
            Log.d(TAG, "GetDevicesTask.doInBackground");
            AddpDeviceList devList;

            if (getActivity() == null) {
                Log.d(TAG, "getActivity() returned null in doInBackground");
                this.cancel(true);
                // return empty Enumeration, because we're not going to find
                // any devices anyway - can't get to an AddpClient.
                return new Enumeration<AddpDevice>() {
                    @Override
                    public AddpDevice nextElement() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public boolean hasMoreElements() {
                        // TODO Auto-generated method stub
                        return false;
                    }
                };
            }
            WvaApplication app = (WvaApplication) getActivity().getApplication();
            AddpClient addpClient = app.getAddpClient();

            if (addpClient == null) {
                // if the addp client is null, we don't want to do anything,
                // least of all trying to execute a discovery.
                Log.d(TAG, "No AddpClient!");
                this.cancel(true);
                // return empty Enumeration just in case cancel doesn't work
                return new Enumeration<AddpDevice>() {
                    @Override
                    public AddpDevice nextElement() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public boolean hasMoreElements() {
                        // TODO Auto-generated method stub
                        return false;
                    }
                };
            }

            if (addpClient.searchForDevices()) {
                devList = addpClient.getDevices();
            } else {
                devList = new AddpDeviceList();
            }

            Enumeration<AddpDevice> e = devList.elements();
            Log.d(TAG, "Discovered " + devList.size() + " devices.");
            return e;
        }

        protected void onPostExecute(Enumeration<AddpDevice> e) {
            if (isCancelled()) {
                Log.i(TAG, "Discovery task was cancelled.");
                return;
            }
            List<AddpDevice> lis = new ArrayList<AddpDevice>();
			
			while (e.hasMoreElements()) {
				AddpDevice dev = e.nextElement();
				lis.add(dev);
				Log.d(TAG, "Found device: "
							+ dev.getDeviceID() + " "
							+ dev.getHardwareName() + " "
							+ dev.getIPAddress().toString() );
			}
			
			endDiscovery(lis);
		}
		
	}


}
