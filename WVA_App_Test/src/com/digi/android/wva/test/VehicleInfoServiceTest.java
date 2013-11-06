/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.ServiceTestCase;
import com.digi.android.wva.VehicleInfoService;
import com.digi.android.wva.WvaApplication;
import com.digi.android.wva.util.MessageCourier;
import com.digi.wva.async.WvaCallback;
import com.digi.wva.device.Device;
import com.digi.wva.device.DeviceConnectionListener;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * Unit tests related to the VehicleInfoService.
 *
 * NOTE: Don't run these tests (well, really, don't run the app tests) on Froyo.
 * The ServiceTestCase implementation on Froyo differs from that of more recent
 * releases, specifically in startService, in such a way that these test cases
 * will fail on Froyo.
 *
 * Created by mwadsten on 5/29/13.
 */
public class VehicleInfoServiceTest extends ServiceTestCase<VehicleInfoService> {
    private WvaApplication app;
    private Device device;
    private String[] names;

    public VehicleInfoServiceTest(Class<VehicleInfoService> serviceClass) {
        super(serviceClass);
    }

    public VehicleInfoServiceTest() {
        this(VehicleInfoService.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        MessageCourier.clear();
        app = mock(WvaApplication.class);
        setApplication(app);

        doNothing().when(app).subscribeToEndpoint(anyString(), anyInt(), any(WvaCallback.class));
        doNothing().when(app).listNewEndpoint(anyString());
        doCallRealMethod().when(app).getHandler();
        doCallRealMethod().when(app).setDevice(any(Device.class));
        doCallRealMethod().when(app).getDevice();
        doNothing().when(app).clearDevice();
        when(app.isTesting()).thenReturn(true);

        final Set<String> endpoints = new HashSet<String>();
        names = new String[] {
                "Speed", "RPM", "Temperature", "Light", "Humidity", "Seat Belt"
        };
        endpoints.addAll(Arrays.asList(names));

        device = mock(Device.class);
        doNothing().when(device).unsubscribe(anyString(), anyBoolean());
        doNothing().when(device).disconnectDataStream();

        setVehicleInitResponse(null, endpoints);

        setConnectDataStreamError(null);

        app.setDevice(device);

        // The ServiceTestCase code on Froyo does not like it when a service
        // is already started/created when startService is called. Calling
        // or not calling start() should have no effect on the actual behavior
        // of the app (and in fact, the whole APP_CREATE code might just be
        // removed in the end), but this is a workaround in the meantime.
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.FROYO)
            start();
    }

