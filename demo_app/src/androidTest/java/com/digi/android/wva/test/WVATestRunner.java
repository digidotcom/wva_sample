/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved.
 */

package com.digi.android.wva.test;

import android.os.Bundle;

import com.zutubi.android.junitreport.JUnitReportTestRunner;

public class WVATestRunner extends JUnitReportTestRunner {
	@Override
	public void onCreate(Bundle args) {
		// There seems to be some type of bug in Android 4.3 that prevents
		// Mockito/Dexmaker from working properly. This is a workaround.
		// https://code.google.com/p/dexmaker/issues/detail?id=2
		System.setProperty("dexmaker.dexcache", getTargetContext().getCacheDir().getAbsolutePath());
		super.onCreate(args);
	}
}
