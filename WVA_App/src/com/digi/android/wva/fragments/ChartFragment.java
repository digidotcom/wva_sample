/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.fragments;

import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.digi.android.wva.WvaApplication;
import com.digi.android.wva.model.VehicleData;
import com.digi.android.wva.util.MessageCourier;
import com.digi.wva.async.WvaCallback;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.LineChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The {@link Fragment} which holds and displays the data graph, as well
 * as handling incoming data to be plotted.
 *
 * @author mwadsten
 */
public class ChartFragment extends Fragment {
	private static final String TAG = "ChartFragment";
    /**
     * Amount of time, in minutes (minutes * 60(seconds) * 1000(milliseconds))
     * to display on the graph at one time.
     */
    private static final int TIMESPAN = 15 * 60 * 1000;
    private final MessageHandler mHandler = new MessageHandler(this);
    private GraphicalView mChart;
	private XYMultipleSeriesDataset mDataset;
	private XYMultipleSeriesRenderer mRenderer;
	private XYSeriesRenderer mSpeedRenderer, mRpmRenderer;
    private double startTime, endTime;
    private boolean paused = false;
    private boolean subscribed = false;
    private boolean isTesting = false;
    // We add the last speed and RPM values to the graph after shifting the
    // X-axis so that we have a visual indication of the difference between
    // them and the new ones.
    private VehicleData lastSpeed, lastRPM;
    private final Object shiftLock = new Object();
    private WvaApplication app;

    private static final int MESSAGE_LOOP_INTERVAL = 1000;

    private static class MessageHandler extends Handler {
    	private final ChartFragment fragment;
    	public MessageHandler(ChartFragment frag) {
    		fragment = frag;
    	}
        @Override
        public void handleMessage(Message msg) {
            fragment.processMessages();
        }

