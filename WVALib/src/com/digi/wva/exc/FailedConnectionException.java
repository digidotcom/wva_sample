/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.exc;

import java.io.IOException;

/**
 * Created by awickert on 6/10/13.
 */
public class FailedConnectionException extends IOException {

    public FailedConnectionException() {
        super();
    }

    public FailedConnectionException(String message) {
        super(message);
    }
    
    public FailedConnectionException(String message, Throwable cause) {
    	// We can't just call super(message, cause) because that constructor
    	// was added in API 9 and we must support API 8.
    	super(message);
    	initCause(cause);
    }
}