    private void setVehicleInitResponse(final Exception e, final Set<String> endpoints) {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                WvaCallback<Set<String>> cb = (WvaCallback<Set<String>>) invocationOnMock.getArguments()[0];
                cb.onResponse(e, endpoints);
                return null;
            }
        }).when(device).initVehicleData(any(WvaCallback.class));
    }

    private void setConnectDataStreamError(final IOException e) {
        if (e == null) {
            doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    DeviceConnectionListener l = (DeviceConnectionListener) invocationOnMock.getArguments()[1];
                    l.onConnected(device);
                    return null;
                }
            }).when(device).connectDataStream(anyInt(), any(DeviceConnectionListener.class));
        }
        else {
            doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    DeviceConnectionListener l = (DeviceConnectionListener) invocationOnMock.getArguments()[1];
                    l.onError(device, e);
                    return null;
                }
            }).when(device).connectDataStream(anyInt(), any(DeviceConnectionListener.class));
        }
    }

    /**
     * Call {@link #startService(android.content.Intent)} with
     * {@link VehicleInfoService#buildCreateIntent(android.content.Context)}
     * result, so as to make sure the service exists.
     */
    private void start() {
        startService(VehicleInfoService.buildCreateIntent(getContext()));
    }

    public void testValidateCreateIntent() {
        Intent i = VehicleInfoService.buildCreateIntent(getContext());

        assertEquals("Wrong intent command on create intent",
                VehicleInfoService.CMD_APPCREATE,
                i.getIntExtra(VehicleInfoService.INTENT_CMD, -1));
        assertEquals("Wrong class", VehicleInfoService.class.getName(),
                i.getComponent().getClassName());
    }

    public void testValidateConnectIntent() {
        Intent i = VehicleInfoService.buildConnectIntent(getContext(), "192.168.255.1");
        assertEquals("Wrong IP address on intent", "192.168.255.1",
                i.getStringExtra(VehicleInfoService.INTENT_IP));
        assertEquals("Wrong intent command on connect intent",
                VehicleInfoService.CMD_CONNECT,
                i.getIntExtra(VehicleInfoService.INTENT_CMD, -1));
        assertEquals("Wrong class", VehicleInfoService.class.getName(),
                i.getComponent().getClassName());
    }

    public void testValidateDisconnectIntent() {
        Intent i = VehicleInfoService.buildDisconnectIntent(getContext());
        assertEquals("Wrong intent command on disconnect intent",
                VehicleInfoService.CMD_DISCONNECT,
                i.getIntExtra(VehicleInfoService.INTENT_CMD, -1));
        assertEquals("Wrong class", VehicleInfoService.class.getName(),
                i.getComponent().getClassName());
    }

    /**
     * Does a lot of mocking operations to make it so that calling
     * startService with a "connect" intent will execute immediately
     * (read: synchronously), thereby allowing us to validate the code
     * inside the initVehicleData callback and the connectDataStream callback
     * therein.
     */
    public void testConnectNoErrors() {
        // Set up the preferences as needed.
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editor.putString("pref_device_port", "5000");
        editor.putBoolean("pref_auto_subscribe", true);
        editor.putString("pref_default_interval", "10");
        editor.commit();

        MessageCourier.clear();

        startService(VehicleInfoService.buildConnectIntent(getContext(), "0.0.0.0"));

        assertSame("Wrong device used", getService().getDevice(), device);

        verify(device).initVehicleData(any(WvaCallback.class));
        verify(device).connectDataStream(eq(5000), any(DeviceConnectionListener.class));
        // Verify that subscribeToEndpoint was called for each endpoint
        for (int i = 0; i < names.length; i++) {
            verify(app).subscribeToEndpoint(eq(names[i]), eq(10), any(WvaCallback.class));
        }

        MessageCourier.DashboardMessage[] msgs = MessageCourier.getDashboardMessages();
        assertEquals("Incorrect number of dashboard messages", 1, msgs.length);
        assertFalse("Dashboard message that was sent is an error!", msgs[0].isError());
        assertEquals("Wrong contents on connected message!", "0.0.0.0", msgs[0].getContents());
        MessageCourier.ChartMessage[] cmsgs = MessageCourier.getChartMessages();
        assertEquals("There are messages for the chart!", 0, cmsgs.length);

        assertEquals("Wrong connection IP address", "0.0.0.0", getService().getConnectionIpAddress());
        assertTrue("Not 'connected'", getService().isConnected());
    }

    public void testDisconnect() {
        // We almost wouldn't need to use mock devices and applications for
        // testing the disconnect call, since it doesn't do anything
        // asynchronously. But this is a useful way of testing that the
        // results of "connecting" to a mock device here can be undone
        // with the disconnect command.

        // No testing of this call needs to be done - that is in testConnectNoErrors
        startService(VehicleInfoService.buildConnectIntent(getContext(), "0.0.0.0"));

        startService(VehicleInfoService.buildDisconnectIntent(getContext()));

        verify(device).disconnectDataStream();
        verify(app).setDevice(null);
        assertNull("Service still has old device", getService().getDevice());
        assertFalse("Service still reports connected", getService().isConnected());
    }

    public void testInitWithErrorMessage() {
        Exception e = mock(Exception.class);

        when(e.getMessage()).thenReturn("Useful Error Message");

        setVehicleInitResponse(e, null);

        MessageCourier.clear();

        startService(VehicleInfoService.buildConnectIntent(getContext(), "0.0.0.0"));

        verify(device, never()).connectDataStream(anyInt(), any(DeviceConnectionListener.class));
        verify(app, never()).subscribeToEndpoint(anyString(), anyInt(), any(WvaCallback.class));
        verify(app).setDevice(null);
        verify(e).getMessage();
        MessageCourier.DashboardMessage[] msgs = MessageCourier.getDashboardMessages();
        assertEquals("No dashboard messages!", 1, msgs.length);
        assertTrue("Non-error dashboard message!", msgs[0].isError());
        assertEquals("Wrong dashboard error!", "Useful Error Message", msgs[0].getContents());
    }

    public void testInitWithErrorCause() {
        Exception e = mock(Exception.class);

        when(e.getMessage()).thenReturn(null);
        when(e.getCause()).thenReturn(new Exception("Another Error"));

        setVehicleInitResponse(e, null);

        MessageCourier.clear();

        startService(VehicleInfoService.buildConnectIntent(getContext(), "0.0.0.0"));

        verify(device, never()).connectDataStream(anyInt(), any(DeviceConnectionListener.class));
        verify(app, never()).subscribeToEndpoint(anyString(), anyInt(), any(WvaCallback.class));
        verify(app).setDevice(null);
        verify(e).getMessage();
        // Need to use atLeast(1), because (seemingly) the logging statement
        // invokes getCause() (or perhaps that is in printStackTrace or something)
        verify(e, atLeast(1)).getCause();
        MessageCourier.DashboardMessage[] msgs = MessageCourier.getDashboardMessages();
        assertEquals("No dashboard messages!", 1, msgs.length);
        assertTrue("Non-error dashboard message!", msgs[0].isError());
        assertEquals("Wrong dashboard error!", "Another Error", msgs[0].getContents());
    }

    public void testInitWithErrorToString() {
        Exception e = mock(Exception.class);

        when(e.getMessage()).thenReturn(null);
        when(e.getCause()).thenReturn(null);
        when(e.toString()).thenReturn("The Error");

        setVehicleInitResponse(e, null);

        MessageCourier.clear();

        startService(VehicleInfoService.buildConnectIntent(getContext(), "0.0.0.0"));

        verify(device, never()).connectDataStream(anyInt(), any(DeviceConnectionListener.class));
        verify(app, never()).subscribeToEndpoint(anyString(), anyInt(), any(WvaCallback.class));
        verify(app).setDevice(null);
        verify(e).getMessage();
        // Need to use atLeast(1), because (seemingly) the logging statement
        // invokes getCause() (or perhaps that is in printStackTrace or something)
        verify(e, atLeast(1)).getCause();
        // Mockito won't let us verify toString()
        MessageCourier.DashboardMessage[] msgs = MessageCourier.getDashboardMessages();
        assertEquals("No dashboard messages!", 1, msgs.length);
        assertTrue("Non-error dashboard message!", msgs[0].isError());
        assertEquals("Wrong dashboard error!", "The Error", msgs[0].getContents());
    }

    // These tests worked, and made sense, when the connectDataStream API
    // was asynchronous.

