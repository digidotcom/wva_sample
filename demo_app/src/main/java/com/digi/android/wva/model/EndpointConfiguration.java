/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.digi.wva.async.AlarmType;

/**
 * Encapsulation of subscription/alarm status on endpoints.
 * 
 * Implements the Parcelable interface so that holding onto EndpointConfiguration
 * instances can be done by calling putParcelable(---) on Bundles when necessary
 * (e.g. in EndpointOptionsDialog)
 * @author mwadsten
 *
 */
public class EndpointConfiguration implements Parcelable {
    /**
     * Representation of alarm configurations.
     */
	public static class AlarmConfig {
		private final int interval;
		private final AlarmType type;
		private final double threshold;
		private boolean isCreated;
		
		public AlarmConfig(AlarmType type,	double threshold, int interval) {
			this.type = type;
			this.threshold = threshold;
			this.interval = interval;
			isCreated = false;
		}

        /**
         * Get the alarm type of this alarm configuration
         * @return alarm type
         */
        public AlarmType getType() {
			return type;
		}

        /**
         * Get the threshold of this alarm configuration
         * @return threshold
         */
		public double getThreshold() {
			return threshold;
		}

        /**
         * Get the interval of this alarm configuration
         * @return interval
         */
        public int getInterval() {
            return interval;
        }

        /**
         * Set whether this alarm is considered to be "created" (e.g. sent to device)
         * @param set true if the alarm has been created on device
         */
		public void setCreated(boolean set) {
			this.isCreated = set;
		}

        /**
         * Get whether this alarm is considered to be "created" or not
         * @return true if the alarm is created
         */
		public boolean isCreated() {
			return isCreated;
		}
	}

    /**
     * Representation of subscription configurations.
     */
	public static class SubscriptionConfig {
		private final int interval;
		private boolean isSubscribed;
		
		public SubscriptionConfig(int interval) {
			this.interval = interval;
			isSubscribed = false;
		}

        /**
         * Get the subscription interval.
         * @return the subscription interval
         */
        public int getInterval() {
			return interval;
		}

        /**
         * Set the boolean indicating if there is an active subscription
         * on this endpoint
         * @param set boolean to store
         */
		public void setSubscribed(boolean set) {
			this.isSubscribed = set;
		}

        /**
         * Fetch the boolean indicating if there is an active subscription
         * on this endpoint
         * @return true if there is an active subscription on this endpoint
         */
		public boolean isSubscribed() {
			return isSubscribed;
		}
	}
	
	private final String endpointName;
	private AlarmConfig mAlarmC;
	private SubscriptionConfig mSubC;
	private boolean shouldBePushedToDC;
	
	public EndpointConfiguration(String endpoint) {
		this.endpointName = endpoint;
		
		this.shouldBePushedToDC = false;
	}

    /**
     * Get the endpoint name of this configuration.
     * @return endpoint name passed into constructor
     */
	public String getEndpoint() {
		return endpointName;
	}

    /**
     * Returns true if there is an active subscription on this endpoint.
     * @return true if there is an active subscription on this endpoint
     */
	public boolean isSubscribed() {
		return (mSubC != null && mSubC.isSubscribed());
	}

    /**
     * Returns true if the alarm configuration has been set on the device
     * @return whether the alarm config has been set on the device
     */
	public boolean hasCreatedAlarm() {
		return (mAlarmC != null && mAlarmC.isCreated());
	}

    /**
     * Fetch the subscription configuration, if any.
     * @return the subscription config contained herein
     */
	public SubscriptionConfig getSubscriptionConfig() {
		return mSubC;
	}

    /**
     * Set the subscription configuration
     * @param conf the {@link SubscriptionConfig} to set
     */
	public void setSubscriptionConfig(SubscriptionConfig conf) {
		this.mSubC = conf;
	}

    /**
     * Fetch the alarm configuration, if any.
     * @return the alarm configuration contained herein
     */
	public AlarmConfig getAlarmConfig() {
		return mAlarmC;
	}

    /**
     * Set the alarm configuration
     * @param conf the {@link AlarmConfig} to set
     */
	public void setAlarmConfig(AlarmConfig conf) {
		this.mAlarmC = conf;
	}

    /**
     * Get a string representation of the subscription configuration state
     * (endpoint name, subscription status). Used in {@link com.digi.android.wva.adapters.EndpointsAdapter}
     * @return a one-line representation of subscription status
     */
	public String getTitleString() {
		if (mSubC != null) {
			return endpointName;
		} else {
			return endpointName + " (not subscribed)";
		}
	}

