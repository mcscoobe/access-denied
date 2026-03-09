package com.osrs.accessdenied;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for BossLocation class.
 */
public class BossLocationTest
{
	@Test
	public void testBossLocationCreation()
	{
		Set<Integer> regions = new HashSet<>(Arrays.asList(11601, 11602));
		BossLocation location = new BossLocation("test", "Test Location", regions);

		assertEquals("test", location.getId());
		assertEquals("Test Location", location.getDisplayName());
		assertEquals(2, location.getRegionIds().size());
		assertTrue(location.getRegionIds().contains(11601));
		assertTrue(location.getRegionIds().contains(11602));
	}

	@Test
	public void testIsInRegion()
	{
		Set<Integer> regions = Collections.singleton(11601);
		BossLocation location = new BossLocation("nex", "Nex", regions);

		assertTrue(location.isInRegion(11601));
		assertFalse(location.isInRegion(11602));
		assertFalse(location.isInRegion(0));
	}

	@Test
	public void testIsInRegionWithMultipleRegions()
	{
		Set<Integer> regions = new HashSet<>(Arrays.asList(13393, 13137));
		BossLocation location = new BossLocation("cox", "Chambers of Xeric", regions);

		assertTrue(location.isInRegion(13393));
		assertTrue(location.isInRegion(13137));
		assertFalse(location.isInRegion(13394));
	}

	@Test
	public void testIsInAnyRegion()
	{
		Set<Integer> regions = Collections.singleton(11601);
		BossLocation location = new BossLocation("nex", "Nex", regions);

		int[] testRegions = {11600, 11601, 11602};
		assertTrue(location.isInAnyRegion(testRegions));
	}

	@Test
	public void testIsInAnyRegionNoMatch()
	{
		Set<Integer> regions = Collections.singleton(11601);
		BossLocation location = new BossLocation("nex", "Nex", regions);

		int[] testRegions = {11600, 11602, 11603};
		assertFalse(location.isInAnyRegion(testRegions));
	}

	@Test
	public void testIsInAnyRegionWithNull()
	{
		Set<Integer> regions = Collections.singleton(11601);
		BossLocation location = new BossLocation("nex", "Nex", regions);

		assertFalse(location.isInAnyRegion(null));
	}

	@Test
	public void testIsInAnyRegionWithEmptyArray()
	{
		Set<Integer> regions = Collections.singleton(11601);
		BossLocation location = new BossLocation("nex", "Nex", regions);

		int[] emptyRegions = {};
		assertFalse(location.isInAnyRegion(emptyRegions));
	}

	@Test
	public void testIsInAnyRegionWithMultipleMatches()
	{
		Set<Integer> regions = new HashSet<>(Arrays.asList(13393, 13137));
		BossLocation location = new BossLocation("cox", "Chambers of Xeric", regions);

		int[] testRegions = {13393, 13137, 13394};
		assertTrue(location.isInAnyRegion(testRegions));
	}
}
