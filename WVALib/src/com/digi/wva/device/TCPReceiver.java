/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.device;

import android.util.Log;
import com.digi.wva.exc.DisconnectedException;
import com.digi.wva.exc.FailedConnectionException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TCPReceiver extends Thread {
    public static final int MAX_LENGTH = 500;
	private static final String TAG = "com.dig.wva.net.TCPReceiver";
	private boolean running;
	private IOException ioe;
    private String hostname;
    private int port;
    private DeviceConnectionListener listener;

    /***
	 * Queue for incoming parsed messages. Will block
	 * when full
	 */
	private final BlockingQueue<JSONObject> incoming = new ArrayBlockingQueue<JSONObject>(100);

	/**
	 * TCP socket for receiving messages from the WVA web service.
	 * Does not write.
	 */
	private Socket clientSock;

    private Device device;

    public TCPReceiver(Device owner, Socket socket) {
        this.device = owner;
        this.port = socket.getPort();
        this.running = false;
        clientSock = socket;
        if (listener == null) {
            listener = DeviceConnectionListener.getDefault();
        }
    }

    public TCPReceiver(Device owner, String hostname, int port) {
        this(owner, hostname, port, null);
    }

    public TCPReceiver(Device owner, String hostname, int port, DeviceConnectionListener listener) {
        this.device = owner;
        this.hostname = hostname;
        this.port = port;
        this.running = false;
        if (listener == null) {
            listener = DeviceConnectionListener.getDefault();
        }
        this.listener = listener;
    }

	/**
	 * Allow the MessageHandler access to the object queue
	 * @return the received object queue
	 */
	public BlockingQueue<JSONObject> getIncoming() {
		return incoming;
	}

	/**
	 * Calling this method will permanently stop the run() method of this
	 * thread.
	 */
	void stopThread() {
		this.running = false;
		this.interrupt();
	}

    public void stopThread(IOException e) {
        this.ioe = e;
        if (this.clientSock != null) {
            try {
                this.clientSock.close();
            } catch (IOException e1) { }
        }

        e.printStackTrace();

        if (e instanceof DisconnectedException) {
            listener.onRemoteClose(device, port);
        } else if (e instanceof FailedConnectionException) {
            listener.onFailedConnection(device, port);
        } else {
            listener.onError(device, e);
        }
        stopThread();
    }
	
	public boolean isStopped() {
		return !running;
	}

    /**
     * @return The most recent exception encountered by this thread
     */
	public IOException ioException() {
		return ioe;
	}

	/**
	 * Read JSONObjects from a TCP stream into the incoming queue until either
     * the thread is interrupted or stopThread() is called. No validation is
     * performed on the objects, but they will be discarded if they are longer
     * than MAX_LENGTH characters.
     *
	 */
	public void run() {
        if (clientSock == null) {
            try {
                clientSock = makeSocket();
            } catch (IOException e) {
                stopThread(new FailedConnectionException("Failed to connect to TCP socket on port " + port, e));
                return;
            }
        }

        listener.onConnected(device);

		this.running = true;
		// Set up the input stream. If this fails, all hope is lost.
		BufferedReader in = null;

		try {
			in = new BufferedReader(new InputStreamReader(clientSock.getInputStream(), "UTF-8"));
		} catch (IOException e1) {
            stopThread(e1);
            return;
		}


		StringBuilder builder = new StringBuilder();
		JSONObject j;
		
		while (running) {

            if (this.isInterrupted()) {
                break;
            }

			try {
                // Read a line out of the incoming buffer. This blocks
                // indefinitely
                String next = in.readLine();

                // If this string is null, we have reached an EOF or the connection
                // has otherwise been severed at the remote end.
                if (next == null) {
                    Log.i(TAG, "Socket closed on remote end");
                    stopThread(new DisconnectedException("Socket closed on remote end"));
                    continue;
                }

                // Append this string to a stringBuilder. If it creates a valid
                // JSONObject, add it to the queue, otherwise, continue building
                builder.append(next);
                int i = builder.indexOf("{");
                builder.delete(0, (i < 0) ? 0 : i);
				j = new JSONObject(builder.toString());
				incoming.put(j);
				builder = new StringBuilder();

			} catch(JSONException je) {
                // JSONException is caught whenever the builder does not have a
                // valid JSON string.

				if (builder.length() > MAX_LENGTH) {
					//Message shouldn't be this long; abort!
					// Should only happen upon receiving a rogue '{'
					builder = new StringBuilder();
				}

			} catch (IOException e) {
                Log.i(TAG, "IOException in TCPReceiver");
                stopThread(e);

			} catch (Exception e) {
				Log.e(TAG, "Unknown Exception in TCPReceiver", e);
			}

		}

        // Close scanner (which closes all underlying structures)
        try {
            in.close();
            clientSock.close();
        } catch (IOException e) { }
        Log.d(TAG, "End of run()");
	}

    private Socket makeSocket() throws IOException {
        return new Socket(hostname, port);
    }

}
