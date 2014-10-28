/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test.model;

import android.test.InstrumentationTestCase;
import com.digi.android.wva.model.VehicleData;

/**
 * Created by mwadsten on 5/24/13.
 */
public class VehicleDataTest extends InstrumentationTestCase {
    public void testCreation() {
        VehicleData test = new VehicleData("Testing", 423.541, null);
        assertEquals("Name not stored correctly", "Testing", test.name);
        assertEquals("Value not stored correctly", 423.541, test.value, 0.01);
        assertNotNull("Timestamp is null", test.timestamp);

        VehicleData later = new VehicleData("Testing 2", 0, null);
        long testms = test.timestamp.getMillis();
        long laterms = later.timestamp.getMillis();
        if (testms > laterms) {
            fail("Newer data does not have newer timestamp: " + testms
                    + " (older) vs. " + laterms + " (newer)");
        }

        boolean raised = false;
        try {
            new VehicleData(null, 0, null);
        } catch (NullPointerException e) {
            // NPE is expected -- VehicleData needs to be given an endpoint name.
            raised = true;
        }

        if (!raised)
            fail("Expected VehicleData(null, ...) to raise NPE.");
    }

    public void testUpdating() {
        VehicleData test = new VehicleData("Original", -100.2, null);
        VehicleData next = new VehicleData("Original", 100.2, null);
        VehicleData wrong = new VehicleData("Brand New", 100.3, null);

        try {
            test.update(next);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        boolean raised = false;
        try {
            test.update(wrong);
        } catch (Exception e) {
            // We expect this to throw an exception, because the
            // endpoint names do not match up.
            raised = true;
        }

        if (!raised)
            fail("Expected update() to raise Exception because of mismatched names.");
    }
}
