/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.device;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import android.annotation.TargetApi;
import android.os.Build;
import org.apache.http.Header;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ssl.SSLSocketFactory;
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
	private static String BASE_URL;
	private final String hostname;
	private final AsyncHttpClient client;
	
	private final Header[] jsonHeaders = new Header[] {
			new BasicHeader("Accept", "application/json")
	};
	
	// http://stackoverflow.com/a/12082810
	private class WvaSSLSocketFactory extends SSLSocketFactory {
		SSLContext context = SSLContext.getInstance("TLS");
		
		public WvaSSLSocketFactory(KeyStore truststore)
				throws NoSuchAlgorithmException, KeyManagementException,
				KeyStoreException, UnrecoverableKeyException {
			super(truststore);
			
			X509TrustManager tm = new X509TrustManager() {
				
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
				
				@Override
				public void checkServerTrusted(X509Certificate[] arg0, String arg1)
						throws CertificateException {
				}
				
				@Override
				public void checkClientTrusted(X509Certificate[] arg0, String arg1)
						throws CertificateException {
				}
			};
			
			context.init(null, new X509TrustManager[] { tm }, null);
		}

		@Override
		public Socket createSocket() throws IOException {
			return context.getSocketFactory().createSocket();
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port,
				boolean autoClose) throws IOException, UnknownHostException {
			return context.getSocketFactory().createSocket(socket, host, port, autoClose);
		}
	}
	
	/**
	 * Returns an SSLSocketFactory which trusts any certificate. (Needed in order to connect
	 * with the WVA when using HTTPS.)
	 * @return an SSLSocketFactory which trusts all certificates
	 */
	private SSLSocketFactory makeSSLSocketFactory() {
		// based on information from:
		// http://engineering.sproutsocial.com/2013/09/android-using-volley-and-loopj-with-self-signed-certificates/
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);
			WvaSSLSocketFactory sf = new WvaSSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			
			return sf;
		} catch (Exception e) {
			return null;
		}
	}
	
	public WvaHttpClient(String hostname) {
		this(hostname, false, null, null, false);
	}
	
	public WvaHttpClient(String hostname, boolean needsAuth, String authName, String authPass, boolean useHttps) {
		this.client = new AsyncHttpClient();
		this.hostname = hostname;

		if (useHttps) {
			BASE_URL = "https://%s/ws/%s";
			// Enable connection via HTTPS
			this.client.setSSLSocketFactory(makeSSLSocketFactory());
		} else {
			BASE_URL = "http://%s/ws/%s";
		}

		// Add basic auth username/password if needed
		if (needsAuth) {
			this.client.setBasicAuth(authName, authPass);
		}
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

