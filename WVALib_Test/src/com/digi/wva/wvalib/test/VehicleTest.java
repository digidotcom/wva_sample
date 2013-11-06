/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.wvalib.test;

import com.digi.wva.async.Event;
import com.digi.wva.async.WvaListener;
import com.digi.wva.exc.EndpointUnknownException;
import com.digi.wva.device.Vehicle;
import com.digi.wva.async.AlarmType;
import com.digi.wva.async.VehicleResponse;
import com.digi.wva.device.WvaHttpClient;
import com.digi.wva.wvalib.test.auxiliary.HttpClientSpoofer;
import com.digi.wva.wvalib.test.auxiliary.JsonFactory;
import com.digi.wva.wvalib.test.auxiliary.PassFailCallback;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

import java.util.Set;

import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.*;

public class VehicleTest extends TestCase {
    HttpClientSpoofer httpClient = new HttpClientSpoofer("hostname");
    JsonFactory jFactory = new JsonFactory();
    Vehicle testVeh = new Vehicle(httpClient);


	protected void setUp() throws Exception {
        httpClient.returnObject = jFactory.vehicleEndpoints();
        testVeh.initialize(new PassFailCallback<Set<String>>());
        httpClient = new HttpClientSpoofer("hostname");
        jFactory = new JsonFactory();
        super.setUp();
    }

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testInitVehicle() {
        Vehicle newVehicle = new Vehicle(new WvaHttpClient("hostname2"));
        assertNotNull(newVehicle);
	}

	public void testInitialize() {
        Vehicle testInitVeh = new Vehicle(httpClient);
        httpClient.returnObject = jFactory.vehicleEndpoints();
        assertNotNull(httpClient.returnObject);
        httpClient.returnString = "";
        httpClient.success = true;
        PassFailCallback<Set<String>> cb1 = new PassFailCallback<Set<String>>();
        PassFailCallback<Set<String>> cb2 = new PassFailCallback<Set<String>>();


        testInitVeh.initialize(cb1);
        assertTrue(cb1.success);

        httpClient.success = false;
        testInitVeh.initialize(cb2);
        assertFalse(cb2.success);
	}

	public void testValidateEndpoint() {
        try {
            testVeh.validateEndpoint("EngineSpeed");
        } catch (EndpointUnknownException e) {
            fail("EngineSpeed should be a valid endpoint");
        }

        try {
            testVeh.validateEndpoint("Not a valid endpoint");
            fail("\"Not a valid Endpoint\" should not be a valid endpoint");
        } catch (EndpointUnknownException e) { }


    }

	public void testPOSITIVE_Subscribe_CreateAlarm() {
        testVeh.removeAllListeners();

        Event subEvent = Event.fromTCP(jFactory.data());
        Event alarmEvent = Event.fromTCP(jFactory.alarm());

        final boolean[] gotHereList = new boolean[2];
        gotHereList[0] = false;
        gotHereList[1] = false;
        addGotHereListeners(subEvent.getEndpoint(), gotHereList);
        testVeh.notifyListeners(subEvent);

        assertFalse(gotHereList[0]);
        assertTrue(gotHereList[1]);

        testVeh.notifyListeners(alarmEvent);

        assertTrue(gotHereList[0]);
        assertTrue(gotHereList[1]);
    }

	public void testUpdateCached_And_GetCached() throws JSONException {
        JSONObject valTimeObject = jFactory.valTimeObj();
        VehicleResponse resp = new VehicleResponse(valTimeObject);

        testVeh.updateCached("EngineSpeed", resp);
        VehicleResponse cached = testVeh.getCached("EngineSpeed");
        assertEquals(cached.time, resp.time);
        assertEquals(cached.value, resp.value);
	}

