/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test;

import android.test.ApplicationTestCase;
import com.digi.android.wva.WvaApplication;
import com.digi.wva.exc.EndpointUnknownException;
import com.digi.wva.device.Device;
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
    Device mockDevice;

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
        Device dev = mock(Device.class);
        doNothing().when(dev).subscribe(anyString(), anyInt(), eq(app.getSubscriptionsListener()));
        doNothing().when(dev).unsubscribe(anyString(), anyBoolean());
        doNothing().when(dev).addAlarm(anyString(), any(AlarmType.class), anyInt(), anyFloat(), eq(app.getAlarmListener()));
        doNothing().when(dev).removeAlarm(anyString(), any(AlarmType.class), anyBoolean());

        mockDevice = dev;
        app.setDevice(dev);
    }

    public void testSubscribeDoesSubscribe() throws EndpointUnknownException {
        app.subscribeToEndpoint("Test", 0, null);
        verify(mockDevice).subscribe("Test", 0, app.getSubscriptionsListener(), null);
    }

    public void testUnsubscribe() throws EndpointUnknownException {
        testSubscribeDoesSubscribe();
        app.unsubscribe("Test", null);
        verify(mockDevice).unsubscribe("Test", true, null);
    }

    public void testAlarmCreatesAlarm() throws EndpointUnknownException {
        app.createAlarm("Test", AlarmType.ABOVE, 5, 0, null);
        verify(mockDevice).addAlarm("Test", AlarmType.ABOVE, 5, 0, app.getAlarmListener(), null);
    }

    public void testRemoveAlarm() throws EndpointUnknownException {
        testAlarmCreatesAlarm();
        app.removeAlarm("Test", AlarmType.ABOVE, null);
        verify(mockDevice).removeAlarm("Test", AlarmType.ABOVE, true, null);
    }
}
