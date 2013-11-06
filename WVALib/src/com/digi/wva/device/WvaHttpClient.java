/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.device;

import java.io.UnsupportedEncodingException;

import android.annotation.TargetApi;
import android.os.Build;
import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.json.JSONObject;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

@SuppressWarnings("UnusedDeclaration")
public class WvaHttpClient {
	private final String TAG = "com.digi.wva.device.WvaHttpClient";

	/**
	 * Provides the basic format of a WVA web service resource.
	 * For instance: http://192.168.0.3/ws/vehicle/EngineSpeed
	 */
	private final static String BASE_URL = "http://%s/ws/%s";
	private final String hostname;
	private final AsyncHttpClient client;
	
	private final Header[] jsonHeaders = new Header[] {
			new BasicHeader("Accept", "application/json")
	};
	
	public WvaHttpClient(String hostname) {
		this.client = new AsyncHttpClient();
		this.hostname = hostname;
	}
	
	public void get(String url, AsyncHttpResponseHandler responseHandler) {
		client.get(null, getAbsoluteUrl(url), jsonHeaders, null, responseHandler);
	}

	public void put(String url, JSONObject jObj, AsyncHttpResponseHandler responseHandler) {
		StringEntity entity;
		try {
			entity = new StringEntity(jObj.toString(), "UTF-8");
			client.put(null, getAbsoluteUrl(url), entity, "application/json", responseHandler);
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "unsupported encoding in put request");
		}
	}

	public void delete(String url, AsyncHttpResponseHandler responseHandler) {
		client.delete(null, getAbsoluteUrl(url), jsonHeaders, responseHandler);
	}


	@TargetApi(Build.VERSION_CODES.FROYO)
    public void post(String url, JSONObject jObj, AsyncHttpResponseHandler responseHandler) {
		StringEntity entity;
		try {
			entity = new StringEntity(jObj.toString(), "UTF-8");
			client.post(null, getAbsoluteUrl(url), entity, "application/json", responseHandler);
		} catch (UnsupportedEncodingException e) {
            Log.wtf(TAG, "unsupported encoding in put request");
		}
	}

	public String getAbsoluteUrl(String relativePath) {
		return String.format(BASE_URL, hostname, relativePath);
	}
}

