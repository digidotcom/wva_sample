/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.wvalib.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		//$JUnit-BEGIN$
        suite.addTestSuite(AlarmTypeTest.class);
		suite.addTestSuite(DeviceTest.class);
		suite.addTestSuite(EcuTest.class);
		suite.addTestSuite(EventTest.class);
		suite.addTestSuite(HardwareTest.class);
		suite.addTestSuite(MessageHandlerTest.class);
		suite.addTestSuite(TCPReceiverTest.class);
		suite.addTestSuite(VehicleTest.class);
		suite.addTestSuite(WvaHttpClientTest.class);
		//$JUnit-END$
		return suite;
	}

}
