/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test.model;

import android.test.InstrumentationTestCase;
import com.digi.android.wva.model.LogEvent;

/**
 * Created by mwadsten on 5/29/13.
 */
public class LogEventTest extends InstrumentationTestCase {
    public void testCreation() {
        LogEvent e = new LogEvent("Test", "Timestamp");
        assertEquals("Wrong message", "Test", e.message);
        assertEquals("Wrong timestamp", "Timestamp", e.timestamp);
        assertFalse("Non-alarm event is alarm", e.isAlarm);

        LogEvent e2 = new LogEvent("Test2", "Timestamp2", true);
        assertEquals("Wrong message", "Test2", e2.message);
        assertEquals("Wrong timestamp", "Timestamp2", e2.timestamp);
        assertTrue("Alarm event is not alarm", e2.isAlarm);
    }
}
