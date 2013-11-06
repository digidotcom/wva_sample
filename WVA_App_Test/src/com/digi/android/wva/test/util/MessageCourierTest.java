/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test.util;

import android.test.InstrumentationTestCase;
import com.digi.android.wva.model.VehicleData;
import com.digi.android.wva.util.MessageCourier;
import com.digi.android.wva.util.MessageCourier.ChartMessage;
import com.digi.android.wva.util.MessageCourier.DashboardMessage;
import org.joda.time.DateTime;

public class MessageCourierTest extends InstrumentationTestCase {
	@Override
	protected void setUp() throws Exception {
		MessageCourier.clear();
	}
	
	public void testConnected() {
		MessageCourier.sendDashConnected("Test");
		DashboardMessage[] msgs = MessageCourier.getDashboardMessages();
		assertEquals("Not exactly 1 message for dashboard", 1, msgs.length);
		assertEquals("First message is not what we expected", "Test", msgs[0].getContents());
		assertFalse("First message is error", msgs[0].isError());
	}
	
	public void testError() {
		// sendError puts the message on both lists

        // TODO: It also puts the error at the front of the queue. Test this.
		MessageCourier.sendError("Error!");
		DashboardMessage[] dmsgs = MessageCourier.getDashboardMessages();
		ChartMessage[] cmsgs = MessageCourier.getChartMessages();
		
		assertEquals("Not exactly 1 message for dashboard", 1, dmsgs.length);
		assertEquals("Not exactly 1 message for chart", 1, cmsgs.length);
		DashboardMessage dm = dmsgs[0];
		ChartMessage cm = cmsgs[0];
		assertEquals("First dash message is wrong", "Error!", dm.getContents());
		assertEquals("First chart message is wrong", "Error!", cm.getError());
		assertTrue("First dash message is not an error", dm.isError());
		assertNull("First chart message contains data, but should be an error", cm.getData());
	}
	
	public void testNewData() {
		VehicleData data = new VehicleData("Test", 10, DateTime.now());
		
		MessageCourier.sendChartNewData(data);
		
		ChartMessage[] msgs = MessageCourier.getChartMessages();
		
		assertEquals("Not exactly 1 message for chart", 1, msgs.length);
		
		ChartMessage msg = msgs[0];
		assertSame("Wrong first message", msg.getData(), data);
		assertNull("Message contains error", msg.getError());
	}
	
	public void testDashSize() {
		// Dashboard message list "max size" is 5
		for (int i = 0; i < 10; i++) {
			MessageCourier.sendDashConnected(Integer.toString(i));
		}
		DashboardMessage[] msgs = MessageCourier.getDashboardMessages();
		assertEquals("Not exactly 5 messages for chart", 5, msgs.length);
		// Check that the first five messages were dropped
		assertTrue("Dash list buffer works incorrectly", "5".equals(msgs[0].getContents()));
	}
	
	public void testChartSize() {
		// Chart message list "max size" is 20
		for (int i = 0; i < 40; i++) {
			MessageCourier.sendChartNewData(new VehicleData("", i, null));
		}
		ChartMessage[] msgs = MessageCourier.getChartMessages();
		assertEquals("Chart message list incorrect size", 20, msgs.length);
		// Check that the first twenty messages were dropped
		assertEquals("Wrong message at front of list", 20, (int)msgs[0].getData().value);
	}
	
	public void testClearEtc() {
		// setUp should clear these out...
		assertEquals("Chart messages not cleared", 0, MessageCourier.getChartMessages().length);
		assertEquals("Dash messages not cleared", 0, MessageCourier.getDashboardMessages().length);
		// should be empty just after being dumped out
		assertEquals("getChartMessages() doesn't clear", 0, MessageCourier.getChartMessages().length);
		assertEquals("getDashboardMessages() doesn't clear", 0, MessageCourier.getDashboardMessages().length);
		
		// Put something into them
		MessageCourier.sendError(null);
		assertEquals("sendError() didn't send to chart", 1, MessageCourier.getChartMessages().length);
		assertEquals("sendError() didn't send to dash", 1, MessageCourier.getDashboardMessages().length);
		// should be empty just after being dumped out
        assertEquals("getChartMessages() doesn't clear", 0, MessageCourier.getChartMessages().length);
        assertEquals("getDashboardMessages() doesn't clear", 0, MessageCourier.getDashboardMessages().length);

        // Put something into them
		MessageCourier.sendError(null);
		// Now clear them out
		MessageCourier.clear();
		assertEquals("clear() didn't clear chart messages", 0, MessageCourier.getChartMessages().length);
		assertEquals("clear() didn't clear dash messages", 0, MessageCourier.getDashboardMessages().length);
	}
}
