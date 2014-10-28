/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test.adapters;

import android.test.AndroidTestCase;
import com.digi.android.wva.adapters.VariableAdapter;
import com.digi.android.wva.model.VehicleData;
import com.digi.android.wva.util.VehicleDataList;
import org.joda.time.DateTime;

import java.util.ArrayList;

/**
 * Created by mwadsten on 5/24/13.
 */
public class VariableAdapterTest extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
        VehicleDataList.initInstance();
        VariableAdapter.initInstance(getContext(), VehicleDataList.getInstance());
        VariableAdapter.getInstance().clear();
    }

    private VariableAdapter get() {
        return VariableAdapter.getInstance();
    }

    public void testInitialized() {
        assertNotNull(VariableAdapter.getInstance());
    }

    public void testAddNew() {
        ArrayList<VehicleData> lis = new ArrayList<VehicleData>();
        for (int i = 0; i < 100; i++) {
            lis.add(new VehicleData("Var" + i, (double) i, DateTime.now()));
        }

        get().add(lis.get(10));
        assertEquals("Size after add is not 1", 1, get().getCount());
        assertSame("Item not the same", get().getItem(0), lis.get(10));
    }

    public void testAddUpdating() {
//        fail("This test has not been implemented yet.");
    }

    public void testRemoveEndpoint() {
        get().add(new VehicleData("Test", 0.0, DateTime.now()));

        assertEquals("Size after initial add is not 1", 1, get().getCount());

        get().removeEndpoint("Test");
        assertEquals("removeEndpoint did not remove the endpoint", 0, get().getCount());
    }

    public void testAddAll() {
        ArrayList<VehicleData> lis = new ArrayList<VehicleData>();
        for (int i = 0; i < 100; i++) {
            lis.add(new VehicleData("Var" + i, (double) i, DateTime.now()));
        }

        assertEquals("Adapter not initially empty", 0, get().getCount());

        get().addAll(lis);
        assertEquals("addAll did not add all...", lis.size(), get().getCount());
    }

    public void testAddAllUpdating() {
//        fail("This test has not been implemented yet.");
    }

    public void testClear() {
        VariableAdapter adapter = get();
        for (int i = 0; i < 100; i++) {
            adapter.add(new VehicleData("Test" + i, (double) i, null));
        }

        assertEquals("Filling adapter did not work completely?", 100, adapter.getCount());

        adapter.clear();
        assertEquals("clear() did not work", 0, adapter.getCount());
    }

    public void testGetCount() {
        VariableAdapter adapter = get();
        for (int i = 0; i < 100; i++) {
            adapter.add(new VehicleData("Test" + i, (double) i, null));
        }

        assertEquals("incorrect return value after initial fill up", 100, adapter.getCount());

        adapter.add(new VehicleData("Another", -1, null));
    }

    public void testGetItem() {
        VariableAdapter adapter = get();
        VehicleData data = new VehicleData("Test", 0, null);
        adapter.add(data);
        assertSame("getItem(0) returned wrong thing", data, adapter.getItem(0));
    }

    public void testGetPosition() {
        ArrayList<VehicleData> datas = new ArrayList<VehicleData>();
        for (int i = 0; i < 10; i++) {
            datas.add(new VehicleData("Test " + i, (double) i, null));
        }
        for (VehicleData c : datas) {
            get().add(c);
        }

        assertEquals("Test 0 in wrong position", 0, get().getPosition(datas.get(0)));
        assertEquals("Test 1 in wrong position", 1, get().getPosition(datas.get(1)));
        assertEquals("Test 2 in wrong position", 2, get().getPosition(datas.get(2)));
        assertEquals("Test 3 in wrong position", 3, get().getPosition(datas.get(3)));
        assertEquals("Test 4 in wrong position", 4, get().getPosition(datas.get(4)));
        assertEquals("Test 5 in wrong position", 5, get().getPosition(datas.get(5)));
        assertEquals("Test 6 in wrong position", 6, get().getPosition(datas.get(6)));
        assertEquals("Test 7 in wrong position", 7, get().getPosition(datas.get(7)));
        assertEquals("Test 8 in wrong position", 8, get().getPosition(datas.get(8)));
        assertEquals("Test 9 in wrong position", 9, get().getPosition(datas.get(9)));
    }

    public void testInsert() {
        VehicleData d1 = new VehicleData("Test1", 0, null);
        VehicleData d2 = new VehicleData("Test2", 1, null);
        get().insert(d1, 0);
        assertSame("insert(0) failed?", get().getItem(0), d1);
        get().insert(d2, 0);
        assertSame("insert(0) didn't work", get().getItem(0), d2);
        assertSame("insert(0) didn't push d1 over", get().getItem(1), d1);
    }

    public void testRemove() {
        VariableAdapter adapter = get();
        ArrayList<VehicleData> datas = new ArrayList<VehicleData>();
        for (int i = 0; i < 10; i++) {
            datas.add(new VehicleData("Test " + i, (double) i, null));
        }
        for (VehicleData c : datas) {
            adapter.add(c);
        }

        assertEquals("Initial size not 10", 10, adapter.getCount());

        adapter.remove(datas.get(2));
        assertEquals("Final size not 9", 9, adapter.getCount());
        adapter.remove(datas.get(2));
        assertEquals("Duplicate remove removed or added something?", 9, adapter.getCount());
    }
}
