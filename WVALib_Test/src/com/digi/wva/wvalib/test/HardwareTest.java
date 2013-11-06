/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.wvalib.test;

import com.digi.wva.exc.EndpointUnknownException;
import com.digi.wva.device.Hardware;
import com.digi.wva.wvalib.test.auxiliary.HttpClientSpoofer;
import com.digi.wva.wvalib.test.auxiliary.JsonFactory;
import com.digi.wva.wvalib.test.auxiliary.PassFailCallback;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

public class HardwareTest extends TestCase {
    HttpClientSpoofer httpClient = new HttpClientSpoofer("hostname");
    JsonFactory jFactory = new JsonFactory();
    Hardware testHw = new Hardware(httpClient);

	protected void setUp() throws Exception {
        super.setUp();

        httpClient.success = true;
        httpClient.returnObject = jFactory.buttonEndpoints();
        testHw.initializeButtons(new PassFailCallback<Set<String>>());

        httpClient.returnObject = jFactory.ledEndpoints();
        testHw.initializeLeds(new PassFailCallback<Set<String>>());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testHardware() {
        Hardware constructorTest = new Hardware(httpClient);
        assertNotNull(constructorTest); // this one is a gimme
	}

	public void testInitializeLeds() {
        Hardware testInit = new Hardware(httpClient);
        httpClient.returnObject = jFactory.ledEndpoints();
        httpClient.returnString = "";
        PassFailCallback<Set<String>> cb1 = new PassFailCallback<Set<String>>();
        PassFailCallback<Set<String>> cb2 = new PassFailCallback<Set<String>>();
        PassFailCallback<Set<String>> cb3 = new PassFailCallback<Set<String>>();

        // onFailure is called
        httpClient.success = false;
        testInit.initializeLeds(cb1);
        assertFalse(cb1.success);
        assertEquals(0, testInit.getLedSet().size());

        // onSuccess is called with bad/unparsable data
        httpClient.success = true;
        httpClient.returnObject = jFactory.junk();
        testInit.initializeLeds(cb2);
        assertFalse(cb2.success);
        assertEquals(0, testInit.getLedSet().size());

        // onSuccess is called with good data
        httpClient.success = true;
        httpClient.returnObject = jFactory.ledEndpoints();
        testInit.initializeLeds(cb3);
        assertTrue(cb3.success);
        try {
            assertEquals(cb3.response.size(),
                    httpClient.returnObject.getJSONArray("leds").length());
        } catch (JSONException e) {
            fail("this should not happen, key is in JsonFactory");
        }
	}

	public void testInitializeButtons() {
        Hardware testInit = new Hardware(httpClient);
        PassFailCallback<Set<String>> cb1 = new PassFailCallback<Set<String>>();
        PassFailCallback<Set<String>> cb2 = new PassFailCallback<Set<String>>();
        PassFailCallback<Set<String>> cb3 = new PassFailCallback<Set<String>>();

        // onFailure is called
        httpClient.success = false;
        testInit.initializeLeds(cb1);
        assertFalse(cb1.success);
        assertEquals(0, testInit.getLedSet().size());

        // onSuccess is called with bad/unparsable data
        httpClient.success = true;
        httpClient.returnObject = jFactory.junk();
        testInit.initializeLeds(cb2);
        assertFalse(cb2.success);
        assertEquals(0, testInit.getButtonSet().size());

        // onSuccess is called with good data
        httpClient.success = true;
        httpClient.returnObject = jFactory.buttonEndpoints();
        testInit.initializeButtons(cb3);
        assertTrue(cb3.success);
        try {
            assertEquals(cb3.response.size(),
                    httpClient.returnObject.getJSONArray("buttons").length());
        } catch (JSONException e) {
            fail("this should not happen, key is in JsonFactory");
        }
	}

	public void testGetButtonSet() {
        try {
            assertEquals(testHw.getButtonSet().size(),
                    jFactory.buttonEndpoints().getJSONArray("buttons").length());
        } catch (JSONException e) {
            fail("buttons stored in Hardware object do not match those in input data");
        }
	}

	public void testGetLedSet() {
        try {
            assertEquals(testHw.getLedSet().size(),
                    jFactory.ledEndpoints().getJSONArray("leds").length());
        } catch (JSONException e) {
            fail("buttons stored in Hardware object do not match those in input data");
        }
	}

	public void testFetchButtonState() {
        PassFailCallback<Boolean> cb1 = new PassFailCallback<Boolean>();
        PassFailCallback<Boolean> cb2 = new PassFailCallback<Boolean>();

        assertTrue(testHw.getButtonSet().contains("big_red"));
        // Endpoint exists
        try {
            httpClient.returnObject = jFactory.buttonState(true);
            testHw.fetchButtonState("big_red", cb1);
            assertTrue(cb1.success);
            assertTrue(cb1.response);

            httpClient.returnObject = jFactory.buttonState(false);
            testHw.fetchButtonState("big_red", cb1);
            assertTrue(cb1.success);
            assertFalse(cb1.response);

        } catch (EndpointUnknownException e) {
            fail("Endpoint should exist in fetchButtonState");
        }

        // Endpoint does not exist
        httpClient.returnObject = jFactory.buttonState(true);
        try {
            testHw.fetchButtonState("does not exist", cb2);
            fail("Endpoint should not exist in fetchButtonState");
        } catch (EndpointUnknownException e) {
            assertNull(cb2.success); // never called
        }
	}

	public void testFetchLedState() {
        PassFailCallback<Boolean> cb1 = new PassFailCallback<Boolean>();
        PassFailCallback<Boolean> cb2 = new PassFailCallback<Boolean>();

        // Endpoint exists
        try {
            httpClient.returnObject = jFactory.ledState(true);
            testHw.fetchLedState("led0", cb1);
            assertTrue(cb1.success);
            assertTrue(cb1.response);

            httpClient.returnObject = jFactory.ledState(false);
            testHw.fetchLedState("led0", cb1);
            assertTrue(cb1.success);
            assertFalse(cb1.response);

        } catch (EndpointUnknownException e) {
            fail("Endpoint should exist in fetchButtonState");
        }

        // Endpoint does not exist
        httpClient.returnObject = jFactory.ledState(true);
        try {
            testHw.fetchLedState("does not exist", cb2);
            fail("Endpoint should not exist in fetchButtonState");
        } catch (EndpointUnknownException e) {
            assertNull(cb2.success); // never called
        }
	}

	public void testSetLed() {
        httpClient.returnObject = new JSONObject();
        httpClient.returnString = "";

        PassFailCallback<Boolean> cb1 = new PassFailCallback<Boolean>();
        PassFailCallback<Boolean> cb2 = new PassFailCallback<Boolean>();
        PassFailCallback<Boolean> cb3 = new PassFailCallback<Boolean>();

        httpClient.success = true;
        try {
            testHw.setLed("led0", true, cb1);
            assertTrue(cb1.success);

            testHw.setLed("led0", false, cb2);
            assertTrue(cb2.success);

        } catch (JSONException e) {
            fail("There is no reason to experience a JSONException in setLed");
        } catch (EndpointUnknownException e) {
            fail("Endpoint 'led0' should be known for the sake of testSetLed");
        }

        try {
            testHw.setLed("not a valid led", true, cb3);
            fail("'not a valid led' is not a valid led name in testSetLed");

        }
        catch (EndpointUnknownException e) { }
        catch (JSONException e) { }
    }

    public void testFetchTime() {
        httpClient.returnObject = jFactory.time();
        httpClient.returnString = "";
        httpClient.success = true;

        PassFailCallback<DateTime> cb1 =  new PassFailCallback<DateTime>();

        try {
            String jFactTimeString = jFactory.time().getString("time");
            DateTimeFormatter dtFormat = ISODateTimeFormat.dateTimeParser();
            DateTime testTime = dtFormat.parseDateTime(jFactTimeString);

            testHw.fetchTime(cb1);
            assertTrue(cb1.success);
            assertEquals(testTime, cb1.response);

        } catch (JSONException e) {
            e.printStackTrace();
        }
	}

}
