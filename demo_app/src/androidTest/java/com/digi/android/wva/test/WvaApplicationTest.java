/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test;

import android.test.ApplicationTestCase;
import com.digi.android.wva.WvaApplication;
import com.digi.wva.exc.EndpointUnknownException;
import com.digi.wva.WVA;
import com.digi.wva.async.AlarmType;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

/**
 * Created by mwadsten on 5/24/13.
 */
public class WvaApplicationTest extends ApplicationTestCase<WvaApplication> {
    WvaApplication app;
    WVA mockDevice;

    public WvaApplicationTest(Class<WvaApplication> applicationClass) {
        super(applicationClass);
    }

    public WvaApplicationTest() {
        this(WvaApplication.class);
    }

    @Override
    protected void setUp() throws Exception {
        createApplication();
        app = getApplication();
        WVA dev = mock(WVA.class);
        doNothing().when(dev).subscribeToVehicleData(anyString(), anyInt());
        doNothing().when(dev).unsubscribeFromVehicleData(anyString());
        doNothing().when(dev).createVehicleDataAlarm(anyString(), any(AlarmType.class), anyFloat(), anyInt());
        doNothing().when(dev).deleteVehicleDataAlarm(anyString(), any(AlarmType.class));

        mockDevice = dev;
        app.setDevice(dev);
    }

    public void testSubscribeDoesSubscribe() throws EndpointUnknownException {
        app.subscribeToEndpoint("Test", 0, null);

        verify(mockDevice).subscribeToVehicleData("Test", 0, null);
        verify(mockDevice).setVehicleDataListener(app.getDataListener());
    }

    public void testUnsubscribe() throws EndpointUnknownException {
        testSubscribeDoesSubscribe();
        app.unsubscribe("Test", null);
        verify(mockDevice).unsubscribeFromVehicleData("Test", null);
    }

    public void testAlarmCreatesAlarm() throws EndpointUnknownException {
        app.createAlarm("Test", AlarmType.ABOVE, 0.0, 5, null);
        verify(mockDevice).createVehicleDataAlarm("Test", AlarmType.ABOVE, 0.0f, 5, null);
        verify(mockDevice).setVehicleDataListener(app.getDataListener());
    }

    public void testRemoveAlarm() throws EndpointUnknownException {
        testAlarmCreatesAlarm();
        app.removeAlarm("Test", AlarmType.ABOVE, null);
        verify(mockDevice).deleteVehicleDataAlarm("Test", AlarmType.ABOVE, null);
    }
}
