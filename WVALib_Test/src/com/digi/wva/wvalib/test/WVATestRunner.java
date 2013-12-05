package com.digi.wva.wvalib.test;

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
