/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.async;

import java.util.HashSet;
import java.util.Set;

/**
 * Attaches to data endpoints that are updated repeatedly
 * @author awickert
 */
public abstract class WvaListener {
    private Set<String> shortNames = new HashSet<String>();

	public void onUpdate(String endpoint, VehicleResponse response) {}

    public final boolean containsName(String shortName) {
        return shortNames.contains(shortName);
    }

    public final void putName(String shortName) {
        shortNames.add(shortName);
    }

    public final void deleteName(String shortName) {
        shortNames.remove(shortName);
    }
}
