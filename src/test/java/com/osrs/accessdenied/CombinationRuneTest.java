package com.osrs.accessdenied;

import net.jqwik.api.Example;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the CombinationRune enum.
 * Verifies item IDs, substitution sets, and lookup methods.
 */
class CombinationRuneTest
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

	@Example
	void aetherHasCorrectItemId()
	{
		assertThat(CombinationRune.AETHER.getItemId()).isEqualTo(30843);
	}

	@Example
	void lavaHasCorrectItemId()
	{
		assertThat(CombinationRune.LAVA.getItemId()).isEqualTo(4699);
	}

	@Example
	void mistHasCorrectItemId()
	{
		assertThat(CombinationRune.MIST.getItemId()).isEqualTo(4695);
	}

	@Example
	void mudHasCorrectItemId()
	{
		assertThat(CombinationRune.MUD.getItemId()).isEqualTo(4698);
	}

	@Example
	void smokeHasCorrectItemId()
	{
		assertThat(CombinationRune.SMOKE.getItemId()).isEqualTo(4697);
	}

	@Example
	void steamHasCorrectItemId()
	{
		assertThat(CombinationRune.STEAM.getItemId()).isEqualTo(4694);
	}

	// -----------------------------------------------------------------------
	// canSubstituteFor
	// -----------------------------------------------------------------------

	@Example
	void aetherSubstitutesForSoulAndCosmic()
	{
		assertThat(CombinationRune.AETHER.canSubstituteFor(SOUL_RUNE_ID)).isTrue();
		assertThat(CombinationRune.AETHER.canSubstituteFor(COSMIC_RUNE_ID)).isTrue();
		assertThat(CombinationRune.AETHER.canSubstituteFor(FIRE_RUNE_ID)).isFalse();
		assertThat(CombinationRune.AETHER.canSubstituteFor(WATER_RUNE_ID)).isFalse();
		assertThat(CombinationRune.AETHER.canSubstituteFor(EARTH_RUNE_ID)).isFalse();
		assertThat(CombinationRune.AETHER.canSubstituteFor(AIR_RUNE_ID)).isFalse();
	}

	@Example
	void lavaSubstitutesForFireAndEarth()
	{
		assertThat(CombinationRune.LAVA.canSubstituteFor(FIRE_RUNE_ID)).isTrue();
		assertThat(CombinationRune.LAVA.canSubstituteFor(EARTH_RUNE_ID)).isTrue();
		assertThat(CombinationRune.LAVA.canSubstituteFor(WATER_RUNE_ID)).isFalse();
		assertThat(CombinationRune.LAVA.canSubstituteFor(AIR_RUNE_ID)).isFalse();
		assertThat(CombinationRune.LAVA.canSubstituteFor(SOUL_RUNE_ID)).isFalse();
	}

	@Example
	void mistSubstitutesForAirAndWater()
	{
		assertThat(CombinationRune.MIST.canSubstituteFor(AIR_RUNE_ID)).isTrue();
		assertThat(CombinationRune.MIST.canSubstituteFor(WATER_RUNE_ID)).isTrue();
		assertThat(CombinationRune.MIST.canSubstituteFor(FIRE_RUNE_ID)).isFalse();
		assertThat(CombinationRune.MIST.canSubstituteFor(EARTH_RUNE_ID)).isFalse();
		assertThat(CombinationRune.MIST.canSubstituteFor(SOUL_RUNE_ID)).isFalse();
	}

	@Example
	void mudSubstitutesForEarthAndWater()
	{
		assertThat(CombinationRune.MUD.canSubstituteFor(EARTH_RUNE_ID)).isTrue();
		assertThat(CombinationRune.MUD.canSubstituteFor(WATER_RUNE_ID)).isTrue();
		assertThat(CombinationRune.MUD.canSubstituteFor(FIRE_RUNE_ID)).isFalse();
		assertThat(CombinationRune.MUD.canSubstituteFor(AIR_RUNE_ID)).isFalse();
		assertThat(CombinationRune.MUD.canSubstituteFor(SOUL_RUNE_ID)).isFalse();
	}

	@Example
	void smokeSubstitutesForAirAndFire()
	{
		assertThat(CombinationRune.SMOKE.canSubstituteFor(AIR_RUNE_ID)).isTrue();
		assertThat(CombinationRune.SMOKE.canSubstituteFor(FIRE_RUNE_ID)).isTrue();
		assertThat(CombinationRune.SMOKE.canSubstituteFor(WATER_RUNE_ID)).isFalse();
		assertThat(CombinationRune.SMOKE.canSubstituteFor(EARTH_RUNE_ID)).isFalse();
		assertThat(CombinationRune.SMOKE.canSubstituteFor(SOUL_RUNE_ID)).isFalse();
	}

	@Example
	void steamSubstitutesForFireAndWater()
	{
		assertThat(CombinationRune.STEAM.canSubstituteFor(FIRE_RUNE_ID)).isTrue();
		assertThat(CombinationRune.STEAM.canSubstituteFor(WATER_RUNE_ID)).isTrue();
		assertThat(CombinationRune.STEAM.canSubstituteFor(EARTH_RUNE_ID)).isFalse();
		assertThat(CombinationRune.STEAM.canSubstituteFor(AIR_RUNE_ID)).isFalse();
		assertThat(CombinationRune.STEAM.canSubstituteFor(SOUL_RUNE_ID)).isFalse();
	}

	// -----------------------------------------------------------------------
	// getSubstitutesForRune — reverse lookup
	// -----------------------------------------------------------------------

	@Example
	void getSubstitutesForRuneReturnsCorrectRunesForFire()
	{
		List<CombinationRune> result = CombinationRune.getSubstitutesForRune(FIRE_RUNE_ID);
		// Lava, Smoke, Steam all substitute for Fire
		assertThat(result.size()).isEqualTo(3);
		assertContains(result, CombinationRune.LAVA);
		assertContains(result, CombinationRune.SMOKE);
		assertContains(result, CombinationRune.STEAM);
	}

	@Example
	void getSubstitutesForRuneReturnsCorrectRunesForWater()
	{
		List<CombinationRune> result = CombinationRune.getSubstitutesForRune(WATER_RUNE_ID);
		// Mist, Mud, Steam all substitute for Water
		assertThat(result.size()).isEqualTo(3);
		assertContains(result, CombinationRune.MIST);
		assertContains(result, CombinationRune.MUD);
		assertContains(result, CombinationRune.STEAM);
	}

	@Example
	void getSubstitutesForRuneReturnsCorrectRunesForEarth()
	{
		List<CombinationRune> result = CombinationRune.getSubstitutesForRune(EARTH_RUNE_ID);
		// Lava and Mud substitute for Earth
		assertThat(result.size()).isEqualTo(2);
		assertContains(result, CombinationRune.LAVA);
		assertContains(result, CombinationRune.MUD);
	}

	@Example
	void getSubstitutesForRuneReturnsCorrectRunesForAir()
	{
		List<CombinationRune> result = CombinationRune.getSubstitutesForRune(AIR_RUNE_ID);
		// Mist and Smoke substitute for Air
		assertThat(result.size()).isEqualTo(2);
		assertContains(result, CombinationRune.MIST);
		assertContains(result, CombinationRune.SMOKE);
	}

	@Example
	void getSubstitutesForRuneReturnsCorrectRunesForSoul()
	{
		List<CombinationRune> result = CombinationRune.getSubstitutesForRune(SOUL_RUNE_ID);
		// Only Aether substitutes for Soul
		assertThat(result.size()).isEqualTo(1);
		assertContains(result, CombinationRune.AETHER);
	}

	@Example
	void getSubstitutesForRuneReturnsCorrectRunesForCosmic()
	{
		List<CombinationRune> result = CombinationRune.getSubstitutesForRune(COSMIC_RUNE_ID);
		// Only Aether substitutes for Cosmic
		assertThat(result.size()).isEqualTo(1);
		assertContains(result, CombinationRune.AETHER);
	}

	@Example
	void getSubstitutesForRuneReturnsEmptyForUnsubstitutableRune()
	{
		// No combination rune substitutes for Death runes
		List<CombinationRune> result = CombinationRune.getSubstitutesForRune(DEATH_RUNE_ID);
		assertThat(result.size()).isEqualTo(0);
	}

	// -----------------------------------------------------------------------
	// getSubstitutesFor
	// -----------------------------------------------------------------------

	@Example
	void getSubstitutesForReturnsUnmodifiableSet()
	{
		assertThatThrownBy(() -> CombinationRune.AETHER.getSubstitutesFor().add(999))
			.isInstanceOf(UnsupportedOperationException.class);
	}

	@Example
	void sixCombinationRunesDefined()
	{
		assertThat(CombinationRune.values().length).isEqualTo(6);
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private void assertContains(List<CombinationRune> runes, CombinationRune expected)
	{
		assertThat(runes.contains(expected)).as("Expected " + expected + " to be present in " + runes).isTrue();
	}
}
