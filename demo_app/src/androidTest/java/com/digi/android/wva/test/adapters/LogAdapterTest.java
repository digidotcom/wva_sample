/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test.adapters;

import android.test.AndroidTestCase;
import com.digi.android.wva.adapters.LogAdapter;
import com.digi.android.wva.model.LogEvent;
import com.digi.android.wva.model.VehicleData;

/**
 * Created by mwadsten on 5/29/13.
 */
public class LogAdapterTest extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        LogAdapter.initInstance(getContext());
        LogAdapter.getInstance().clear();
    }

    public void testEmpty() {
        assertTrue("Adapter not cleared", LogAdapter.getInstance().isEmpty());
    }

    public void testAddEvent() {
        LogEvent event = new LogEvent("Test Message", "Test Timestamp");

        LogAdapter adapter = LogAdapter.getInstance();

        adapter.add(event);

        assertEquals("Wrong event in adapter", event, adapter.getItem(0));
        assertEquals("Incorrect event count in adapter", 1, adapter.getCount());
        assertFalse("Log event is alarm", adapter.getItem(0).isAlarm);
    }

    public void testAlarmTriggered() {
        VehicleData alarmdata = new VehicleData("Test", 0, null);

        LogAdapter adapter = LogAdapter.getInstance();

        adapter.alarmTriggered(alarmdata);

        assertEquals("Wrong number of events in adapter", 1, adapter.getCount());
        assertEquals("Wrong event message for alarm", adapter.getItem(0).message, "Alarm: " + alarmdata.name + " = " + alarmdata.value);
        assertEquals("Wrong event timestamp", adapter.getItem(0).timestamp, alarmdata.timestamp.toString());
        assertTrue("Log event is not alarm", adapter.getItem(0).isAlarm);
    }
}
