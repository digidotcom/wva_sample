/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.wvalib.test;

import com.digi.wva.async.Event;
import com.digi.wva.async.VehicleResponse;
import com.digi.wva.wvalib.test.auxiliary.JsonFactory;
import junit.framework.TestCase;

public class EventTest extends TestCase {
    Event subscriptionEventTest = null;
    Event alarmEventTest = null;
    JsonFactory jFactory = new JsonFactory();

	protected void setUp() throws Exception {
        subscriptionEventTest = Event.fromTCP(jFactory.data());
        alarmEventTest = Event.fromTCP(jFactory.alarm());
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testFromTCP() {
        Event e1 = Event.fromTCP(jFactory.data());
        Event e2 = Event.fromTCP(jFactory.alarm());
        Event e3 = Event.fromTCP(jFactory.junk());

        //Should return an object on correct input, null on bad input
        assertNotNull(e1);
        assertNotNull(e2);
        assertNull(e3);
	}

	public void testGetType() {
        String type1 = subscriptionEventTest.getType();
        assertEquals(type1, "subscription");

        String type2 = alarmEventTest.getType();
        assertEquals(type2, "alarm");
	}

	public void testGetResponse() {
        VehicleResponse resp1 = subscriptionEventTest.getResponse();
        assertEquals(resp1.value, 4.3);

        VehicleResponse resp2 = alarmEventTest.getResponse();
        assertEquals(resp2.value, 4.3);
	}

	public void testGetEndpoint() {
        String endpoint1 = subscriptionEventTest.getEndpoint();
        assertEquals(endpoint1, "baz");

        String endpoint2 = alarmEventTest.getEndpoint();
        assertEquals(endpoint2, "baz");
	}

}
