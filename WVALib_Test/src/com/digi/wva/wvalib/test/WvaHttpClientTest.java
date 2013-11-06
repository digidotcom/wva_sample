/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.wvalib.test;

import com.digi.wva.device.WvaHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import junit.framework.TestCase;
import org.json.JSONObject;

/**
 * This test case is rather light, because most functionality relies
 * on the com.loopj library (http://loopj.com/android-async-http/),
 * which in turn relies on the Apache HttpClient libraries. Therefore,
 * most of these tests are just assuring the interface hasn't changed.
 */
public class WvaHttpClientTest extends TestCase {
    String HOSTNAME = "hostname";
    WvaHttpClient httpClientTest;

	protected void setUp() throws Exception {
        httpClientTest = new WvaHttpClient(HOSTNAME);
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testWvaHttpClient() {
        WvaHttpClient constructorTest = new WvaHttpClient(HOSTNAME);
        assertNotNull(constructorTest);
	}

	public void testGet() {
        try {
            httpClientTest.get("url", new AsyncHttpResponseHandler());
        }catch (Exception e) {
            fail("got exception in WvaHttpClient#testGet");
        }
	}

	public void testPut() {
        try {
            httpClientTest.put("url", new JSONObject(), new AsyncHttpResponseHandler());
        }catch (Exception e) {
            fail("got exception in WvaHttpClient#testPut");
        }
	}

	public void testDelete() {
        try {
            httpClientTest.delete("url", new AsyncHttpResponseHandler());
        }catch (Exception e) {
            fail("got exception in WvaHttpClient#testDelete");
        }
	}

	public void testPost() {
        try {
            httpClientTest.post("url", new JSONObject(), new AsyncHttpResponseHandler());
        }catch (Exception e) {
            fail("got exception in WvaHttpClient#testPost");
        }
	}

	public void testGetAbsoluteUrl() {
        String absUrl = httpClientTest.getAbsoluteUrl("xyz");
        assertEquals(absUrl, "http://" + HOSTNAME + "/ws/xyz");
	}

}
