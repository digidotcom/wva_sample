/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.wvalib.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.BlockingQueue;

import com.digi.wva.wvalib.test.auxiliary.JsonFactory;
import junit.framework.TestCase;

import org.json.JSONObject;

import com.digi.wva.device.Vehicle;
import com.digi.wva.device.MessageHandler;
import com.digi.wva.device.TCPReceiver;

public class MessageHandlerTest extends TestCase {
	MessageHandler handler;
	Vehicle mockVehicle;
	TCPReceiver mockReceiver;
	BlockingQueue<JSONObject> mockingQueue;
	JsonFactory jsonFactory;
	
	@SuppressWarnings("unchecked")
	protected void setUp() throws Exception {
		super.setUp();
		jsonFactory = new JsonFactory();
		mockingQueue = (BlockingQueue<JSONObject>) mock(BlockingQueue.class);
		when(mockingQueue.take()).thenReturn(jsonFactory.data(), jsonFactory.data());
		
		mockReceiver = mock(TCPReceiver.class);
		when(mockReceiver.getIncoming()).thenReturn(mockingQueue);
		
		mockVehicle = mock(Vehicle.class);
	}
	
	public void testStartStop() throws Exception{
		handler = new MessageHandler(mockReceiver, mockVehicle);
		handler.start();
		handler.stopThread();
		handler.join();
		assertTrue(!handler.isAlive());
	}
	
	public void testIO() throws Exception {
		//Unimplemented, see VehicleTest and TCPReceiverTest
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