	/**
	 * Create the string used to populate the second line of
	 * the listview item. Null if there is none.
	 * @return a string describing alarm setup
	 */
	public String getAlarmSummary() {
		if (mAlarmC == null)
			return null;
		else {
			String fmt = "Alarm when %s %s";
            AlarmType type = mAlarmC.getType();
			String alarmType = AlarmType.makeString(type);
			String thresh = Double.toString(mAlarmC.getThreshold());
            if (type == AlarmType.CHANGE)
                return "Alarm when value changes";
            if (type == AlarmType.DELTA)
                return "Alarm when value changes by " + thresh;
			return String.format(fmt, alarmType, thresh);
		}
	}
	
	/**
	 * Set the internal flag indicating whether new data arriving from this endpoint shall be
	 * sent to Device Cloud.
	 * @param set boolean to indicate if this data should be uploaded to Device Cloud
	 */
	public void setPushToDeviceCloud(boolean set) {
		this.shouldBePushedToDC = set;
	}
	
	/**
	 * Query if data from this endpoint should be uploaded to Device Cloud.
	 * @return true if data will be uploaded, false otherwise
	 */
	public boolean shouldBePushedToDeviceCloud() {
		return this.shouldBePushedToDC;
	}

	@Override
	public int describeContents() {
		// TODO: Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(endpointName);
        dest.writeByte((byte) (this.shouldBePushedToDC ? 1 : 0));
        
        // For both AlarmConfig and SubscriptionConfig, the first
        // value read out of the parcel (an integer) signifies if the
        // configuration in question was null or not, when writeToParcel
        // was called. (0 -> null, 1 -> not null)
        if (mAlarmC == null) {
            dest.writeInt(0);
            dest.writeString(null);
            dest.writeInt(0);
            dest.writeDouble(0);
            dest.writeByte((byte) 0);
        } else {
            dest.writeInt(1);
            dest.writeString(AlarmType.makeString(mAlarmC.getType()));
            dest.writeInt(mAlarmC.getInterval());
            dest.writeDouble(mAlarmC.getThreshold());
            dest.writeByte((byte) (mAlarmC.isCreated() ? 1 : 0));
        }

        if (mSubC == null) {
            dest.writeInt(0);
            dest.writeInt(0);
            dest.writeByte((byte) 0);
        } else {
            dest.writeInt(1);
            dest.writeInt(mSubC.getInterval());
            dest.writeByte((byte) (mSubC.isSubscribed() ? 1 : 0));
        }
    }
	
	@SuppressWarnings("UnusedDeclaration")
    public static final Parcelable.Creator<EndpointConfiguration> CREATOR =
			new Parcelable.Creator<EndpointConfiguration>() {
				@Override
				public EndpointConfiguration createFromParcel(Parcel source) {
                    source.setDataPosition(0); // stackoverflow.com/q/12829700

					String name = source.readString();
                    EndpointConfiguration conf = new EndpointConfiguration(name);
                    
                    conf.setPushToDeviceCloud(source.readByte() != 0);

                    boolean alarmExists = (source.readInt() != 0);
                    if (alarmExists) {
                        AlarmType type = AlarmType.fromString(source.readString());
                        int interval = source.readInt();
                        double threshold = source.readDouble();
                        boolean created = (source.readByte() != 0);
                        AlarmConfig c = new AlarmConfig(type, threshold, interval);
                        c.setCreated(created);
                        conf.setAlarmConfig(c);
                    } else {
                        // we can ignore the rest of the alarm configuration,
                        // because it was null going into
                        // writeToParcel()
                        source.readString();
                        source.readInt();
                        source.readDouble();
                        source.readByte();
                    }

                    boolean subExists = (source.readInt() != 0);
                    if (subExists) {
                        int interval = source.readInt();
                        boolean subscribed = (source.readByte() != 0);
                        SubscriptionConfig c = new SubscriptionConfig(interval);
                        c.setSubscribed(subscribed);
                        conf.setSubscriptionConfig(c);
                    } else {
                        // Ignore the rest of the subscription configuration
                        source.readInt();
                        source.readByte();
                    }

					return conf;
				}

				@Override
				public EndpointConfiguration[] newArray(int size) {
					// TODO Auto-generated method stub
					return null;
				}
			};
}
