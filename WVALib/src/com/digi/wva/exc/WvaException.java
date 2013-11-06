/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2013 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.wva.exc;

@SuppressWarnings("UnusedDeclaration")
public class WvaException extends Exception {
	private static final long serialVersionUID = 8694550164902360640L;

	public WvaException() {
	}

	public WvaException(String detailMessage) {
		super(detailMessage);
	}

	public WvaException(Throwable throwable) {
		super(throwable);
	}

	public WvaException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
