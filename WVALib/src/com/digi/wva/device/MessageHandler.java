/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.device;

import org.json.JSONObject;

import android.util.Log;

import com.digi.wva.async.Event;
import com.digi.wva.async.VehicleResponse;

public class MessageHandler extends Thread {

	private boolean running;
	private final TCPReceiver rec;
	private final Vehicle vehicle;

	/**
	 * The MessageHandler takes JSONObjects from the TCPReceiver queue,
	 * parses them into Events, and then hands them off to the dispatcher.
	 * 
	 * All Events passing through this runnable should be well-formed
	 * @param rec
     * @param vehicle
	 */
	public MessageHandler(TCPReceiver rec, Vehicle vehicle) {
		this.vehicle = vehicle;
		this.rec = rec;
		running = true;
	}


    /**
     * Interrupts the thread and exits the run method
     */
	public void stopThread() {
		this.running = false;
		this.interrupt();
	}

    /**
     * This object will continually attempt to take JSONObjects from the
     * TCPReceiver, parse them into events, and call Vehicle.notifyListeners.
     * If an object received from TCPReceiver is invalid or malformed, it will
     * be disregarded.
     */
	public void run() {
		while(running) {
			JSONObject obj;
			try {
				
				obj = rec.getIncoming().take(); //blocks until available
//				Log.i("MessageHandler", "Got message: " + obj.toString());
				Event e = Event.fromTCP(obj);
				if (e != null) { // if obj contained a valid message
					vehicle.updateCached(e);
				}
				else {
					Log.i("MessageHandler", "Message wasn't parsed...");
				}
				
			} catch (InterruptedException e) {
				running = false;
			} 
			if (Thread.interrupted()) {
				running = false;
			}
		}

	}
}
