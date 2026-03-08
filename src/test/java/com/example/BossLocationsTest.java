package com.example;

import org.junit.Test;
import static org.junit.Assert.*;

public class BossLocationsTest
{
	@Test
	public void testNexLocationDefinition()
	{
		assertEquals("nex", BossLocations.NEX.getId());
		assertEquals("Nex", BossLocations.NEX.getDisplayName());
		assertTrue(BossLocations.NEX.isInRegion(11601));
		assertFalse(BossLocations.NEX.isInRegion(99999));
	}

	@Test
	public void testTheatreOfBloodLocationDefinition()
	{
		assertEquals("tob", BossLocations.THEATRE_OF_BLOOD.getId());
		assertEquals("Theatre of Blood", BossLocations.THEATRE_OF_BLOOD.getDisplayName());
		assertTrue(BossLocations.THEATRE_OF_BLOOD.isInRegion(14642));
	}

	@Test
	public void testTombsOfAmascutLocationDefinition()
	{
		assertEquals("toa", BossLocations.TOMBS_OF_AMASCUT.getId());
		assertEquals("Tombs of Amascut", BossLocations.TOMBS_OF_AMASCUT.getDisplayName());
		assertTrue(BossLocations.TOMBS_OF_AMASCUT.isInRegion(13454));
	}

	@Test
	public void testChambersOfXericLocationDefinition()
	{
		assertEquals("cox", BossLocations.CHAMBERS_OF_XERIC.getId());
		assertEquals("Chambers of Xeric", BossLocations.CHAMBERS_OF_XERIC.getDisplayName());
		assertTrue(BossLocations.CHAMBERS_OF_XERIC.isInRegion(13393));
		assertTrue(BossLocations.CHAMBERS_OF_XERIC.isInRegion(13137));
		assertFalse(BossLocations.CHAMBERS_OF_XERIC.isInRegion(99999));
	}

	@Test
	public void testFindByRegion()
	{
		assertEquals(BossLocations.NEX, BossLocations.findByRegion(11601));
		assertEquals(BossLocations.THEATRE_OF_BLOOD, BossLocations.findByRegion(14642));
		assertEquals(BossLocations.TOMBS_OF_AMASCUT, BossLocations.findByRegion(13454));
		assertEquals(BossLocations.CHAMBERS_OF_XERIC, BossLocations.findByRegion(13393));
		assertEquals(BossLocations.CHAMBERS_OF_XERIC, BossLocations.findByRegion(13137));
		assertNull(BossLocations.findByRegion(99999));
	}

	@Test
	public void testFindByAnyRegion()
	{
		int[] nexRegions = {11601};
		assertEquals(BossLocations.NEX, BossLocations.findByAnyRegion(nexRegions));

		int[] multipleRegions = {11344, 11345, 11601};
		assertEquals(BossLocations.NEX, BossLocations.findByAnyRegion(multipleRegions));

		int[] coxRegions = {13393, 13137};
		assertEquals(BossLocations.CHAMBERS_OF_XERIC, BossLocations.findByAnyRegion(coxRegions));

		int[] noMatchRegions = {99999, 88888};
		assertNull(BossLocations.findByAnyRegion(noMatchRegions));

		assertNull(BossLocations.findByAnyRegion(null));
	}

	@Test
	public void testGetObjectForLocation()
	{
		assertEquals(Integer.valueOf(42967), BossLocations.getObjectForLocation(BossLocations.NEX));
		assertEquals(Integer.valueOf(32653), BossLocations.getObjectForLocation(BossLocations.THEATRE_OF_BLOOD));
		assertEquals(Integer.valueOf(46089), BossLocations.getObjectForLocation(BossLocations.TOMBS_OF_AMASCUT));
		assertEquals(Integer.valueOf(29789), BossLocations.getObjectForLocation(BossLocations.CHAMBERS_OF_XERIC));
		assertNull(BossLocations.getObjectForLocation(null));
	}

	@Test
	public void testIsValidatedObject()
	{
		assertTrue(BossLocations.isValidatedObject(BossLocations.NEX, 42967));
		assertFalse(BossLocations.isValidatedObject(BossLocations.NEX, 42968));
		assertFalse(BossLocations.isValidatedObject(BossLocations.NEX, 99999));

		assertTrue(BossLocations.isValidatedObject(BossLocations.THEATRE_OF_BLOOD, 32653));
		assertFalse(BossLocations.isValidatedObject(BossLocations.THEATRE_OF_BLOOD, 99999));

		assertTrue(BossLocations.isValidatedObject(BossLocations.TOMBS_OF_AMASCUT, 46089));
		assertTrue(BossLocations.isValidatedObject(BossLocations.CHAMBERS_OF_XERIC, 29789));

		assertFalse(BossLocations.isValidatedObject(null, 42967));
	}

	@Test
	public void testAllLocationsContainsAllDefinedLocations()
	{
		assertTrue(BossLocations.ALL_LOCATIONS.contains(BossLocations.NEX));
		assertTrue(BossLocations.ALL_LOCATIONS.contains(BossLocations.THEATRE_OF_BLOOD));
		assertTrue(BossLocations.ALL_LOCATIONS.contains(BossLocations.TOMBS_OF_AMASCUT));
		assertTrue(BossLocations.ALL_LOCATIONS.contains(BossLocations.CHAMBERS_OF_XERIC));
		assertEquals(4, BossLocations.ALL_LOCATIONS.size());
	}
}