	public void testFetchNew() {
        httpClient.success = true;
        httpClient.returnObject = jFactory.vehicleDataEndpoint();
        httpClient.returnString = "";

        String endpoint = (String) (httpClient.returnObject.keys().next());
        assertEquals(endpoint, "EngineSpeed");

        // valid endpoint, successful response
        PassFailCallback<VehicleResponse> cb1 = new PassFailCallback<VehicleResponse>();
        try {
            testVeh.fetchNew(endpoint, cb1);
            //assertTrue(cb1.success);
        } catch (EndpointUnknownException e) {
            fail("unnecessary exception thrown for Vehicle.fetchNew");
        }

        // valid endpoint, unsuccessful response
        PassFailCallback<VehicleResponse> cb2 = new PassFailCallback<VehicleResponse>();
        httpClient.success = false;
        try {
            testVeh.fetchNew(endpoint, cb2);
            assertFalse(cb2.success);
        } catch (EndpointUnknownException e) {
            fail("unnecessary exception thrown for Vehicle.fetchNew");
        }

        // invalid endpoint
        PassFailCallback<VehicleResponse> cb3 = new PassFailCallback<VehicleResponse>();
        httpClient.success = true;
        try {
            testVeh.fetchNew("not a valid endpoint", cb3);
            assertFalse(cb3.success);
            fail("Vehicle.fetchNew should have thrown an error for invalid input");
        } catch (EndpointUnknownException e) { }

    }

	public void testUnsubscribe() throws JSONException {

        testVeh.removeAllListeners();

        httpClient.success = true;
        httpClient.returnObject = new JSONObject();
        Event e = Event.fromTCP(jFactory.data());

        final boolean[] gotHereList = new boolean[2];
        gotHereList[0] = false;
        gotHereList[1] = false;

        addGotHereListeners(e.getEndpoint(), gotHereList);

        testVeh.unsubscribe(e.getEndpoint(), false, null);
        testVeh.notifyListeners(e);
        assertFalse(gotHereList[0]);
        assertTrue(gotHereList[1]);


        gotHereList[0] = false;
        gotHereList[1] = false;
        testVeh.unsubscribe(e.getEndpoint(), true, null);
        testVeh.notifyListeners(e);
        assertFalse(gotHereList[0]);
        assertFalse(gotHereList[1]);

	}

	public void testDeleteAlarm() throws JSONException {
        testVeh.removeAllListeners();

        httpClient.success = true;
        httpClient.returnObject = new JSONObject();
        Event e = Event.fromTCP(jFactory.alarm());

        final boolean[] gotHereList = {false, false};

        addGotHereListeners(e.getEndpoint(), gotHereList);

        testVeh.deleteAlarm(e.getEndpoint(), AlarmType.ABOVE, false, null);
        testVeh.notifyListeners(e);
        assertTrue(gotHereList[0]);
        assertFalse(gotHereList[1]);


        gotHereList[0] = false;
        gotHereList[1] = false;
        testVeh.deleteAlarm(e.getEndpoint(), AlarmType.ABOVE, true, null);
        testVeh.notifyListeners(e);
        assertFalse(gotHereList[0]);
        assertFalse(gotHereList[1]);

	}

	public void testRemoveAllListeners() {
        Set<String> endpoints = testVeh.getKeySet();
        int numEndpoints = endpoints.size();
        boolean[][] gotHereLists;
        gotHereLists = new boolean[numEndpoints][2]; //booleans initialize to false

        int count = 0;
        for (String endpoint : endpoints) {
            addGotHereListeners(endpoint, gotHereLists[count++]);
        }

        testVeh.removeAllListeners();

        for (boolean[] list : gotHereLists) {
            assertFalse(list[0]);
            assertFalse(list[1]);
        }
	}

    /**
     * takes an endpoint (should be a part of testVeh), and adds a
     * subscription and an alarm listener. These listeners switch their
     * respective gotHereList index to `true' when executed.
     * @param endpoint
     * @param gotHereList
     */
    private void addGotHereListeners(final String endpoint, final boolean[] gotHereList) {

        WvaListener alarmListener = new WvaListener() {
            @Override
            public void onUpdate(String endpoint, VehicleResponse response) {
                gotHereList[0] = !gotHereList[0];
            }
        };

        WvaListener subListener = new WvaListener() {
            @Override
            public void onUpdate(String endpoint, VehicleResponse response) {
                gotHereList[1] = !gotHereList[1];
            }
        };

        try {
            testVeh.subscribe(endpoint, 10, subListener, null);
            testVeh.createAlarm(endpoint, AlarmType.ABOVE, 10, 40, alarmListener, null);
        } catch (JSONException e) {
            fail("No JSONException was expected in addGotHereListeners");

        } catch (EndpointUnknownException e) {
            fail(String.format("endpoint %s not in list %s", endpoint, testVeh.getKeySet().toString()));
        }
    }

}
