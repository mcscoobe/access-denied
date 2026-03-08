package com.osrs.accessdenied;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import static org.junit.Assert.*;

public class BossLocationTest
{
	@Test
	public void testBossLocationCreation()
	{
		BossLocation location = new BossLocation("test", "Test Location", Collections.singleton(12345));
		
		assertEquals("test", location.getId());
		assertEquals("Test Location", location.getDisplayName());
		assertTrue(location.getRegionIds().contains(12345));
	}

	@Test
	public void testIsInRegion()
	{
		BossLocation location = new BossLocation("test", "Test", Collections.singleton(12345));
		
		assertTrue(location.isInRegion(12345));
		assertFalse(location.isInRegion(99999));
	}

	@Test
	public void testIsInRegionWithMultipleRegions()
	{
		BossLocation location = new BossLocation(
			"test", 
			"Test", 
			new HashSet<>(Arrays.asList(12345, 67890))
		);
		
		assertTrue(location.isInRegion(12345));
		assertTrue(location.isInRegion(67890));
		assertFalse(location.isInRegion(99999));
	}

	@Test
	public void testIsInAnyRegion()
	{
		BossLocation location = new BossLocation("test", "Test", Collections.singleton(12345));
		
		int[] regions = {11111, 12345, 22222};
		assertTrue(location.isInAnyRegion(regions));
		
		int[] noMatchRegions = {11111, 22222};
		assertFalse(location.isInAnyRegion(noMatchRegions));
		
		assertFalse(location.isInAnyRegion(null));
	}

	@Test
	public void testIsInAnyRegionWithMultipleLocationRegions()
	{
		BossLocation location = new BossLocation(
			"test", 
			"Test", 
			new HashSet<>(Arrays.asList(12345, 67890))
		);
		
		int[] regionsMatchFirst = {11111, 12345};
		assertTrue(location.isInAnyRegion(regionsMatchFirst));
		
		int[] regionsMatchSecond = {67890, 99999};
		assertTrue(location.isInAnyRegion(regionsMatchSecond));
		
		int[] regionsMatchBoth = {12345, 67890};
		assertTrue(location.isInAnyRegion(regionsMatchBoth));
		
		int[] noMatch = {11111, 99999};
		assertFalse(location.isInAnyRegion(noMatch));
	}
}
