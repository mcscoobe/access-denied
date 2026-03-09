package com.osrs.accessdenied;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for BossLocations registry class.
 */
public class BossLocationsTest
{
	@Test
	public void testAllLocationsContainsExpectedLocations()
	{
		assertEquals(5, BossLocations.ALL_LOCATIONS.size());
		assertTrue(BossLocations.ALL_LOCATIONS.contains(BossLocations.NEX));
		assertTrue(BossLocations.ALL_LOCATIONS.contains(BossLocations.THEATRE_OF_BLOOD));
		assertTrue(BossLocations.ALL_LOCATIONS.contains(BossLocations.TOMBS_OF_AMASCUT));
		assertTrue(BossLocations.ALL_LOCATIONS.contains(BossLocations.CHAMBERS_OF_XERIC));
		assertTrue(BossLocations.ALL_LOCATIONS.contains(BossLocations.INFERNO));
	}

	@Test
	public void testNexLocationProperties()
	{
		assertEquals("nex", BossLocations.NEX.getId());
		assertEquals("Nex", BossLocations.NEX.getDisplayName());
		assertTrue(BossLocations.NEX.isInRegion(11601));
	}

	@Test
	public void testTheatreOfBloodLocationProperties()
	{
		assertEquals("tob", BossLocations.THEATRE_OF_BLOOD.getId());
		assertEquals("Theatre of Blood", BossLocations.THEATRE_OF_BLOOD.getDisplayName());
		assertTrue(BossLocations.THEATRE_OF_BLOOD.isInRegion(14642));
	}

	@Test
	public void testTombsOfAmascutLocationProperties()
	{
		assertEquals("toa", BossLocations.TOMBS_OF_AMASCUT.getId());
		assertEquals("Tombs of Amascut", BossLocations.TOMBS_OF_AMASCUT.getDisplayName());
		assertTrue(BossLocations.TOMBS_OF_AMASCUT.isInRegion(13454));
	}

	@Test
	public void testChambersOfXericLocationProperties()
	{
		assertEquals("cox", BossLocations.CHAMBERS_OF_XERIC.getId());
		assertEquals("Chambers of Xeric", BossLocations.CHAMBERS_OF_XERIC.getDisplayName());
		assertTrue(BossLocations.CHAMBERS_OF_XERIC.isInRegion(13393));
		assertTrue(BossLocations.CHAMBERS_OF_XERIC.isInRegion(13137));
	}

	@Test
	public void testInfernoLocationProperties()
	{
		assertEquals("inferno", BossLocations.INFERNO.getId());
		assertEquals("Inferno", BossLocations.INFERNO.getDisplayName());
		assertTrue(BossLocations.INFERNO.isInRegion(10063));
		assertTrue(BossLocations.INFERNO.isInRegion(9807));
	}

	@Test
	public void testLocationObjectsMapping()
	{
		assertEquals(5, BossLocations.LOCATION_OBJECTS.size());
		assertEquals(Integer.valueOf(42967), BossLocations.LOCATION_OBJECTS.get("nex"));
		assertEquals(Integer.valueOf(32653), BossLocations.LOCATION_OBJECTS.get("tob"));
		assertEquals(Integer.valueOf(46089), BossLocations.LOCATION_OBJECTS.get("toa"));
		assertEquals(Integer.valueOf(29789), BossLocations.LOCATION_OBJECTS.get("cox"));
		assertEquals(Integer.valueOf(30352), BossLocations.LOCATION_OBJECTS.get("inferno"));
	}

	@Test
	public void testFindByRegion()
	{
		BossLocation nex = BossLocations.findByRegion(11601);
		assertNotNull(nex);
		assertEquals("nex", nex.getId());

		BossLocation tob = BossLocations.findByRegion(14642);
		assertNotNull(tob);
		assertEquals("tob", tob.getId());

		BossLocation toa = BossLocations.findByRegion(13454);
		assertNotNull(toa);
		assertEquals("toa", toa.getId());

		BossLocation cox = BossLocations.findByRegion(13393);
		assertNotNull(cox);
		assertEquals("cox", cox.getId());

		BossLocation inferno = BossLocations.findByRegion(10063);
		assertNotNull(inferno);
		assertEquals("inferno", inferno.getId());
	}

