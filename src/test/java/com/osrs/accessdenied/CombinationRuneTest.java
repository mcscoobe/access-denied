package com.osrs.accessdenied;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the CombinationRune enum.
 * Verifies item IDs, substitution sets, and lookup methods.
 */
public class CombinationRuneTest
{
	// Standard rune IDs
	private static final int AIR_RUNE_ID = 556;
	private static final int WATER_RUNE_ID = 555;
	private static final int EARTH_RUNE_ID = 557;
	private static final int FIRE_RUNE_ID = 554;
	private static final int SOUL_RUNE_ID = 566;
	private static final int COSMIC_RUNE_ID = 564;
	private static final int DEATH_RUNE_ID = 560;

	// -----------------------------------------------------------------------
	// Item IDs
	// -----------------------------------------------------------------------

	@Test
	public void aetherHasCorrectItemId()
	{
		assertEquals(30843, CombinationRune.AETHER.getItemId());
	}

	@Test
	public void lavaHasCorrectItemId()
	{
		assertEquals(4699, CombinationRune.LAVA.getItemId());
	}

	@Test
	public void mistHasCorrectItemId()
	{
		assertEquals(4695, CombinationRune.MIST.getItemId());
	}

	@Test
	public void mudHasCorrectItemId()
	{
		assertEquals(4698, CombinationRune.MUD.getItemId());
	}

	@Test
	public void smokeHasCorrectItemId()
	{
		assertEquals(4697, CombinationRune.SMOKE.getItemId());
	}

	@Test
	public void steamHasCorrectItemId()
	{
		assertEquals(4694, CombinationRune.STEAM.getItemId());
	}

	// -----------------------------------------------------------------------
	// canSubstituteFor
	// -----------------------------------------------------------------------

	@Test
	public void aetherSubstitutesForSoulAndCosmic()
	{
		assertTrue(CombinationRune.AETHER.canSubstituteFor(SOUL_RUNE_ID));
		assertTrue(CombinationRune.AETHER.canSubstituteFor(COSMIC_RUNE_ID));
		assertFalse(CombinationRune.AETHER.canSubstituteFor(FIRE_RUNE_ID));
		assertFalse(CombinationRune.AETHER.canSubstituteFor(WATER_RUNE_ID));
		assertFalse(CombinationRune.AETHER.canSubstituteFor(EARTH_RUNE_ID));
		assertFalse(CombinationRune.AETHER.canSubstituteFor(AIR_RUNE_ID));
	}

	@Test
	public void lavaSubstitutesForFireAndEarth()
	{
		assertTrue(CombinationRune.LAVA.canSubstituteFor(FIRE_RUNE_ID));
		assertTrue(CombinationRune.LAVA.canSubstituteFor(EARTH_RUNE_ID));
		assertFalse(CombinationRune.LAVA.canSubstituteFor(WATER_RUNE_ID));
		assertFalse(CombinationRune.LAVA.canSubstituteFor(AIR_RUNE_ID));
		assertFalse(CombinationRune.LAVA.canSubstituteFor(SOUL_RUNE_ID));
	}

	@Test
	public void mistSubstitutesForAirAndWater()
	{
		assertTrue(CombinationRune.MIST.canSubstituteFor(AIR_RUNE_ID));
		assertTrue(CombinationRune.MIST.canSubstituteFor(WATER_RUNE_ID));
		assertFalse(CombinationRune.MIST.canSubstituteFor(FIRE_RUNE_ID));
		assertFalse(CombinationRune.MIST.canSubstituteFor(EARTH_RUNE_ID));
		assertFalse(CombinationRune.MIST.canSubstituteFor(SOUL_RUNE_ID));
	}

	@Test
	public void mudSubstitutesForEarthAndWater()
	{
		assertTrue(CombinationRune.MUD.canSubstituteFor(EARTH_RUNE_ID));
		assertTrue(CombinationRune.MUD.canSubstituteFor(WATER_RUNE_ID));
		assertFalse(CombinationRune.MUD.canSubstituteFor(FIRE_RUNE_ID));
		assertFalse(CombinationRune.MUD.canSubstituteFor(AIR_RUNE_ID));
		assertFalse(CombinationRune.MUD.canSubstituteFor(SOUL_RUNE_ID));
	}

