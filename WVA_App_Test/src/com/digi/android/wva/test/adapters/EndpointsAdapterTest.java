/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test.adapters;

import android.test.AndroidTestCase;
import com.digi.android.wva.adapters.EndpointsAdapter;
import com.digi.android.wva.model.EndpointConfiguration;

import java.util.ArrayList;

/**
 * Created by mwadsten on 5/24/13.
 */
public class EndpointsAdapterTest extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
        EndpointsAdapter.initInstance(getContext());
        EndpointsAdapter.getInstance().clear();
    }

    private EndpointsAdapter get() {
        return EndpointsAdapter.getInstance();
    }

    public void testInitialized() {
        assertNotNull("EndpointsAdapter not initialized?", get());
    }

    public void testFindEndpointConfiguration() {
        ArrayList<EndpointConfiguration> confs = new ArrayList<EndpointConfiguration>();
        for (int i = 0; i < 10; i++) {
            confs.add(new EndpointConfiguration("Test " + i));
        }
        for (EndpointConfiguration c : confs) {
            get().add(c);
        }

        assertSame("Wrong Test 0 conf", confs.get(0), get().findEndpointConfiguration("Test 0"));
        assertSame("Wrong Test 1 conf", confs.get(1), get().findEndpointConfiguration("Test 1"));
        assertSame("Wrong Test 2 conf", confs.get(2), get().findEndpointConfiguration("Test 2"));
        assertSame("Wrong Test 3 conf", confs.get(3), get().findEndpointConfiguration("Test 3"));
        assertSame("Wrong Test 4 conf", confs.get(4), get().findEndpointConfiguration("Test 4"));
        assertSame("Wrong Test 5 conf", confs.get(5), get().findEndpointConfiguration("Test 5"));
        assertSame("Wrong Test 6 conf", confs.get(6), get().findEndpointConfiguration("Test 6"));
        assertSame("Wrong Test 7 conf", confs.get(7), get().findEndpointConfiguration("Test 7"));
        assertSame("Wrong Test 8 conf", confs.get(8), get().findEndpointConfiguration("Test 8"));
        assertSame("Wrong Test 9 conf", confs.get(9), get().findEndpointConfiguration("Test 9"));
        assertNull("Bad lookup got non-null answer", get().findEndpointConfiguration("I am not here"));
    }

    public void testAdd() {
        EndpointConfiguration newconf = new EndpointConfiguration("Test");
        get().add(newconf);
        assertEquals("getCount() after add is wrong...", 1, get().getCount());
        assertEquals("Found wrong conf endpoint", "Test", get().getItem(0).getEndpoint());
        assertSame("Found wrong endpoint object", newconf, get().findEndpointConfiguration("Test"));
    }

    public void testClear() {
        for (int i = 0; i < 10; i++)
            get().add(new EndpointConfiguration(null));
        get().clear();
        assertEquals("clear() did not empty out configurations", 0, get().getCount());
    }

    public void testGetCount() {
        for (int i = 0; i < 100; i++)
            get().add(new EndpointConfiguration(null));
        assertEquals("getCount() didn't return 100", 100, get().getCount());

        get().clear();
        for (int i = 0; i < 77; i++)
            get().add(new EndpointConfiguration(null));
        assertEquals("getCount() didn't return 77", 77, get().getCount());
    }

    public void testGetItem() {
        ArrayList<EndpointConfiguration> confs = new ArrayList<EndpointConfiguration>();
        for (int i = 0; i < 10; i++) {
            confs.add(new EndpointConfiguration("Test " + i));
        }
        for (EndpointConfiguration c : confs) {
            get().add(c);
        }

        assertSame("Wrong Test 0 conf", confs.get(0), get().getItem(0));
        assertSame("Wrong Test 1 conf", confs.get(1), get().getItem(1));
        assertSame("Wrong Test 2 conf", confs.get(2), get().getItem(2));
        assertSame("Wrong Test 3 conf", confs.get(3), get().getItem(3));
        assertSame("Wrong Test 4 conf", confs.get(4), get().getItem(4));
        assertSame("Wrong Test 5 conf", confs.get(5), get().getItem(5));
        assertSame("Wrong Test 6 conf", confs.get(6), get().getItem(6));
        assertSame("Wrong Test 7 conf", confs.get(7), get().getItem(7));
        assertSame("Wrong Test 8 conf", confs.get(8), get().getItem(8));
        assertSame("Wrong Test 9 conf", confs.get(9), get().getItem(9));
    }

    public void testGetPosition() {
        ArrayList<EndpointConfiguration> confs = new ArrayList<EndpointConfiguration>();
        for (int i = 0; i < 10; i++) {
            confs.add(new EndpointConfiguration("Test " + i));
        }
        for (EndpointConfiguration c : confs) {
            get().add(c);
        }

        assertEquals("Test 0 in wrong position", 0, get().getPosition(confs.get(0)));
        assertEquals("Test 1 in wrong position", 1, get().getPosition(confs.get(1)));
        assertEquals("Test 2 in wrong position", 2, get().getPosition(confs.get(2)));
        assertEquals("Test 3 in wrong position", 3, get().getPosition(confs.get(3)));
        assertEquals("Test 4 in wrong position", 4, get().getPosition(confs.get(4)));
        assertEquals("Test 5 in wrong position", 5, get().getPosition(confs.get(5)));
        assertEquals("Test 6 in wrong position", 6, get().getPosition(confs.get(6)));
        assertEquals("Test 7 in wrong position", 7, get().getPosition(confs.get(7)));
        assertEquals("Test 8 in wrong position", 8, get().getPosition(confs.get(8)));
        assertEquals("Test 9 in wrong position", 9, get().getPosition(confs.get(9)));
    }

    public void testInsert() {
        EndpointConfiguration newconf = new EndpointConfiguration(null);
        get().insert(newconf, 0);
        EndpointConfiguration newconf2 = new EndpointConfiguration("Test");
        get().insert(newconf2, 0);
        assertSame("insert(0) didn't work?", get().getItem(0), newconf2);
        assertSame("insert(0) didn't push original item 0?", get().getItem(1), newconf);
    }

    public void testRemove() {
        ArrayList<EndpointConfiguration> confs = new ArrayList<EndpointConfiguration>();
        for (int i = 0; i < 10; i++) {
            confs.add(new EndpointConfiguration("Test " + i));
        }
        for (EndpointConfiguration c : confs) {
            get().add(c);
        }

        get().remove(confs.get(3));
        assertNull("Removed item still in adapter", get().findEndpointConfiguration(confs.get(3).getEndpoint()));
        assertSame("Removed wrong index", get().getItem(2), confs.get(2));
        assertSame("Removed wrong index", get().getItem(3), confs.get(4));
    }
}
