/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Created by awickert on 6/13/13.
 */
public class WvaUtil {
    private static final DateTimeFormatter formatMillis = ISODateTimeFormat.dateTime();
    private static final DateTimeFormatter format = ISODateTimeFormat.dateTimeNoMillis();


    public static DateTime dateTimeFromString(String timestamp) {
        try {
            return format.parseDateTime(timestamp);
        } catch (IllegalArgumentException e) {
            // Real WVA sends timestamps without milliseconds. If not connected
            // to "real" WVA (i.e. spoofer), we might be sending out timestamps
            // with milliseconds. In that case, parse it out here.
           return formatMillis.parseDateTime(timestamp);
        }
    }
}
