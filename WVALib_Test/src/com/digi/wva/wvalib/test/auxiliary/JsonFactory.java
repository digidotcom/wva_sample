/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.wvalib.test.auxiliary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

/**
 * Contains static methods which return JSONObjects. Superficially, this
 * allows the tests to all use identical data without needing to reconstruct
 * messages from the device in every test. More importantly, these methods
 * should reflect the API of the WVA Device's web services.
 */
public class JsonFactory {

	public JsonFactory() { }

	public JSONObject data() {
		// Create some fake data
		JSONObject dataObj = new JSONObject();
		JSONObject infoObj = new JSONObject();
		JSONObject endpointObj = new JSONObject();
        try {
            endpointObj.put("value",  4.3);
            endpointObj.put("timestamp", "2007-03-01T13:00:00Z");

            infoObj.put("timestamp", "2007-03-01T12:00:00Z");
            infoObj.put("uri", "/ws/foo/bar/baz");
            infoObj.put("short_name", "baz~sub");
            infoObj.put("baz", endpointObj);

            dataObj.put("data", infoObj);
        } catch (JSONException e) {
            return null;
        }

		return dataObj;
	}

	public JSONObject alarm() {
		// Create some fake data
		JSONObject alarmObj = new JSONObject();
		JSONObject infoObj = new JSONObject();
		JSONObject endpointObj = new JSONObject();
        try {
            endpointObj.put("value",  4.3);
            endpointObj.put("timestamp", "2007-03-01T13:00:00Z");

            infoObj.put("timestamp", "2007-03-01T12:00:00Z");
            infoObj.put("uri", "/ws/foo/bar/baz");
            infoObj.put("short_name", "baz~above");
            infoObj.put("baz", endpointObj);

            alarmObj.put("alarm", infoObj);
        } catch (JSONException e) {
            return null;
        }
        return alarmObj;
	}

    public JSONObject valTimeObj() {
        JSONObject vtObj = new JSONObject();
        try {
            vtObj.put("value", 5.0);
            vtObj.put("timestamp", "2007-03-01T13:00:00Z");
        } catch (JSONException e) {
            return null;
        }
        return vtObj;
    }

    public JSONObject vehicleEndpoints() {
        JSONObject obj = new JSONObject();
        JSONArray list = new JSONArray();
        try {
            list.put("vehicle/data/EngineSpeed");
            list.put("vehicle/data/PorridgeViscosity");
            list.put("vehicle/data/PassengerEuphoria");
            list.put("vehicle/data/DriverIncome");
            list.put("vehicle/data/baz");
            obj.put("data", list);
        } catch (JSONException e) {
            return null;
        }
        return obj;
    }

    public JSONObject vehicleDataEndpoint() {
        JSONObject dataObj = new JSONObject();
        JSONObject valTimeObj = this.valTimeObj();
        try {
            dataObj.put("EngineSpeed", valTimeObj);
        } catch (JSONException e) {
            return null;
        }
        return dataObj;
    }

    public JSONObject ledEndpoints() {
        JSONObject obj = new JSONObject();
        JSONArray list = new JSONArray();
        try {
            list.put("hw/leds/led0");
            list.put("hw/leds/zeppelin");
            list.put("hw/leds/fred");
            obj.put("leds", list);
        } catch (JSONException e) {
            return null;
        }
        return obj;
    }

    public JSONObject buttonEndpoints() {
        JSONObject obj = new JSONObject();
        JSONArray list = new JSONArray();
        try {
            list.put("hw/buttons/reset");
            list.put("hw/buttons/rewind");
            list.put("hw/buttons/eject");
            list.put("hw/buttons/big_red");
            obj.put("buttons", list);
        } catch (JSONException e) {
            return null;
        }
        return obj;
    }
    public JSONObject time() {
        JSONObject timeObj = new JSONObject();
        try {
            timeObj.put("time", "2007-03-01T13:00:01Z");
        } catch (JSONException e) {
            return null;
        }
        return timeObj;
    }

    public JSONObject ledState(boolean onOff) {
        JSONObject ledObj = new JSONObject();
        try {
            ledObj.put("led", onOff ? "on" : "off");
        } catch (JSONException e) {
            return null;
        }
        return ledObj;
    }

    public JSONObject buttonState(boolean upDown) {
        JSONObject buttonObj = new JSONObject();
        try {
            buttonObj.put("button", upDown ? "up" : "down");
        } catch (JSONException e) {
            return null;
        }
        return buttonObj;
    }

    public JSONObject ecuNames() {
        JSONObject ecuObj = new JSONObject();
        JSONArray ecuNames = new JSONArray();
        try {
            ecuNames.put("vehicle/ecus/can0ecu0");
            ecuNames.put("vehicle/ecus/can0ecu295");
            ecuObj.put("ecus", ecuNames);
        } catch (JSONException e) {
            return null;
        }
        return ecuObj;
    }

    public JSONObject ecuEndpoints() {
        JSONObject endpointsObj = new JSONObject();
        JSONArray endpointsArr = new JSONArray();
        try {
            endpointsArr.put("name");
            endpointsArr.put("VIN");
            endpointsArr.put("make");
            endpointsArr.put("model");
            endpointsObj.put("can0ecu0", endpointsArr);
        } catch (JSONException e) {
            return null;
        }
        return endpointsObj;
    }

    public JSONObject ecuEndpoint() {
        JSONObject endpointObj = new JSONObject();
        try {
            endpointObj.put("name", "Indigo Montoya");
        } catch (JSONException e) {
            return null;
        }
        return endpointObj;
    }

    public JSONObject junk() {
        JSONObject obj = new JSONObject();
        JSONArray arr = new JSONArray();
        try {
            arr.put("elephantSoup").put(true).put(new Object());
            obj.put("this is invalid data", arr);
        } catch (JSONException e) {
            return null;
        }

        return obj;
    }
}
