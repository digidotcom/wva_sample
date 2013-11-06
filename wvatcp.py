#!/usr/bin/env python
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
# the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
# Copyright (c) 2013 Digi International Inc., All Rights Reserved.

import threading
import SocketServer
import json
from time import sleep
from datetime import datetime as d
from random import randint


# Code modified from docs.python.org/2/library/socketserver.html
def make_timestamp():
    #t = d.strftime(d.now(), "%Y-%m-%dT%H:%M:%S.%f")
    t = d.strftime(d.utcnow(), "%Y-%m-%dT%H:%M:%S")
    #return (t[:-3]) + "Z"
    return t + "Z"
    #return d.strftime(d.now(), "%Y-%m-%dT%H:%M:%S.000Z")


class ThreadedTCPRequestHandler(SocketServer.BaseRequestHandler):
    elapsed_time = 0.0
    ignition_state = 0.0
    engine_rpm = 0.0
    temperature = 0.0

    def handle(self):
        while 1:
            elapsed_time = self.elapsed_time
            now = make_timestamp()
            elapsed = {"Elapsed_Time": {"timestamp": now,
                                        "value": str(elapsed_time)},
                        "timestamp": now,
                        "uri": "vehicle/Elapsed_Time",
                        "short_name": "Elapsed_Time~sub"}
            self.request.sendall(json.dumps({"data": elapsed}) + "\r\n")
            print json.dumps(elapsed)
            sleep(0.2)
            ignition_state = self.ignition_state
            ignition = {"Ignition_State": {"timestamp": now,
                                            "value": str(ignition_state)},
                        "timestamp": now,
                        "uri": "vehicle/Ignition_State",
                        "short_name": "Ignition_State~sub"}
            self.request.sendall(json.dumps({"data": ignition}) + "\r\n")
            sleep(0.2)
            engine_rpm = self.engine_rpm
            rpm = {"EngineRPM": {"timestamp": now,
                                    "value": str(engine_rpm)},
                        "timestamp": now,
                        "uri": "vehicle/EngineRPM",
                        "short_name": "EngineRPM~sub"}
            self.request.sendall(json.dumps({"data": rpm}) + "\r\n")
            sleep(0.2)
            temperature = self.temperature
            temp = {"VehicleSpeed": {"timestamp": now,
                                    "value": str(temperature)},
                        "timestamp": now,
                        "uri": "vehicle/VehicleSpeed",
                        "short_name": "VehicleSpeed~sub"}
            self.request.sendall(json.dumps({"data": temp}) + "\r\n")
            sleep(10)

            self.elapsed_time += 5
            self.ignition_state += 1
            self.engine_rpm = randint(0, self.elapsed_time)
            self.temperature += 1

            #if (self.elapsed_time % 5) == 0:
                #d = {"MyAlarm": {"timestamp": make_timestamp(), "value": "1337"}}
                #self.request.sendall(json.dumps({"alarm": d}) + "\r\n")
        # end while loop...


class ThreadedTCPServer(SocketServer.ThreadingMixIn, SocketServer.TCPServer):
    pass

if __name__ == "__main__":
    HOST, PORT = "0.0.0.0", 5000

    server = ThreadedTCPServer((HOST, PORT), ThreadedTCPRequestHandler)
    ip, port = server.server_address

    server_thread = threading.Thread(target=server.serve_forever)
    server_thread.daemon = True
    server_thread.start()
    print "Server loop running in thread", server_thread.name

    while 1:
        # keep program alive
        sleep(1)
