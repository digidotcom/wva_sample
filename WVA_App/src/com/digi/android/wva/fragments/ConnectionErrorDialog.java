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

/**
 * A {@link DialogFragment} used to display error messages related to the
 * connection with a WVA device.
 */
public class ConnectionErrorDialog extends DialogFragment {
	private String title, message;
	
	/**
	 * Interface for activities to implement,
	 * so they can handle clicking "Ok" in this dialog and
     * (in most cases) call finish() on themselves (as a response to the
     * error).
	 * 
	 * @see <a href="http://stackoverflow.com/q/7557265">StackOverflow inspiration</a>
	 * 
	 * @author mwadsten
	 *
	 */
	public interface ErrorDialogListener {
        /**
         * Called when the "Okay" button is pressed on the error dialog.
         */
		void onOkay();
	}

	public static ConnectionErrorDialog newInstance(String title, String message) {
		// See Android developer documentation, DialogFragment
		ConnectionErrorDialog f = new ConnectionErrorDialog();
		
		Bundle args = new Bundle();
		args.putString("title", title);
		args.putString("message", message);
		f.setArguments(args);

		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		title = args.getString("title");
		message = args.getString("message");
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof ErrorDialogListener)) {
			throw new ClassCastException(activity.toString() + " must implement ErrorDialogListener!");
		}
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		this.setCancelable(false);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(this.message)
			   .setTitle(this.title)
			   .setPositiveButton(android.R.string.ok,
					   new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((ErrorDialogListener)getActivity()).onOkay();
				}
			});
		return builder.create();
	}
}
