/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.Log;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * The activity to which the user will navigate to change application
 * settings/preferences.
 */
public class SettingsActivity extends SherlockPreferenceActivity {
    private static final String INTERVAL_PREF = "pref_default_interval";

    SharedPreferences.OnSharedPreferenceChangeListener changeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (INTERVAL_PREF.equals(key)) {
                        setAutosubscribeSummary();
                    }
                }
            };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // This is deprecated because we're using a SherlockPreferenceActivity
        // without any PreferenceFragment instances. Oh well.
        //noinspection deprecation
        addPreferencesFromResource(R.xml.settings);

        setAutosubscribeSummary();
	}

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(changeListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(changeListener);
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

    private void setAutosubscribeSummary() {
        ListPreference pref = (ListPreference) findPreference(INTERVAL_PREF);
        if (pref == null)
            Log.d("SettingsActivity", "Auto-subscribe preference was null");
        else
            pref.setSummary(pref.getEntry());
    }
}