	@Test
	public void testFindByRegionNotFound()
	{
		BossLocation result = BossLocations.findByRegion(99999);
		assertNull(result);
	}

	@Test
	public void testFindByAnyRegion()
	{
		int[] nexRegions = {11600, 11601, 11602};
		BossLocation nex = BossLocations.findByAnyRegion(nexRegions);
		assertNotNull(nex);
		assertEquals("nex", nex.getId());

		int[] coxRegions = {13136, 13137, 13138};
		BossLocation cox = BossLocations.findByAnyRegion(coxRegions);
		assertNotNull(cox);
		assertEquals("cox", cox.getId());
	}

	@Test
	public void testFindByAnyRegionNotFound()
	{
		int[] unknownRegions = {99998, 99999};
		BossLocation result = BossLocations.findByAnyRegion(unknownRegions);
		assertNull(result);
	}

	@Test
	public void testFindByAnyRegionWithNull()
	{
		BossLocation result = BossLocations.findByAnyRegion(null);
		assertNull(result);
	}

	@Test
	public void testGetObjectForLocation()
	{
		Integer nexObject = BossLocations.getObjectForLocation(BossLocations.NEX);
		assertEquals(Integer.valueOf(42967), nexObject);

		Integer tobObject = BossLocations.getObjectForLocation(BossLocations.THEATRE_OF_BLOOD);
		assertEquals(Integer.valueOf(32653), tobObject);

		Integer toaObject = BossLocations.getObjectForLocation(BossLocations.TOMBS_OF_AMASCUT);
		assertEquals(Integer.valueOf(46089), toaObject);

		Integer coxObject = BossLocations.getObjectForLocation(BossLocations.CHAMBERS_OF_XERIC);
		assertEquals(Integer.valueOf(29789), coxObject);

		Integer infernoObject = BossLocations.getObjectForLocation(BossLocations.INFERNO);
		assertEquals(Integer.valueOf(30352), infernoObject);
	}

	@Test
	public void testGetObjectForLocationWithNull()
	{
		Integer result = BossLocations.getObjectForLocation(null);
		assertNull(result);
	}

	@Test
	public void testIsValidatedObject()
	{
		assertTrue(BossLocations.isValidatedObject(BossLocations.NEX, 42967));
		assertFalse(BossLocations.isValidatedObject(BossLocations.NEX, 42968));
		assertFalse(BossLocations.isValidatedObject(BossLocations.NEX, 99999));

		assertTrue(BossLocations.isValidatedObject(BossLocations.THEATRE_OF_BLOOD, 32653));
		assertFalse(BossLocations.isValidatedObject(BossLocations.THEATRE_OF_BLOOD, 32654));

		assertTrue(BossLocations.isValidatedObject(BossLocations.TOMBS_OF_AMASCUT, 46089));
		assertTrue(BossLocations.isValidatedObject(BossLocations.CHAMBERS_OF_XERIC, 29789));
		assertTrue(BossLocations.isValidatedObject(BossLocations.INFERNO, 30352));
	}

	@Test
	public void testIsValidatedObjectWithNull()
	{
		assertFalse(BossLocations.isValidatedObject(null, 42967));
	}

	@Test
	public void testObjectIdConstants()
	{
		assertEquals(42967, BossLocations.NEX_OBJECT);
		assertEquals(32653, BossLocations.TOB_OBJECT);
		assertEquals(46089, BossLocations.TOA_OBJECT);
		assertEquals(29789, BossLocations.COX_OBJECT);
		assertEquals(30352, BossLocations.INFERNO_OBJECT);
	}
}
