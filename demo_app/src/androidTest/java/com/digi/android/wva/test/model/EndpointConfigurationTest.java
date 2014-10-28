/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test.model;

import android.os.Parcel;
import android.test.InstrumentationTestCase;
import com.digi.android.wva.model.EndpointConfiguration;
import com.digi.wva.async.AlarmType;

/**
 * Created by mwadsten on 5/24/13.
 */
public class EndpointConfigurationTest extends InstrumentationTestCase {
    public void testConstruction() {
        EndpointConfiguration c = new EndpointConfiguration("Test Configuration");
        assertEquals("Endpoint name incorrect", "Test Configuration", c.getEndpoint());
        assertNull("Subscription configuration non-null", c.getSubscriptionConfig());
        assertNull("Alarm configuration non-null", c.getAlarmConfig());

        assertEquals("Title string not of format '<name> (not subscribed)'",
                "Test Configuration (not subscribed)", c.getTitleString());
        assertNull("Alarm summary non-null", c.getAlarmSummary());
        assertFalse("isSubscribed returned true", c.isSubscribed());
        assertFalse("hasCreatedAlarm returned true", c.hasCreatedAlarm());
    }

    public void testAlarmTypeToFromString() {
        assertEquals("ABOVE bad", "above", AlarmType.makeString(AlarmType.ABOVE));
        assertEquals("BELOW bad", "below", AlarmType.makeString(AlarmType.BELOW));

        assertNotNull("above from string bad", AlarmType.fromString("above"));
        assertEquals("above from string not right", AlarmType.ABOVE, AlarmType.fromString("above"));
    }

    public void testAlarmConfigConstruction() {
        // TODO: test creation for each type?
        EndpointConfiguration.AlarmConfig c = new EndpointConfiguration.AlarmConfig(AlarmType.ABOVE, 0.0, 100);
        assertEquals("Alarm type not saved right", AlarmType.ABOVE, c.getType());
        assertEquals("Alarm interval not saved right", 100, c.getInterval());
        assertEquals("Alarm threshold not saved right", 0.0, c.getThreshold());
        // What does isCreated represent? I don't remember anymore.
        // Might have to do with create/delete alarms.
    }

    public void testSubscriptionConfigConstruction() {
        EndpointConfiguration.SubscriptionConfig c = new EndpointConfiguration.SubscriptionConfig(10);
        assertEquals("Wrong interval", 10, c.getInterval());
        assertFalse("isSubscribed is wrong", c.isSubscribed());
    }

    public void testEndpointConfigurationParceling() {
        EndpointConfiguration c, c2;
        EndpointConfiguration.SubscriptionConfig sc;
        EndpointConfiguration.AlarmConfig ac;

        c = new EndpointConfiguration("Oranges");
        // Write out to the parcel
        Parcel p = Parcel.obtain();
        c.writeToParcel(p, 0);
        // Read the configuration back out.
        c2 = EndpointConfiguration.CREATOR.createFromParcel(p);
        assertNotNull("Endpoint configuration createFromParcel came back null", c2);
        assertEquals("Endpoint name lost", "Oranges", c2.getEndpoint());
        assertNull("Subscription config somehow introduced", c2.getSubscriptionConfig());
        assertNull("Alarm config somehow introduced", c2.getAlarmConfig());

        c = new EndpointConfiguration("Apples");
        sc = new EndpointConfiguration.SubscriptionConfig(50);
        ac = new EndpointConfiguration.AlarmConfig(AlarmType.BELOW, 100.0, 10);
        c.setSubscriptionConfig(sc);
        c.setAlarmConfig(ac);
        // Write out to the parcel
        p = Parcel.obtain();
        c.writeToParcel(p, 0);
        // Read the configuration back out.
        c2 = EndpointConfiguration.CREATOR.createFromParcel(p);
        assertNotNull("Endpoint configuration createFromParcel came back null", c2);
        EndpointConfiguration.SubscriptionConfig sc2 = c2.getSubscriptionConfig();
        EndpointConfiguration.AlarmConfig ac2 = c2.getAlarmConfig();
        assertEquals("Endpoint name lost", "Apples", c2.getEndpoint());
        assertNotNull("Subscription config lost", sc2);
        assertNotNull("Alarm config lost", ac2);
        assertEquals("Subscription interval lost", sc.getInterval(), sc2.getInterval());
        assertEquals("Subscription status lost", sc.isSubscribed(), sc2.isSubscribed());
        assertEquals("Alarm type lost", ac.getType(), ac2.getType());
        assertEquals("Alarm threshold lost", ac.getThreshold(), ac2.getThreshold());
        assertEquals("Alarm interval lost", ac.getInterval(), ac2.getInterval());
    }
}
