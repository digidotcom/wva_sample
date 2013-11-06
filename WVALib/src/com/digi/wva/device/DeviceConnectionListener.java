/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.device;

import android.util.Log;

import java.io.IOException;

/**
 * Listener for certain events that happen within the {@link TCPReceiver}
 * class, such as successfully connecting, stopping because of an error, and
 * the remote end of the socket closing.
 *
 * <p>This class is in the com.digi.wva.device package, rather than
 * the com.digi.wva.async package, because there are certain methods which we
 * want to make package-default; this would be impossible if this class were not
 * in the com.digi.wva.device package.</p>
 *
 * Created by mwadsten on 6/5/13.
 */
public abstract class DeviceConnectionListener {
    /**
     * Get a DeviceConnectionListener implementation which just
     * calls through to the super's methods in each callback.
     *
     * <p>This is the default DeviceConnectionListener used by
     * {@link TCPReceiver}.</p>
     * @return a new {@link DeviceConnectionListener} which uses only
     * the default class implementations for callbacks
     */
    static final DeviceConnectionListener getDefault() {
        return new DeviceConnectionListener() {};
    }

    /**
     * Sleep the current thread, disconnect the data stream and reconnect it.
     * @param device the Device to be manipulated
     * @param millis length of time to sleep the thread, in milliseconds
     * @param port port to connect to upon reconnect
     */
    public final void reconnectAfter(Device device, long millis, int port) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // An interruption will presumably be caused by us wanting to
            // stop this thread... so let's abort the whole reconnection
            // process.
            Log.e("DeviceConnectionListener",
                    "Sleep before disconnect-reconnect was interrupted!", e);
            return;
        }

        device.disconnectDataStream();
        device.connectDataStream(port, this);
    }

    /**
     * Callback triggered when the {@link TCPReceiver}'s
     * socket has been successfully created (or, if the socket already existed,
     * when the thread has started up.
     *
     * <p>The default implementation of this method is to do nothing --
     * override this method to add, for example, interactivity with the UI.</p>
     *
     * @param device the {@link Device} associated with the TCPReceiver which
     *               triggered this call
     */
    public void onConnected(Device device) {
        // Default implementation does nothing. Can be overridden to
        // notify a UI, for example.
    }

    /**
     * Callback triggered when the {@link TCPReceiver} encounters an exception
     * (IOException, specifically) and stops its thread.
     *
     * @param device the {@link Device} associated with the TCPReceiver which
     *               triggered this call
     * @param error the exception causing the receiver to stop
     */
    public void onError(Device device, IOException error) {
        // Default implementation does nothing. Can be overridden to
        // notify a UI, for example.
    }

    /**
     * Callback triggered when the TCPReceiver detects that the WVA has closed
     * its Event Channel socket (generally because the web services process
     * on the device has restarted, or because the device is shutting down)
     *
     * <p>The default implementation calls
     * {@link #reconnectAfter(com.digi.wva.device.Device, long, int)}
     * with 15000 as the second parameter (i.e. "reconnect after 15 seconds").</p>
     *
     * @param device the {@link Device} associated with the TCPReceiver which
     *               triggered this call
     * @param port the port which we were connected to
     */
    public void onRemoteClose(Device device, int port) {
        reconnectAfter(device, 15000, port);
    }

    /**
     * Callback is triggered when the TCPReceiver was unable to make an initial
     * connection to the device. This often happens when first attempting to
     * configure a port to connect to.
     *
     * @param device the {@link Device} associated with the TCPReceiver which
     *               triggered this call
     * @param port the port on which we attempted to connect
     */
    public void onFailedConnection(Device device, int port) {
        // abstract
    }
}
