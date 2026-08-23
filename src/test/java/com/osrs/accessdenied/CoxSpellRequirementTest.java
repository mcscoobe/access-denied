package com.osrs.accessdenied;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the CoxSpellRequirement enum.
 * Verifies the per-spell flags, the Arceuus/Lunar groupings, the dropdown labels,
 * and the core invariant that no value requires both spellbooks at once.
 */
public class CoxSpellRequirementTest
{
	@Test
	public void noneRequiresNothing()
	{
		CoxSpellRequirement none = CoxSpellRequirement.NONE;
		assertFalse(none.requiresThralls());
		assertFalse(none.requiresDeathCharge());
		assertFalse(none.requiresHumidify());
		assertFalse(none.requiresVengeance());
		assertFalse(none.requiresArceuus());
		assertFalse(none.requiresLunar());
	}

	@Test
	public void thrallsRequiresOnlyThrallsOnArceuus()
	{
		CoxSpellRequirement thralls = CoxSpellRequirement.THRALLS;
		assertTrue(thralls.requiresThralls());
		assertFalse(thralls.requiresDeathCharge());
		assertTrue(thralls.requiresArceuus());
		assertFalse(thralls.requiresLunar());
	}

	@Test
	public void deathChargeRequiresOnlyDeathChargeOnArceuus()
	{
		CoxSpellRequirement dc = CoxSpellRequirement.DEATH_CHARGE;
		assertTrue(dc.requiresDeathCharge());
		assertFalse(dc.requiresThralls());
		assertTrue(dc.requiresArceuus());
		assertFalse(dc.requiresLunar());
	}

	@Test
	public void thrallsAndDeathChargeRequiresBothArceuusSpells()
	{
		CoxSpellRequirement combo = CoxSpellRequirement.THRALLS_AND_DEATH_CHARGE;
		assertTrue(combo.requiresThralls());
		assertTrue(combo.requiresDeathCharge());
		assertTrue(combo.requiresArceuus());
		assertFalse(combo.requiresLunar());
	}

	@Test
	public void humidifyRequiresOnlyHumidifyOnLunar()
	{
		CoxSpellRequirement humidify = CoxSpellRequirement.HUMIDIFY;
		assertTrue(humidify.requiresHumidify());
		assertFalse(humidify.requiresVengeance());
		assertTrue(humidify.requiresLunar());
		assertFalse(humidify.requiresArceuus());
	}

	@Test
	public void vengeanceRequiresOnlyVengeanceOnLunar()
	{
		CoxSpellRequirement veng = CoxSpellRequirement.VENGEANCE;
		assertTrue(veng.requiresVengeance());
		assertFalse(veng.requiresHumidify());
		assertTrue(veng.requiresLunar());
		assertFalse(veng.requiresArceuus());
	}

	@Test
	public void humidifyAndVengeanceRequiresBothLunarSpells()
	{
		CoxSpellRequirement combo = CoxSpellRequirement.HUMIDIFY_AND_VENGEANCE;
		assertTrue(combo.requiresHumidify());
		assertTrue(combo.requiresVengeance());
		assertTrue(combo.requiresLunar());
		assertFalse(combo.requiresArceuus());
	}

	/**
	 * The invariant the whole redesign relies on: a single value can never demand spells
	 * from both spellbooks, so an unsatisfiable cross-spellbook conflict cannot be expressed.
	 */
	@Test
	public void noValueRequiresBothSpellbooks()
	{
		for (CoxSpellRequirement value : CoxSpellRequirement.values())
		{
			assertFalse(
				value + " must not require both Arceuus and Lunar spellbooks",
				value.requiresArceuus() && value.requiresLunar());
		}
	}

	@Test
	public void displayNamesAreHumanReadable()
	{
		assertEquals("None", CoxSpellRequirement.NONE.toString());
		assertEquals("Thralls", CoxSpellRequirement.THRALLS.toString());
		assertEquals("Death Charge", CoxSpellRequirement.DEATH_CHARGE.toString());
		assertEquals("Thralls + Death Charge", CoxSpellRequirement.THRALLS_AND_DEATH_CHARGE.toString());
		assertEquals("Humidify", CoxSpellRequirement.HUMIDIFY.toString());
		assertEquals("Vengeance", CoxSpellRequirement.VENGEANCE.toString());
		assertEquals("Humidify + Vengeance", CoxSpellRequirement.HUMIDIFY_AND_VENGEANCE.toString());
	}
}
