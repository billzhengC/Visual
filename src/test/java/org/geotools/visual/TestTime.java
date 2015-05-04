package org.geotools.visual;

import junit.framework.*;


public class TestTime extends TestCase {
	
	public void test() {
		GTFSParser timeTest = new GTFSParser();
		assertTrue(20 == timeTest.toElapsedTime("00:00:20").longValue());
		assertTrue(150 == timeTest.toElapsedTime("00:02:30").longValue());
		assertTrue(82201 == timeTest.toElapsedTime("22:50:01").longValue());
	}
}
