/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.wvalib.test;

import com.digi.wva.async.WvaCallback;
import com.digi.wva.async.WvaListener;
import com.digi.wva.exc.EndpointUnknownException;
import com.digi.wva.device.Device;
import com.digi.wva.device.Ecu;
import com.digi.wva.device.Hardware;
import com.digi.wva.device.Vehicle;
import com.digi.wva.async.AlarmType;
import com.digi.wva.async.VehicleResponse;
import com.digi.wva.device.WvaHttpClient;

import com.digi.wva.wvalib.test.auxiliary.JsonFactory;
import junit.framework.TestCase;
import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DeviceTest extends TestCase {
    private static String hostname = "hostname";
    private static int port = 65534;

    // Mocks
    WvaHttpClient mClnt = mock(WvaHttpClient.class);
    Vehicle mVeh = mock(Vehicle.class);
    Hardware mHw = mock(Hardware.class);
    Ecu mEcu = mock(Ecu.class);
    WvaCallback<Set<String>> mCbSet = mock(WvaCallback.class);
    WvaListener mListener = mock(WvaListener.class);

    // Spies
    Vehicle vehSpy = null;

    // Auxiliary objects
    Set<String> endpoints = new HashSet<String>();
    JsonFactory jsonFactory = new JsonFactory();

    Device d1; // normal constructor
    Device d2; // factory method, mocked internals
    Device d3; // factory method, vehSpy internals

	protected void setUp() throws Exception {
        d1 = new Device(hostname, port);
        d2 = Device.getDevice(hostname, port, mClnt, mVeh, mEcu, mHw);

        Vehicle mVeh2 = new Vehicle(mClnt);
        vehSpy = spy(mVeh2);
        endpoints.add("DriverIncome");
        endpoints.add("PassengerEuphoria");
        d3 = Device.getDevice(hostname, port, mClnt, vehSpy, mEcu, mHw);

		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testDevice() throws Exception{
        assertNotNull(d1);
        assertNotNull(d2);
	}

	public void testInitVehicleData() throws Exception {
        d2.initVehicleData(mCbSet);
        verify(mVeh).initialize(any(WvaCallback.class));
	}

	public void testInitLeds() throws Exception {
        d2.initLeds(mCbSet);
        verify(mHw).initializeLeds(any(WvaCallback.class));
	}

	public void testInitButtons() throws Exception {
        d2.initButtons(mCbSet);
        verify(mHw).initializeButtons(any(WvaCallback.class));
	}

	public void testInitECUs() throws Exception {
        d2.initECUs(mCbSet);
        verify(mEcu).initialize(any(WvaCallback.class));
	}

    public void testSubscribe() {

        doReturn(endpoints).when(vehSpy).getKeySet();
        try {
            d3.subscribe("DriverIncome", 10, mListener);
            assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            d3.subscribe("PassengerIncome", 10, mListener);
            fail("should throw exception");
        } catch (EndpointUnknownException e) {
            assertTrue(true);
        }
    }

	public void testAddAlarm() {
        doReturn(endpoints).when(vehSpy).getKeySet();
        assertTrue(endpoints.contains("DriverIncome"));
        assertTrue(vehSpy.getKeySet().contains("DriverIncome"));
        try {
            d3.addAlarm("DriverIncome", AlarmType.ABOVE, 20, 10, mListener);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            d3.addAlarm("PassengerIncome", AlarmType.ABOVE, 20, 10, mListener);
            fail("should throw exception");
        } catch (EndpointUnknownException e) {
            assertTrue(true);
        }
	}

	public void testLastReceived() throws Exception {
        VehicleResponse lastResp = new VehicleResponse(jsonFactory.valTimeObj());
        when(mVeh.getCached("PassengerEuphoria")).thenReturn(lastResp);
        VehicleResponse newResp = d2.lastReceived("PassengerEuphoria");
        assertEquals(lastResp, newResp);

        doReturn(endpoints).when(vehSpy).getKeySet();
        VehicleResponse badResp = d3.lastReceived("not in endpoint set");
        assertNull(badResp);
	}

	public void testFetchSubscribable() throws Exception {
        WvaCallback<VehicleResponse> cbVR = mock(WvaCallback.class);
        d2.fetchSubscribable("Endpoint", cbVR);
        verify(mVeh).fetchNew("Endpoint", cbVR);
	}

	public void testSetTime() throws Exception {
        WvaCallback<DateTime> cbVR = mock(WvaCallback.class);
        DateTime time = new DateTime();
        d2.setTime(time, cbVR);
        verify(mHw).setTime(time, cbVR);
	}

	public void testGetTime() throws Exception {
        WvaCallback<DateTime> cbDT = mock(WvaCallback.class);
        d2.getTime(cbDT);
        verify(mHw).fetchTime(cbDT);
	}

	public void testSetLed() throws Exception {
        WvaCallback<Boolean> cbBool = mock(WvaCallback.class);
        d2.setLed("ledName", true, cbBool);
        verify(mHw).setLed("ledName", true, cbBool);
	}

	public void testGetLed() throws Exception {
        WvaCallback<Boolean> cbBool = mock(WvaCallback.class);
        d2.getLed("ledName", cbBool);
        verify(mHw).fetchLedState("ledName", cbBool);
	}

	public void testGetButton() throws Exception {
        WvaCallback<Boolean> cbBool = mock(WvaCallback.class);
        d2.getButton("buttonName", cbBool);
        verify(mHw).fetchButtonState("buttonName", cbBool);
	}

	public void testGetEcuEndpoints() throws Exception {
//        WvaCallback<String> cbStr = mock(WvaCallback.class);
//        d2.getEcuEndpoints("ecuName", cbStr);
//        verify(mEcu).defineEcuEndpoints("ecuName", cbStr);
	}

	public void testGetAllEcuData() throws Exception {
        WvaCallback<Map<String, String>> cbMap = mock(WvaCallback.class);
        d2.getAllEcuData("ecuName", cbMap);
        verify(mEcu).fetchAllEndpoints("ecuName", cbMap);
	}

}
