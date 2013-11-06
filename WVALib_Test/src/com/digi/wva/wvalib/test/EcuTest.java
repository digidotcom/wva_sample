/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.wvalib.test;

import com.digi.wva.exc.EndpointUnknownException;
import com.digi.wva.device.Ecu;
import com.digi.wva.wvalib.test.auxiliary.HttpClientSpoofer;
import com.digi.wva.wvalib.test.auxiliary.JsonFactory;
import com.digi.wva.wvalib.test.auxiliary.PassFailCallback;
import junit.framework.TestCase;

import java.util.Map;
import java.util.Set;

public class EcuTest extends TestCase {
    HttpClientSpoofer httpClient = new HttpClientSpoofer("hostname");
    JsonFactory jFactory = new JsonFactory();
    Ecu testEcu = new Ecu(httpClient);

	protected void setUp() throws Exception {
        httpClient.returnObject = jFactory.ecuNames();
        httpClient.returnString = "";
        httpClient.success = true;

        testEcu.initialize(null);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testEcu() {
        Ecu testConstructor = new Ecu(httpClient);
        assertNotNull(testConstructor);
	}

	public void testInitialize() {
        PassFailCallback<Set<String>> cb1 = new PassFailCallback<Set<String>>();
        PassFailCallback<Set<String>> cb2 = new PassFailCallback<Set<String>>();
        PassFailCallback<Set<String>> cb3 = new PassFailCallback<Set<String>>();
        Ecu ecu = new Ecu(httpClient);

        // Failure due to onFailure being called
        httpClient.returnObject = jFactory.ecuNames();
        httpClient.returnString = "";
        httpClient.success = false;
        ecu.initialize(cb1);
        assertFalse(cb1.success);
        assertNull(cb1.response);

        // Failure due to bad data
        httpClient.returnObject = jFactory.junk();
        httpClient.success = true;
        ecu.initialize(cb2);
        assertFalse(cb2.success);
        assertNull(cb2.response);

        // Success (good data, onSuccess called)
        httpClient.returnObject = jFactory.ecuNames();
        httpClient.success = true;
        ecu.initialize(cb3);
        assertTrue(cb3.success);
        assertNotNull(cb3.response);
        assertTrue(cb3.response.size() > 0);
	}

	public void testDefineEcuEndpoints() {
        httpClient.returnObject = jFactory.ecuNames();
        testEcu.initialize(null);

        PassFailCallback<Set<String>> cb1 = new PassFailCallback<Set<String>>();
        PassFailCallback<Set<String>> cb2 = new PassFailCallback<Set<String>>();
        PassFailCallback<Set<String>> cb3 = new PassFailCallback<Set<String>>();
        PassFailCallback<Set<String>> cb4 = new PassFailCallback<Set<String>>();

        // Failure due to unknown endpoint
        httpClient.returnObject = jFactory.ecuEndpoints();
        httpClient.success = true;
        try {
            testEcu.defineEcuEndpoints("not an endpoint", cb1);
            fail("The endpoint 'not an endpoint' should not be known in EcuTest#testDefineEcuEndpoints");
        } catch (EndpointUnknownException e) { }

        // Failure due to onFailure being called
        httpClient.returnObject = jFactory.ecuEndpoints();
        httpClient.success = false;
        try {
            testEcu.defineEcuEndpoints("can0ecu0", cb2);
            assertFalse(cb2.success);
            assertNull(cb2.response);
        } catch (EndpointUnknownException e) {
            fail("No exception should have been thrown");
        }

        // Failure due to bad data
        httpClient.returnObject = jFactory.junk();
        httpClient.success = true;
        try {
            testEcu.defineEcuEndpoints("can0ecu0", cb3);
            assertFalse(cb3.success);
            assertNull(cb3.response);
        } catch (EndpointUnknownException e) {
            fail("No exception should have been thrown");
        }

        // Success (good data + onSuccess)
        httpClient.returnObject = jFactory.ecuEndpoints();
        httpClient.success = true;
        try {
            testEcu.defineEcuEndpoints("can0ecu0", cb4);
            assertTrue(cb4.success);
            assertTrue(cb4.response.size() > 0);
        } catch (EndpointUnknownException e) {
            fail("No exception should have been thrown");
        }
    }

	public void testFetchAllEndpoints() {
        httpClient.returnObject = jFactory.ecuEndpoints();
        httpClient.success = true;
        testEcu.initialize(null);
        try {
            testEcu.defineEcuEndpoints("can0ecu0", null);
        } catch (EndpointUnknownException e) {
            fail("defineEndpoints failed in EcuTest#testFetchAllEndpoints");
        }

        httpClient.returnObject = jFactory.ecuEndpoint();
        httpClient.success = true;
        PassFailCallback<Map<String, String>> cb1 = new PassFailCallback<Map<String, String>>();

        try {
            testEcu.fetchAllEndpoints("can0ecu0", cb1);
        } catch (EndpointUnknownException e) {
            fail("got exception when fetching all endpoints, can0ecu0 unknown");
        }

    }

	public void testGetCachedEcuNames() {
        httpClient.returnObject = jFactory.ecuEndpoints();
        httpClient.success = true;
        testEcu.initialize(null);
        try {
            testEcu.defineEcuEndpoints("can0ecu0", null);
        } catch (EndpointUnknownException e) {
            fail("defineEndpoints failed in EcuTest#testFetchAllEndpoints");
        }
	}

	public void testGetCachedEndpoints() {
        httpClient.returnObject = jFactory.ecuEndpoints();
        httpClient.success = true;
        testEcu.initialize(null);
        try {
            testEcu.defineEcuEndpoints("can0ecu0", null);
        } catch (EndpointUnknownException e) {
            fail("defineEndpoints failed in EcuTest#testFetchAllEndpoints");
        }
	}

	public void testGetCachedData() {
        httpClient.returnObject = jFactory.ecuEndpoints();
        httpClient.success = true;
        testEcu.initialize(null);
        try {
            httpClient.returnObject = jFactory.ecuEndpoint();
            testEcu.defineEcuEndpoints("can0ecu0", null);
        } catch (EndpointUnknownException e) {
            fail(e.toString());
        }
	}

}