	@Test
	public void smokeSubstitutesForAirAndFire()
	{
		assertTrue(CombinationRune.SMOKE.canSubstituteFor(AIR_RUNE_ID));
		assertTrue(CombinationRune.SMOKE.canSubstituteFor(FIRE_RUNE_ID));
		assertFalse(CombinationRune.SMOKE.canSubstituteFor(WATER_RUNE_ID));
		assertFalse(CombinationRune.SMOKE.canSubstituteFor(EARTH_RUNE_ID));
		assertFalse(CombinationRune.SMOKE.canSubstituteFor(SOUL_RUNE_ID));
	}

	@Test
	public void steamSubstitutesForFireAndWater()
	{
		assertTrue(CombinationRune.STEAM.canSubstituteFor(FIRE_RUNE_ID));
		assertTrue(CombinationRune.STEAM.canSubstituteFor(WATER_RUNE_ID));
		assertFalse(CombinationRune.STEAM.canSubstituteFor(EARTH_RUNE_ID));
		assertFalse(CombinationRune.STEAM.canSubstituteFor(AIR_RUNE_ID));
		assertFalse(CombinationRune.STEAM.canSubstituteFor(SOUL_RUNE_ID));
	}

	// -----------------------------------------------------------------------
	// getSubstitutesForRune — reverse lookup
	// -----------------------------------------------------------------------

	@Test
	public void getSubstitutesForRuneReturnsCorrectRunesForFire()
	{
		CombinationRune[] result = CombinationRune.getSubstitutesForRune(FIRE_RUNE_ID);
		// Lava, Smoke, Steam all substitute for Fire
		assertEquals(3, result.length);
		assertContains(result, CombinationRune.LAVA);
		assertContains(result, CombinationRune.SMOKE);
		assertContains(result, CombinationRune.STEAM);
	}

	@Test
	public void getSubstitutesForRuneReturnsCorrectRunesForWater()
	{
		CombinationRune[] result = CombinationRune.getSubstitutesForRune(WATER_RUNE_ID);
		// Mist, Mud, Steam all substitute for Water
		assertEquals(3, result.length);
		assertContains(result, CombinationRune.MIST);
		assertContains(result, CombinationRune.MUD);
		assertContains(result, CombinationRune.STEAM);
	}

	@Test
	public void getSubstitutesForRuneReturnsCorrectRunesForEarth()
	{
		CombinationRune[] result = CombinationRune.getSubstitutesForRune(EARTH_RUNE_ID);
		// Lava and Mud substitute for Earth
		assertEquals(2, result.length);
		assertContains(result, CombinationRune.LAVA);
		assertContains(result, CombinationRune.MUD);
	}

	@Test
	public void getSubstitutesForRuneReturnsCorrectRunesForAir()
	{
		CombinationRune[] result = CombinationRune.getSubstitutesForRune(AIR_RUNE_ID);
		// Mist and Smoke substitute for Air
		assertEquals(2, result.length);
		assertContains(result, CombinationRune.MIST);
		assertContains(result, CombinationRune.SMOKE);
	}

	@Test
	public void getSubstitutesForRuneReturnsCorrectRunesForSoul()
	{
		CombinationRune[] result = CombinationRune.getSubstitutesForRune(SOUL_RUNE_ID);
		// Only Aether substitutes for Soul
		assertEquals(1, result.length);
		assertContains(result, CombinationRune.AETHER);
	}

	@Test
	public void getSubstitutesForRuneReturnsCorrectRunesForCosmic()
	{
		CombinationRune[] result = CombinationRune.getSubstitutesForRune(COSMIC_RUNE_ID);
		// Only Aether substitutes for Cosmic
		assertEquals(1, result.length);
		assertContains(result, CombinationRune.AETHER);
	}

	@Test
	public void getSubstitutesForRuneReturnsEmptyForUnsubstitutableRune()
	{
		// No combination rune substitutes for Death runes
		CombinationRune[] result = CombinationRune.getSubstitutesForRune(DEATH_RUNE_ID);
		assertEquals(0, result.length);
	}

	// -----------------------------------------------------------------------
	// getSubstitutesFor
	// -----------------------------------------------------------------------

	@Test(expected = UnsupportedOperationException.class)
	public void getSubstitutesForReturnsUnmodifiableSet()
	{
		CombinationRune.AETHER.getSubstitutesFor().add(999);
	}

	@Test
	public void sixCombinationRunesDefined()
	{
		assertEquals(6, CombinationRune.values().length);
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private void assertContains(CombinationRune[] array, CombinationRune expected)
	{
		for (CombinationRune rune : array)
		{
			if (rune == expected) return;
		}
		fail("Expected " + expected + " to be present in result array");
	}
}