        public void sleep() {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), MESSAGE_LOOP_INTERVAL);
        }
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Don't recreate fragment if activity gets recreated -- just hold
		// onto the existing instance.
		// This is to ensure that the chart remains in existence.
		setRetainInstance(true);

		
		startTime = DateTime.now().getMillis();
		// endTime is 15 minutes after startTime
		endTime = startTime + TIMESPAN;
		
		buildGraphPieces();
		clearDataset();

        app = (WvaApplication) getActivity().getApplication();
	}

    @Override
	public void onPause() {
        // Ensure messages stop being processed.
        mHandler.removeMessages(0);
        paused = true;
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
        if (!subscribed) {
            // Will only happen on first start-up.
            // Get any messages waiting for the chart. If there are no
            // errors, drop any other messages (e.g. "reconnecting"/"connected")
            MessageCourier.ChartMessage[] msgs = MessageCourier.getChartMessages();
            if (msgs.length == 0 || msgs[0].getError() == null) {
                // No errors (they appear at front of queue);
                // ignore any other messages.
                Log.d(TAG, "No errors on entry.");
            } else {
                Log.d(TAG, "Encountered error upon entry.");
                processMessage(msgs[0]);
                return;
            }
            subscribed = true;
            if (!isTesting) {
                app.subscribeToEndpoint("EngineSpeed", 10, new WvaCallback<Void>() {
                    @Override
                    public void onResponse(Throwable error, Void response) {
                        if (error != null) {
                            Log.e(TAG, "Unable to subscribe to EngineSpeed", error);
                            Toast.makeText(getActivity(), "Unable to subscribe to EngineSpeed: " + error, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "Successfully subscribed to EngineSpeed.");
                            Toast.makeText(getActivity(), "Subscribed to EngineSpeed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                app.subscribeToEndpoint("VehicleSpeed", 10, new WvaCallback<Void>() {
                    @Override
                    public void onResponse(Throwable error, Void response) {
                        if (error != null) {
                            Log.e(TAG, "Unable to subscribe to VehicleSpeed", error);
                            Toast.makeText(getActivity(), "Unable to subscribe to VehicleSpeed: " + error, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "Successfully subscribed to VehicleSpeed.");
                            Toast.makeText(getActivity(), "Subscribed to VehicleSpeed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }

        paused = false;
        processMessages();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Add chart view to container
		if (container == null) {
			container = new LinearLayout(getActivity());
		}
		container.addView(ChartFactory.getCombinedXYChartView(getActivity(), mDataset, mRenderer, new String[] { LineChart.TYPE, LineChart.TYPE}));
		return container;
	}

    /**
     * In the process of unit-testing, effectively mocking the WvaApplication
     * and making that mock application accessible from this fragment has
     * proven to be extremely difficult, if not impossible, because of
     * @param isTesting
     */
    public void setIsTesting(boolean isTesting) {
        this.isTesting = isTesting;
    }
	
	/**
	 * Repaints the chart view onto the screen. This is done by
	 * removing the chart view from the layout, calling repaint(),
	 * and putting the view back into the layout.
	 * 
	 * (Add StackOverflow link?)
	 */
	private void redrawChart() {
		// super.getView() should return NoSaveStateFrameLayout, which
		// extends FrameLayout, which extends ViewGroup
		ViewGroup l = (ViewGroup)super.getView();
		if (mChart == null) {
			mChart = ChartFactory.getCombinedXYChartView(
					getActivity(), mDataset, mRenderer,
					new String[] {LineChart.TYPE, LineChart.TYPE});
			mChart.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					SeriesSelection sel = mChart.getCurrentSeriesAndPoint();
					
					if (sel == null)
						return;
					
					String series = "SERIES";
					switch (sel.getSeriesIndex()) {
					case 0:
						series = "Vehicle Speed";
						break;
					case 1:
						series = "Engine RPM";
						break;
					}
					
					String time = ISODateTimeFormat.dateTimeNoMillis().print((long)sel.getXValue());
					
					Toast.makeText(getActivity(),
							series + " (" + time + "): " + sel.getValue(),
							Toast.LENGTH_SHORT).show();
				}
			});
		}
		if (l != null) {
            // Ensure that the chart is removed from all views.
            try {
                if (mChart.getParent() != null) {
                    //noinspection ConstantConditions
                    ((ViewGroup) mChart.getParent()).removeAllViews();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            l.removeAllViews();
            mChart.repaint();
            l.addView(mChart);
        } else {
            // If the getView() result is null, something's up, like
            // the activity has been backgrounded.
            Log.d(TAG, "redrawChart -- l is null");
        }
    }
	
	private void buildGraphPieces() {
		if (mRenderer != null || mDataset != null
			|| mRpmRenderer != null || mSpeedRenderer != null) {
			// Don't want to leak any memory or whatnot.
			return;
		}
		mRenderer = new XYMultipleSeriesRenderer(2);
		mDataset = new XYMultipleSeriesDataset();
		mSpeedRenderer = new XYSeriesRenderer();
		mRpmRenderer = new XYSeriesRenderer();
		
		// Initialize renderer settings
		mRenderer.setShowGrid(true);
		mRenderer.setFitLegend(true);
		// Number of grid lines in either direction by default
		mRenderer.setXLabels(0);
		mRenderer.setYLabels(10);
		mRenderer.setXLabelsAlign(Align.RIGHT);
		mRenderer.setYLabelsAlign(Align.RIGHT);
		mRenderer.setPointSize(5f);
		// AChartEngine output defaults to a black background.
		// This doesn't fit with the general WVA color scheme.
		mRenderer.setApplyBackgroundColor(true);
		mRenderer.setBackgroundColor(Color.WHITE);
		mRenderer.setMarginsColor(Color.WHITE);
		mRenderer.setAxesColor(Color.DKGRAY);
		mRenderer.setLabelsColor(Color.BLACK);
		mRenderer.setXLabelsColor(Color.DKGRAY);
		mRenderer.setYLabelsColor(0, Color.DKGRAY);
        mRenderer.setYLabelsColor(1, Color.DKGRAY);
		mRenderer.setGridColor(Color.LTGRAY);
		mRenderer.setPanEnabled(false, false);
		mRenderer.setZoomEnabled(false, false);
		mRenderer.setXAxisMin(startTime);
		mRenderer.setXAxisMax(endTime);
        mRenderer.setXAxisMin(startTime, 1);
        mRenderer.setXAxisMax(endTime, 1);
		mRenderer.setYAxisMin(0, 0);
		mRenderer.setYAxisMax(100, 0);

		mSpeedRenderer.setColor(Color.RED);
		mSpeedRenderer.setPointStyle(PointStyle.CIRCLE);
		mSpeedRenderer.setFillPoints(true);
		
		mRpmRenderer.setColor(Color.BLUE);
		mRpmRenderer.setPointStyle(PointStyle.SQUARE);
		mRpmRenderer.setFillPoints(true);
		
		XYSeries speedSeries = new XYSeries("Vehicle Speed");
		XYSeries rpmSeries = new XYSeries("Engine RPM", 1);
		
		mDataset.addSeries(0, speedSeries);
		mDataset.addSeries(1, rpmSeries);
		mRenderer.addSeriesRenderer(0, mSpeedRenderer);
		mRenderer.addSeriesRenderer(1, mRpmRenderer);

        mRenderer.setYAxisMin(0, 1);
        mRenderer.setYAxisMax(10000, 1);

        mRenderer.setYTitle("VehicleSpeed");
        mRenderer.setYTitle("EngineSpeed", 1);
        mRenderer.setYAxisAlign(Align.RIGHT, 1);
        mRenderer.setYLabelsAlign(Align.RIGHT, 1);
		
		// Add X-axis labels with time.
		Log.d(TAG, "Time range: " + startTime + " to " + endTime);
		SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss", Locale.US);
		for (double t = startTime; t <= endTime; t += 60*1000) {
			String time = fmt.format(new Date((long)t));
			Log.d(TAG, "Adding label " + t + ", " + time);
			mRenderer.addXTextLabel(t, time);
		}
	}

    /**
     * Does what it says on the tin: clears out the data sets.
     */
	public void clearDataset() {
		if (mDataset != null && mDataset.getSeriesCount() == 2) {
			for (XYSeries series : mDataset.getSeries()) {
				series.clear();
			}
		}
	}

    /**
     * Fetch the XYSeries representing the sequence of speed values
     * @return the speed series, or null if there is an error or the series
     * is itself null
     */
	public XYSeries getSpeedSeries() {
		if (mDataset != null)
			try {
				return mDataset.getSeriesAt(0);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		return null;
	}

    /**
     * Fetch the XYSeries representing the sequence of RPM values
     * @return the RPM series, or null if there is an error or the series
     * is itself null
     */
	public XYSeries getRpmSeries() {
		if (mDataset != null)
			try {
				return mDataset.getSeriesAt(1);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		return null;
	}

    /**
     * Fetch the last vehicle speed data point
     * @return the last speed data
     */
    public VehicleData getLastSpeed() {
        return lastSpeed;
    }

    /**
     * Fetch the last ending rpm data point
     * @return the last rpm data
     */
    public VehicleData getLastRPM() {
        return lastRPM;
    }

    /**
     * Get the X-axis start time
     * @return X-axis start time
     */
    public double getStartTime() {
        return startTime;
    }

    /**
     * Get the X-axis end time
     * @return the X-axis end time
     */
    public double getEndTime() {
        return endTime;
    }

    /**
     * Iterate over {@link MessageCourier#getChartMessages() any pending messages}
     * and call {@link #processMessage(com.digi.android.wva.util.MessageCourier.ChartMessage)}
     * on each of them.
     */
    private void processMessages() {
        for (MessageCourier.ChartMessage message : MessageCourier.getChartMessages()) {
            if (processMessage(message)) {
                // processMessage returning true means that the message was
                // an error, and so we should have displayed an error
                // dialog; we want to stop processing messages and get ready
                // for leaving the chart activity.
                return;
            }
        }
        // Stop mHandler loop if paused.
        if (paused)
            return;
        mHandler.sleep();
    }

    /**
     * Process a {@link com.digi.android.wva.util.MessageCourier.ChartMessage}
     * and deal with its information. ChartMessages can be either
     * notifications of connection error, or they can contain new data.
     * @param msg the ChartMessage to be processed
     * @return true if the message was an error, and we should stop processing
     * any more messages
     */
    private boolean processMessage(MessageCourier.ChartMessage msg) {
        if (msg.isReconnecting()) {
            Toast.makeText(getActivity(), "Reconnecting...", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(msg.getError())) {
            // No error message - must be new vehicle data.
            VehicleData data = msg.getData();
            if (data == null) {
                // This is odd!
                Log.e(TAG, "processMessage - got message without error or data.");
                return false;
            }
            else {
                handleNewData(data);
                return false;
            }
        }
        else {
            // error is non-null/empty -- there is an error to display
            String error = msg.getError();
            Log.e(TAG, "Error: " + error);
            ConnectionErrorDialog dialog = ConnectionErrorDialog.newInstance("Connection error", error);
            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            ft.addToBackStack(null);
            dialog.show(ft, "error_dialog");
            return true;
        }
    }

    /**
     * Process an incoming piece of {@link VehicleData} and
     * plot it on the graph.
     *
     * <p>This method is public so that its behavior can be tested.</p>
     * @param incoming the {@link VehicleData} point to plot on screen
     */
    public void handleNewData(VehicleData incoming) {
        try {
            String endpoint = incoming.name;
            double value = incoming.value;
            long timeMs = incoming.timestamp.getMillis();
            if (!isTesting)
                Log.d(TAG, "Got new data on " + endpoint + ", value: " + value + ", time: " + timeMs);

            // Synchronize on shiftLock so that if two data points come in
            // practically simultaneously, we don't shift the view ahead and
            // then shift ahead another time.
            synchronized (shiftLock) {
                if (timeMs > endTime) {
                    // Shift graph view ahead so current endTime becomes new startTime
                    startTime = endTime;
                    endTime = startTime + TIMESPAN;
                    /*
                    Wipe out dataset and renderers, so that we can rebuild them
                    using buildGraphPieces, and they will reflect the new
                    start and end times.
                    */
                    clearDataset();
                    mRenderer.clearXTextLabels();
                    mRenderer.removeSeriesRenderer(mRpmRenderer);
                    mRenderer.removeSeriesRenderer(mSpeedRenderer);
                    mRenderer = null;
                    mDataset = null;
                    mRpmRenderer = null;
                    mSpeedRenderer = null;
                    mChart = null;
                    buildGraphPieces();
                    try {
                        ((ViewGroup)getView()).removeAllViews();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (lastSpeed != null && getSpeedSeries() != null) {
                        getSpeedSeries().add(lastSpeed.timestamp.getMillis(), lastSpeed.value);
                    }
                    if (lastRPM != null && getRpmSeries() != null) {
                        getRpmSeries().add(lastRPM.timestamp.getMillis(), lastRPM.value);
                    }
                }
            }

            // Add the new data point to the graph.
            if ("VehicleSpeed".equals(endpoint)) {
                XYSeries series = getSpeedSeries();
                if (series != null) {
                    series.add(timeMs, value);
                }
                lastSpeed = incoming;
            }
            else if ("EngineSpeed".equals(endpoint)) {
                XYSeries series = getRpmSeries();
                if (series != null) {
                    series.add(timeMs, value);
                }
                lastRPM = incoming;
            } else {
                Log.d(TAG, "Unknown graphing endpoint: " + endpoint);
            }

            // Redraw the chart on-screen so the new data point is visible.
            redrawChart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
