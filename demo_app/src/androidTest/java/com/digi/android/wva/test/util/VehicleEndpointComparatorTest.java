/* 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright (c) 2014 Digi International Inc., All Rights Reserved. 
 */
 
package com.digi.android.wva.test.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.test.InstrumentationTestCase;

import com.digi.android.wva.util.VehicleEndpointComparator;

public class VehicleEndpointComparatorTest extends InstrumentationTestCase {
	private VehicleEndpointComparator comp;
	
	/* Pressure Pro endpoint prefix. */
	private static final String PRESSURE_PRO_PREFIX = "CTI";
	
	@Override protected void setUp() {
		comp = new VehicleEndpointComparator();
	}
	
	public void testCompareLexicographically() {
		// Check basic lexicographic sorting...
	
		// .compare returns > 0 if arg1 comes after arg2
		assertTrue(comp.compare("bcd", "abc") > 0);
		// .compare returns < 0 if arg1 comes before arg2
		assertTrue(comp.compare("abc", "bcd") < 0);
		// .compare returns 0 if they are the same
		assertEquals(0, comp.compare("abc", "abc"));
	}
	
	public void testComparePressureProLexicographically() {
		// Check that Pressure Pro endpoints are sorted properly.
		// This test case focuses on the case where both strings are
		// Pressure Pro-related.
		
		String s1, s2;
		s1 = PRESSURE_PRO_PREFIX + "abc";
		s2 = PRESSURE_PRO_PREFIX + "bcd";
	
		// .compare returns > 0 if arg1 belongs after arg2
		assertTrue(comp.compare(s2, s1) > 0);
		// .compare returns < 0 if arg1 belongs before arg2
		assertTrue(comp.compare(s1, s2) < 0);
		// .compare returns 0 if they are the same
		assertEquals(0, comp.compare(s1, s1));
	}
	
	public void testCompareMixed() {
		// Check that Pressure Pro endpoints are pushed to the right
		// hand side when sorting.
		
		String ppString = PRESSURE_PRO_PREFIX + "test";
		
		// .compare returns > 0 if first arg belongs after second arg
		assertTrue(comp.compare(ppString, "test") > 0);
		assertTrue(comp.compare(ppString, "another test") > 0);
		// .compare returns < 0 if first arg belongs before second arg
		assertTrue(comp.compare("test", ppString) < 0);
		assertTrue(comp.compare("another test", ppString) < 0);
	}
	
	public void testCapitalization() {
		assertTrue(comp.compare(PRESSURE_PRO_PREFIX + "a", "CTia") > 0);
	}
	
	public void testAsSortedList() {
		// Test the static .asSortedList method
		Set<String> set = new HashSet<String>();
		set.add("VehicleSpeed");
		set.add("EngineRPM");
		set.add(PRESSURE_PRO_PREFIX);
		set.add(PRESSURE_PRO_PREFIX + "abc");
		set.add(PRESSURE_PRO_PREFIX + "Xyz");
		set.add("CTixNotPressurePro");
		
		List<String> sorted = VehicleEndpointComparator.asSortedList(set);
		
		String[] expected = new String[] {
				"CTixNotPressurePro", "EngineRPM", "VehicleSpeed",
				PRESSURE_PRO_PREFIX,
				PRESSURE_PRO_PREFIX + "Xyz",
				PRESSURE_PRO_PREFIX + "abc",
		};
		
		for (int i = 0; i < sorted.size(); i++) {
			assertEquals("Sort mismatch!", expected[i], sorted.get(i));
		}
	}
}