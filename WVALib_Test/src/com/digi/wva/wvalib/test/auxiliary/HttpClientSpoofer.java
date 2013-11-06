/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.wvalib.test.auxiliary;

import com.digi.wva.device.WvaHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import org.json.JSONObject;

/**
 * Created by awickert on 5/22/13.
 */
public class HttpClientSpoofer extends WvaHttpClient {
    public JSONObject returnObject = null;
    public String returnString = null;
    public boolean success = true;

    public HttpClientSpoofer(String hostname) {
        super(hostname);
    }

    private void spoof(AsyncHttpResponseHandler handler) {
        System.out.println("here");
        if (handler instanceof JsonHttpResponseHandler) {
            JsonHttpResponseHandler jHandler = (JsonHttpResponseHandler) handler;
            if (success == true) {
                jHandler.onSuccess(returnObject);
            }
            else {
                jHandler.onFailure(new Throwable(), returnObject);
            }
        }
        else {
            if (success) {
                handler.onSuccess(returnString);
            }
            else {
                handler.onFailure(new Throwable(), returnString);
            }
        }
    }

    @Override
    public void get(String url, AsyncHttpResponseHandler responseHandler) {
        spoof(responseHandler);
    }

    @Override
    public void put(String url, JSONObject jObj, AsyncHttpResponseHandler responseHandler) {
        spoof(responseHandler);
    }

    @Override
    public void delete(String url, AsyncHttpResponseHandler responseHandler) {
        spoof(responseHandler);
    }

    @Override
    public void post(String url, JSONObject jObj, AsyncHttpResponseHandler responseHandler) {
        spoof(responseHandler);
    }
}
