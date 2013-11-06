/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test;

import android.test.ActivityInstrumentationTestCase2;

import com.digi.android.wva.SettingsActivity;

public class SettingsActivityTest extends
		ActivityInstrumentationTestCase2<SettingsActivity> {

	public SettingsActivityTest() {
		super(SettingsActivity.class);
	}

	public void testNothing() {
		getActivity().finish();
	}
}
