/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.wvalib.test;

import com.digi.wva.async.AlarmType;
import junit.framework.TestCase;

/**
 * Created by awickert on 5/28/13.
 */
public class AlarmTypeTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAlarmType() {
        AlarmType a = AlarmType.ABOVE;
        AlarmType b = AlarmType.BELOW;
        AlarmType c = AlarmType.CHANGE;
        AlarmType d = AlarmType.DELTA;

        assertNotNull(a);
        assertNotNull(b);
        assertNotNull(c);
        assertNotNull(d);
    }

    public void testFromString() {
        String a = "above";
        String b = "below";
        String c = "change";
        String d = "delta";
        String e = "error";

        AlarmType aa = AlarmType.fromString(a);
        AlarmType bb = AlarmType.fromString(b);
        AlarmType cc = AlarmType.fromString(c);
        AlarmType dd = AlarmType.fromString(d);
        AlarmType ee = AlarmType.fromString(e);

        assertEquals(AlarmType.ABOVE, aa);
        assertEquals(AlarmType.BELOW, bb);
        assertEquals(AlarmType.CHANGE, cc);
        assertEquals(AlarmType.DELTA, dd);
        assertNull(ee);
    }

    public void testToString() {
        AlarmType a = AlarmType.ABOVE;
        AlarmType b = AlarmType.BELOW;
        AlarmType c = AlarmType.CHANGE;
        AlarmType d = AlarmType.DELTA;
        AlarmType e = null;

        String aa = AlarmType.makeString(a);
        String bb = AlarmType.makeString(b);
        String cc = AlarmType.makeString(c);
        String dd = AlarmType.makeString(d);
        String ee = AlarmType.makeString(e);

        assertEquals("above", aa);
        assertEquals("below", bb);
        assertEquals("change", cc);
        assertEquals("delta", dd);
        assertEquals("", ee);
    }
}
