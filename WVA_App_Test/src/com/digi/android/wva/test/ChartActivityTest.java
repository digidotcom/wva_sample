/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test;

import android.os.Build;
import android.test.ActivityInstrumentationTestCase2;
import com.digi.android.wva.ChartActivity;
import com.digi.android.wva.WvaApplication;
import com.digi.android.wva.fragments.ChartFragment;
import com.digi.android.wva.model.VehicleData;
import org.joda.time.DateTime;
import com.digi.wva.device.Device;
import com.digi.wva.async.WvaCallback;
import com.digi.wva.async.WvaListener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.any;

/**
 * This might be more accurately described as ChartFragmentTest. However, since
 * there is no built-in Android fragment unit testing, using a
 * ChartActivityTest class to wrap testing for the ChartFragment will have
 * to suffice.
 *
 * <p>
 * I suppose it would be possible to make an AndroidTestCase
 * which creates a ChartFragment and an Activity, attaches the fragment to that
 * activity, and does its testing from there...</p>
 * Created by mwadsten on 5/28/13.
 */
public class ChartActivityTest extends ActivityInstrumentationTestCase2<ChartActivity> {

    public static final String ENGINE_SPEED = "EngineSpeed";
    public static final String VEHICLE_SPEED = "VehicleSpeed";

    public ChartActivityTest() {
        this(ChartActivity.class);
    }

    public ChartActivityTest(Class<ChartActivity> activityClass) {
        super(activityClass);
    }

    private ChartFragment getChartFragment() {
        if (getActivity() == null)
            return null;
        return getActivity().getChartFragment();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);

        getChartFragment().setIsTesting(true);

        getChartFragment().clearDataset();

        Device dev = mock(Device.class);
        doNothing().when(dev).subscribe(anyString(), anyInt(), any(WvaListener.class), any(WvaCallback.class));
        ((WvaApplication)getActivity().getApplication()).setDevice(dev);
    }

    @Override
    protected void tearDown() throws Exception {
        // super doesn't destroy (i.e. finish) activity at end of any tests
        // (and by extension, doesn't end the activity at the end of all
        // these tests)
        getActivity().finish();

        super.tearDown();
    }

    public void testPreconditions() {
        assertNotNull("Activity is null", getActivity());
        assertNotNull("Chart fragment is null", getChartFragment());
        // isDestroyed() was introduced in API 17
        if (Build.VERSION.SDK_INT >= 17)
            assertFalse("Activity destroyed!", getActivity().isDestroyed());
    }

    public void testGraphBuilt() {
        ChartFragment frag = getChartFragment();
        assertNotNull("Speed series is null", frag.getSpeedSeries());
        assertNotNull("RPM series is null", frag.getRpmSeries());

        assertEquals("Speed series has data", 0, frag.getSpeedSeries().getItemCount());
        assertEquals("RPM series has data", 0, frag.getRpmSeries().getItemCount());
        assertNull("Last speed is not null", frag.getLastSpeed());
        assertNull("Last RPM is not null", frag.getLastRPM());
    }

    public void testHandleNewData() {
        VehicleData newSpeed = new VehicleData(VEHICLE_SPEED, 100, null);
        VehicleData newRpm = new VehicleData(ENGINE_SPEED, 200, null);

        ChartFragment frag = getChartFragment();

        assertEquals("Speed series has data", 0, frag.getSpeedSeries().getItemCount());
        assertEquals("RPM series has data", 0, frag.getRpmSeries().getItemCount());

        frag.handleNewData(newRpm);
        assertEquals("RPM series doesn't have new data", 1, frag.getRpmSeries().getItemCount());
        assertEquals("Speed series got new data it shouldn't have", 0, frag.getSpeedSeries().getItemCount());

        frag.handleNewData(newSpeed);
        assertEquals("Speed series doesn't have new data", 1, frag.getSpeedSeries().getItemCount());
        assertEquals("RPM series got new data it shouldn't have", 1, frag.getRpmSeries().getItemCount());

        assertSame("Wrong last speed", frag.getLastSpeed(), newSpeed);
        assertSame("Wrong last RPM", frag.getLastRPM(), newRpm);
    }

    public void testStressTest() {
        ChartFragment frag = getChartFragment();

        assertEquals("Speed series has data", 0, frag.getSpeedSeries().getItemCount());
        assertEquals("RPM series has data", 0, frag.getRpmSeries().getItemCount());

        int count = 10000; // ten seconds worth, on the screen
        long time = (long) frag.getStartTime();
        for (int i = 0; i < count; i++) {
            frag.handleNewData(new VehicleData(ENGINE_SPEED, i+count, new DateTime(time + i)));
            frag.handleNewData(new VehicleData(VEHICLE_SPEED, i, new DateTime(time + i)));
        }

        assertEquals("Speed series has wrong count", count, frag.getSpeedSeries().getItemCount());
        assertEquals("RPM series has wrong count", count, frag.getRpmSeries().getItemCount());

        assertEquals("Wrong last rpm", 2*count - 1, frag.getLastRPM().value, 0.1);
        assertEquals("Wrong last speed", count - 1, frag.getLastSpeed().value, 0.1);
    }

    public void testClearDataset() {
        ChartFragment frag = getChartFragment();

        frag.handleNewData(new VehicleData(ENGINE_SPEED, 0, null));
        frag.handleNewData(new VehicleData(VEHICLE_SPEED, 0, null));

        assertEquals("RPM series wrong count", 1, frag.getRpmSeries().getItemCount());
        assertEquals("Speed series wrong count", 1, frag.getSpeedSeries().getItemCount());

        frag.clearDataset();

        assertEquals("RPM series not cleared", 0, frag.getRpmSeries().getItemCount());
        assertEquals("Speed series not cleared", 0, frag.getSpeedSeries().getItemCount());
    }

    public void testTimespanCorrect() {
        ChartFragment frag = getChartFragment();

        double start = frag.getStartTime();
        double end = frag.getEndTime();
        double interval = 15 * 60 * 1000;
        assertEquals("End time is off", end, start + interval, 0.01);
    }

//    public void testTimeShift() {
//
//    }
}
