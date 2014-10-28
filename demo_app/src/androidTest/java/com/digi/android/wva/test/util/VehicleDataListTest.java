/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test.util;

import android.test.AndroidTestCase;
import com.digi.android.wva.adapters.LogAdapter;
import com.digi.android.wva.model.VehicleData;
import com.digi.android.wva.util.VehicleDataList;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by mwadsten on 5/28/13.
 */
public class VehicleDataListTest extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
        VehicleDataList.initInstance();
        // Clean out any data.
        VehicleDataList.getInstance().getList().clear();

        LogAdapter.initInstance(getContext());
        LogAdapter.getInstance().clear();
    }

    public void testSetUpClears() {
        assertTrue("VehicleDataList non-empty", VehicleDataList.getInstance().getList().isEmpty());
        assertTrue("LogAdapter non-empty", LogAdapter.getInstance().isEmpty());
    }

    public void testSingle() {
        VehicleDataList lis = VehicleDataList.getInstance();
        LogAdapter logs = LogAdapter.getInstance();

        VehicleData data = new VehicleData("Test", 1.0, null);
        lis.update(data);

        assertEquals("VehicleDataList size wrong", 1, lis.getList().size());
        assertEquals("LogAdapter size wrong", 1, logs.getCount());
        assertSame("VehicleDataList has wrong data", lis.getList().get(0), data);
    }

    public void testManyDifferent() {
        VehicleDataList lis = VehicleDataList.getInstance();
        LogAdapter logs = LogAdapter.getInstance();

        String[] names = new String[]{"Apples", "Bananas", "Carrots", "Danger"};
        for (int i = 0; i < names.length; i++) {
            lis.update(new VehicleData(names[i], i, null));
        }

        assertEquals("VehicleDataList size is wrong", names.length, lis.getList().size());
        assertEquals("LogAdapter size is wrong", names.length, logs.getCount());
    }

    public void testUpdating() {
        VehicleDataList lis = VehicleDataList.getInstance();
        LogAdapter logs = LogAdapter.getInstance();
        Set<String> nameset = new HashSet<String>();

        String[] names = new String[]
                {"Apples", "Bananas", "Custard", "Daleks", "Elephants",
                 "Bananas", "Fish Fingers", "Gorn", "Apples", "Daleks",
                 "Hills", "Daleks", "Bananas", "Elephants", "Fish Fingers"};
        int count = names.length;
        for (int i = 0; i < count; i++) {
            nameset.add(names[i]);
        }

        for (int i = 0; i < count; i++) {
            lis.update(new VehicleData(names[i], i, null));
        }

        assertEquals("VehicleDataList size is wrong", nameset.size(), lis.getList().size());
        assertEquals("LogAdapter size is wrong", count, logs.getCount());
    }
}
