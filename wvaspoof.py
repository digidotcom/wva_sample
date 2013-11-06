#!/usr/bin/env python
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
# the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
# Copyright (c) 2013 Digi International Inc., All Rights Reserved.

from datetime import datetime
import json
import random

from flask import Flask
from flask import request
from flask import Response
app = Flask(__name__)

subs = {'sally': {'uri': 'vehicle/data/Temperature',
                  'buffer': 'queue',
                  'interval': 10 }}
alarm_names = []
ecu_refs = ['can0ecu0',
        'can0ecu1',
        'can1ecu0' ]

data_stuff = {'EngineRPM': 'int',
        'Ignition_State': 'string',
        'VehicleSpeed': 'int',
        'Elapsed_Time': 'int'}

ecu_stuff = {'VIN': 'string',
        'Price': 'int',
        'Previous_Owner': 'string'}

#trailingslash = '/'
trailingslash = ""


def resp(resp_str):
    return Response(resp_str, mimetype='application/json')


@app.route('/')
def index():
    pass

@app.route('/ws' + trailingslash)
def ws():
    return resp(json.dumps({
        'ws': ['vehicle', 'config', 'hw']}))

@app.route('/ws/<endpoint>' + trailingslash)
def ws_endpoint(endpoint):
    if endpoint == 'vehicle':
        return resp(json.dumps(
            {'vehicle': ['vehicle/ecus', 'vehicle/data',]}))
    elif endpoint == 'hw':
        return resp(json.dumps({
            'hw': ['hw/buttons', 'hw/leds', 'hw/time']}))
    elif endpoint == 'subscriptions':
        return resp(json.dumps({
            'subscriptions': ['subscriptions/{0}'.format(sn) for sn in subs]}))
    elif endpoint == 'alarms':
        return resp(json.dumps({
            'alarms': ['alarms/{0}'.format(sn) for sn in alarm_names]}))
    else:
        return resp(None)

@app.route('/ws/vehicle/<endpoint>' + trailingslash)
def vehicle_endpoint(endpoint):
    if endpoint == 'data' or endpoint == "data/":
        return resp(json.dumps({
            'data': ['vehicle/data/{0}'.format(item) for item in data_stuff.keys()]}))

    elif endpoint == 'ecus':
        return resp(json.dumps({
            'data': ['vehicle/ecus/{0}'.format(item) for item in ecu_refs]}))
    else:
        return resp(None)

@app.route('/ws/vehicle/data/', defaults={'endpoint': ""})
@app.route('/ws/vehicle/data/<endpoint>' + trailingslash)
def data_endpoint(endpoint):
    if endpoint in data_stuff:
        if data_stuff[endpoint] == 'string':
            value = 'string' + str(random.randint(1,1000))
        else:
            value = random.randint(1,1000);
        return resp(json.dumps({
            endpoint: {'timestamp': datetime.isoformat(datetime.now()),
                       'value': value}}))
    elif endpoint == "":
        return vehicle_endpoint('data')
    else:
        return resp(None)

@app.route('/ws/vehicle/ecus/<endpoint>' + trailingslash)
def ecus_plural_endpoint(endpoint):
    if endpoint in ecu_refs:
        return resp(json.dumps({
            endpoint: ['vehicle/ecus/{0}/{1}'.format(endpoint, item)
                        for item in ecu_stuff]}))

@app.route('/ws/vehicle/ecus/<this_ecu>/<endpoint>' + trailingslash)
def singular_ecu_endpoint(this_ecu, endpoint):
    if this_ecu in ecu_refs and endpoint in ecu_stuff:
        if ecu_stuff[endpoint] == 'string':
            value = 'string' + str(random.randint(1,1000))
        else:
            value = random.randint(1,1000);
        return resp(json.dumps({ endpoint: value}))
    else:
        return resp(None)

@app.route('/ws/subscriptions/<endpoint>' + trailingslash,
        methods = ['GET', 'PUT', 'DELETE'])
def subscriptions_endpoint(endpoint):
    if request.method == 'GET' and endpoint in subs:
        return resp(json.dumps({ 'subscription': subs[endpoint]}))
    elif request.method == 'PUT':
        subs[endpoint] = request.args
        return resp(json.dumps(request.args))
    elif request.method == 'DELETE' and endpoint in subs:
        ret = resp(json.dumps({ 'subscription': subs[endpoint]}))
        del subs[endpoint]
        return ret
    else:
        return resp(None)

@app.route('/ws/alarms/<alarmname>' + trailingslash,
            methods=['PUT'])
def put_alarm(alarmname):
    return resp(alarmname)

if __name__=='__main__':
    app.debug=True;
    app.run(host='0.0.0.0', port=80)
