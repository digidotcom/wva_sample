/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.util;

import com.actionbarsherlock.view.MenuItem;
import com.digi.android.wva.R;

/**
 * Utility class to help handle displaying either the Refresh icon
 * or an indeterminate progress bar (aka "spinny thing") in the action
 * bar depending on what state we want it to be in.
 * @author mwadsten
 *
 */
public final class RefreshManager {
	private MenuItem icon;
	private boolean isRefreshing;

    /**
     * Set the {@link MenuItem} whose action view will be manipulated
     * by the refresh manager
     * @param icon icon to manipulate
     */
	public void setIcon(MenuItem icon) {
		this.icon = icon;
	}

    /**
     * Set the state of the refresh manager, and the state of the refreshing
     * action view along with it.
     * @param refreshing true if the icon should indicate active refreshing, false
     *                   if the icon should go back to a static refresh icon
     */
	public void setRefreshing(boolean refreshing) {
		if (icon == null) {
//			Log.d("RefreshManager", "setRefreshing called before setIcon");
			return;
		}

		isRefreshing = refreshing;

		if (isRefreshing)
			icon.setActionView(R.layout.action_bar_indeterminate_progress);
		else
			icon.setActionView(null);
	}

    /**
     * Indicate the refresh manager's current state.
     * <p>The method name is "isNotRefreshing" because this method is only
     * ever invoked to check if the state is "not refreshing"; therefore, it
     * makes more sense to name it "isNotRefreshing" than to always have the
     * code read "!isRefreshing()"</p>
     * @return true if the refresh manager is not currently in a state of
     * displaying an indeterminate-progress view, indicating refreshing
     */
	public boolean isNotRefreshing() {
		// If the icon is null or isn't showing the action view,
		// we aren't really "refreshing" per se.
		return !isRefreshing || icon == null || icon.getActionView() == null;
	}
}