//    public void testConnectDataStreamErrorMessage() {
//        IOException e = mock(IOException.class);
//        when(e.getMessage()).thenReturn("Error Message");
//
//        setConnectDataStreamError(e);
//
//        startService(VehicleInfoService.buildConnectIntent(getContext(), "0.0.0.0"));
//
//        verify(e).getMessage();
//        verify(app).setDevice(null);
//        assertFalse("Service reports connected", getService().isConnected());
//        MessageCourier.DashboardMessage[] msgs = MessageCourier.getDashboardMessages();
//        assertEquals("No dashboard messages!", 1, msgs.length);
//        assertTrue("Non-error dashboard message!", msgs[0].isError());
//        assertEquals("Wrong dashboard error!", "Error Message", msgs[0].getContents());
//    }
//
//    public void testConnectDataStreamErrorCause() {
//        IOException e = mock(IOException.class);
//        when(e.getMessage()).thenReturn(null);
//        when(e.getCause()).thenReturn(new Exception("The cause"));
//
//        setConnectDataStreamError(e);
//
//        startService(VehicleInfoService.buildConnectIntent(getContext(), "0.0.0.0"));
//
//        verify(e).getMessage();
//        verify(e, atLeast(1)).getCause();
//        verify(app).setDevice(null);
//        assertNull("Service has old device", getService().getDevice());
//        assertFalse("Service reports connected", getService().isConnected());
//        MessageCourier.DashboardMessage[] msgs = MessageCourier.getDashboardMessages();
//        assertEquals("No dashboard messages!", 1, msgs.length);
//        assertTrue("Non-error dashboard message!", msgs[0].isError());
//        assertEquals("Wrong dashboard error!", "The cause", msgs[0].getContents());
//    }
//
//    public void testConnectDataStreamErrorToString() {
//        IOException e = mock(IOException.class);
//        when(e.getMessage()).thenReturn(null);
//        when(e.getCause()).thenReturn(null);
//        when(e.toString()).thenReturn("An Error");
//
//        setConnectDataStreamError(e);
//
//        startService(VehicleInfoService.buildConnectIntent(getContext(), "0.0.0.0"));
//
//        verify(e).getMessage();
//        verify(e, atLeast(1)).getCause();
//        // Mockito won't let us verify toString()
//        verify(app).setDevice(null);
//        assertNull("Service has old device", getService().getDevice());
//        assertFalse("Service reports connected", getService().isConnected());
//        MessageCourier.DashboardMessage[] msgs = MessageCourier.getDashboardMessages();
//        assertEquals("No dashboard messages!", 1, msgs.length);
//        assertTrue("Non-error dashboard message!", msgs[0].isError());
//        assertEquals("Wrong dashboard error!", "An Error", msgs[0].getContents());
//    }

//    public void testCheckConnectionRunnable() {
//        Runnable r = getService().getConnectionLoopRunnable();
//        assertEquals("MessageCourier should be empty!", 0, MessageCourier.getDashboardMessages().length);
//        assertEquals("MessageCourier should be empty!", 0, MessageCourier.getChartMessages().length);
//        assertFalse("Service should report false for isConnected()", getService().isConnected());
//        // Service should start out in a "disconnected" state.
//        r.run();
//        assertFalse("checkConnection set isConnected to true!", getService().isConnected());
//
//        // "Connect" the service so that we get to the second condition.
//        IOException e = new IOException("Fail.");
//        when(device.isDataStreamDisconnected()).thenReturn(true);
//        when(device.dataStreamException()).thenReturn(e);
//        startService(VehicleInfoService.buildConnectIntent(getContext(), "0.0.0.0"));
//
//        r.run();
//        MessageCourier.DashboardMessage[] dm = MessageCourier.getDashboardMessages();
//        assertEquals("No error message for dashboard", 2, dm.length);
//        assertTrue("Non-error in dashboard messages", dm[1].isError());
//        if (!dm[1].getContents().contains("Fail.")) {
//            fail("'Fail.' does not appear in error message: " + dm[1].getContents());
//        }
//    }
}
