/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.wvalib.test.auxiliary;

import com.digi.wva.async.WvaCallback;

/**
 * A WvaCallback implementation which records its error state and response
 */
public class PassFailCallback<T> implements WvaCallback<T> {

    public Boolean success = null;
    public T response = null;
    @Override
    public void onResponse(Throwable error, T response) {
        this.success  = (error == null);
        this.response = response;
    }
}
