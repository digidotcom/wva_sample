/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.digi.android.wva.R;

/**
 * A {@link DialogFragment} used to prompt the user for the username/password
 * for their WVA device.
 */
public class PreConnectionDialog extends DialogFragment {
	private String ip;
	
	/**
	 * Interface for activities (the DeviceListActivity in particular) to implement,
	 * so they can handle clicking "Ok" in this dialog and pass the username/password and
	 * IP address to DashboardActivity and connect to the device.
	 * 
	 * @author mwadsten
	 *
	 */
	public interface PreConnectionDialogListener {
        /**
         * Called when the "Okay" button is pressed on the dialog.
         */
		void onOkay(String ipAddress, String username, String password, boolean useHttps);
		
		/**
		 * Called when the "Cancel" button is pressed.
		 */
		void onCancelConnection();
	}

	public static PreConnectionDialog newInstance(String ipAddress) {
		// See Android developer documentation, DialogFragment
		PreConnectionDialog f = new PreConnectionDialog();
		
		Bundle args = new Bundle();
		args.putString("ip", ipAddress);
		f.setArguments(args);

		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		ip = args.getString("ip");
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof PreConnectionDialogListener)) {
			throw new ClassCastException(activity.toString() + " must implement AuthenticationDialogListener!");
		}
	}
	
	private String makeTitle() {
		return String.format(getResources().getString(R.string.pre_connection_dialog_title_template), this.ip);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		this.setCancelable(false);

		final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.pre_connection_dialog_layout, null);
		final PreConnectionDialogListener activity = (PreConnectionDialogListener) getActivity();

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(dialogView)
			   .setTitle(this.makeTitle())
			   .setPositiveButton(android.R.string.ok,
					   new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String username = ((EditText)dialogView.findViewById(R.id.auth_username)).getText().toString();
						String password = ((EditText)dialogView.findViewById(R.id.auth_password)).getText().toString();
						boolean useHttps = ((CheckBox)dialogView.findViewById(R.id.https_checkbox)).isChecked();
						
						// Turn null username/password into empty strings. Otherwise, we get IllegalArgumentException
						// for passing null username into setBasicAuth within WVALib code.
						if (username == null) {
							username = "";
						}
						if (password == null) {
							password = "";
						}
						activity.onOkay(ip, username, password, useHttps);
					}
			   }).setNegativeButton(android.R.string.cancel,
					   new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.onCancelConnection();
					}
				});
		return builder.create();
	}
}
