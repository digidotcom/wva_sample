/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.util;

import com.digi.android.wva.model.VehicleData;

import java.util.LinkedList;

/**
 * Class containing static LinkedLists used as queues for sending
 * messages and data from various sources (like {@link com.digi.android.wva.VehicleInfoService}
 * and {@link com.digi.android.wva.WvaApplication}) to the
 * {@link com.digi.android.wva.DashboardActivity} and {@link com.digi.android.wva.fragments.ChartFragment}
 * for use.
 *
 * Created by mwadsten on 5/22/13.
 */
public class MessageCourier {
    /**
     * Wrapper around data (pertaining to device connection status) to be
     * processed by the DashboardActivity.
     */
    public static class DashboardMessage {
        final String data;
        final boolean isError, isReconnecting;

        private DashboardMessage(String data, boolean isError, boolean isReconnect) {
            this.data = data;
            this.isError = isError;
            this.isReconnecting = isReconnect;
        }

        /**
         * Get the string held in this message
         * @return the string data held by this message
         */
        public String getContents() {
            return data;
        }

        /**
         * Get the boolean held in this message (indicating if this message
         * signifies an error or not)
         * @return true if this message indicates an error
         */
        public boolean isError() {
            return isError;
        }

        public boolean isReconnecting() {
            return isReconnecting;
        }

        /**
         * Create a new {@link DashboardMessage} to report that connecting
         * to the device at <b>ip</b> was successful.
         * @param ip IP address we are connected to
         * @return new {@link DashboardMessage} containing the IP address
         */
        public static DashboardMessage connected(String ip) {
            return new DashboardMessage(ip, false, false);
        }

        /**
         * Create a new {@link DashboardMessage} to report that some error
         * has occurred.
         * @param error a String message to indicate the error
         * @return new {@link DashboardMessage} containing the error message
         */
        public static DashboardMessage error(String error) {
            return new DashboardMessage(error, true, false);
        }

        public static DashboardMessage reconnecting(String ip) {
            return new DashboardMessage(ip, false, true);
        }
    }

    /**
     * Wrapper around data to be processed by the ChartFragment.
     */
    public static class ChartMessage {
        String error;
        VehicleData data;
        boolean isReconnecting;

        /**
         * Get the error string passed in when creating this message, if any
         * @return error string contained within, or null if none
         */
        public String getError() {
            return error;
        }

        /**
         * Get the {@link VehicleData} object passed in when creating this
         * message, if any
         * @return VehicleData contained within, or null if none
         */
        public VehicleData getData() {
            return data;
        }

        public boolean isReconnecting() {
            return isReconnecting;
        }

        /**
         * Create a new {@link ChartMessage} to indicate that an error
         * has occurred within the application
         * @param error error string to pass along
         * @return new {@link ChartMessage} containing the error
         */
        public static ChartMessage error(String error) {
            ChartMessage rv = new ChartMessage();
            rv.error = error;
            rv.isReconnecting = false;
            return rv;
        }

        /**
         * Create a new {@link ChartMessage} to indicate that new vehicle
         * data has arrived and should be plotted on the graph
         * @param data {@link VehicleData} to plot
         * @return new {@link ChartMessage} containing the data
         */
        public static ChartMessage newData(VehicleData data) {
            ChartMessage rv = new ChartMessage();
            rv.data = data;
            rv.isReconnecting = false;
            return rv;
        }

        public static ChartMessage reconnecting() {
            ChartMessage rv = new ChartMessage();
            rv.isReconnecting = true;
            return rv;
        }
    }
    
    private static final LinkedList<DashboardMessage> toDash = new LinkedList<DashboardMessage>();
    private static final LinkedList<ChartMessage> toChart = new LinkedList<ChartMessage>();
    private static final int DASH_MAX = 5;
    private static final int CHART_MAX = 20;

    private static final Object dashlock = new Object(),
                                   chartlock = new Object();

    /**
     * No need for a constructor for MessageCourier.
     */
    private MessageCourier() {}

    /**
     * Queue up a new message for the dashboard to inform it that
     * we have successfully connected to a device.
     * @param ipaddress IP address we are connected to
     */
    public static void sendDashConnected(String ipaddress) {
        putDashMessage(DashboardMessage.connected(ipaddress));
    }

    /**
     * Queue up a new message for both the dashboard and the chart, notifying
     * them that there has been a connection error with the device.
     *
     * <p>The error message gets placed at the front of the message "queues";
     * this way, the error will be processed before other messages, such as
     * "connected"/"reconnecting" messages.</p>
     * @param error error message to display
     */
    public static void sendError(String error) {
        putDashMessage(DashboardMessage.error(error), true);
        putChartMessage(ChartMessage.error(error), true);
    }

    public static void sendReconnecting(String ipaddress) {
        putDashMessage(DashboardMessage.reconnecting(ipaddress));
        putChartMessage(ChartMessage.reconnecting());
    }

    /**
     * Queue up a new message for the chart, giving it new vehicle data to plot.
     * @param data VehicleData object to plot on screen
     */
    public static void sendChartNewData(VehicleData data) {
        putChartMessage(ChartMessage.newData(data));
    }

    private static void putDashMessage(DashboardMessage msg) {
        putDashMessage(msg, false);
    }

    private static void putDashMessage(DashboardMessage msg, boolean putAtFront) {
        synchronized (dashlock) {
            if (putAtFront)
                toDash.addFirst(msg);
            else
                toDash.add(msg);
            // Keep dashboard message queue to an appropriate size.
            while (toDash.size() > DASH_MAX) {
                toDash.remove();
            }
        }
    }

    private static void putChartMessage(ChartMessage msg) {
        putChartMessage(msg, false);
    }

    private static void putChartMessage(ChartMessage msg, boolean putAtFront) {
        synchronized (chartlock) {
            if (putAtFront)
                toChart.addFirst(msg);
            else
                toChart.add(msg);
            // Keep chart message queue to an appropriate size.
            while (toChart.size() > CHART_MAX) {
                toChart.remove();
            }
        }
    }

    /**
     * Retrieve all DashboardMessages currently queued up, as an array.
     * @return all currently enqueued DashboardMessages, or an empty array if
     * there are none
     */
    public static DashboardMessage[] getDashboardMessages() {
        synchronized (dashlock) {
            final int count = toDash.size();
            if (count <= 0)
                return new DashboardMessage[0];
            DashboardMessage[] msgs = new DashboardMessage[count];
            toDash.toArray(msgs);
            toDash.clear();
            return msgs;
        }
    }

    /**
     * Retrieve all ChartMessages currently queued up, as an array.
     * @return all currently enqueued ChartMessages, or an empty array if
     * there are none
     */
    public static ChartMessage[] getChartMessages() {
        synchronized (chartlock) {
            final int count = toChart.size();
            if (count <= 0)
                return new ChartMessage[0];
            ChartMessage[] msgs = new ChartMessage[count];
            toChart.toArray(msgs);
            toChart.clear();
            return msgs;
        }
    }

    /**
     * Clear out any pending {@link ChartMessage}s and {@link DashboardMessage}s.
     */
    public static void clear() {
        synchronized (chartlock) {
            toChart.clear();
        }
        synchronized (dashlock) {
            toDash.clear();
        }
    }
}
