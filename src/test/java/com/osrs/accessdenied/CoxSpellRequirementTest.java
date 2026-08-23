package com.osrs.accessdenied;

import net.jqwik.api.Example;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the CoxSpellRequirement enum.
 * Verifies the per-spell flags, the Arceuus/Lunar groupings, the dropdown labels,
 * and the core invariant that no value requires both spellbooks at once.
 */
class CoxSpellRequirementTest
{
	@Example
	void noneRequiresNothing()
	{
		CoxSpellRequirement none = CoxSpellRequirement.NONE;
		assertThat(none.requiresThralls()).isFalse();
		assertThat(none.requiresDeathCharge()).isFalse();
		assertThat(none.requiresHumidify()).isFalse();
		assertThat(none.requiresVengeance()).isFalse();
		assertThat(none.requiresArceuus()).isFalse();
		assertThat(none.requiresLunar()).isFalse();
	}

	@Example
	void thrallsRequiresOnlyThrallsOnArceuus()
	{
		CoxSpellRequirement thralls = CoxSpellRequirement.THRALLS;
		assertThat(thralls.requiresThralls()).isTrue();
		assertThat(thralls.requiresDeathCharge()).isFalse();
		assertThat(thralls.requiresArceuus()).isTrue();
		assertThat(thralls.requiresLunar()).isFalse();
	}

	@Example
	void deathChargeRequiresOnlyDeathChargeOnArceuus()
	{
		CoxSpellRequirement dc = CoxSpellRequirement.DEATH_CHARGE;
		assertThat(dc.requiresDeathCharge()).isTrue();
		assertThat(dc.requiresThralls()).isFalse();
		assertThat(dc.requiresArceuus()).isTrue();
		assertThat(dc.requiresLunar()).isFalse();
	}

	@Example
	void thrallsAndDeathChargeRequiresBothArceuusSpells()
	{
		CoxSpellRequirement combo = CoxSpellRequirement.THRALLS_AND_DEATH_CHARGE;
		assertThat(combo.requiresThralls()).isTrue();
		assertThat(combo.requiresDeathCharge()).isTrue();
		assertThat(combo.requiresArceuus()).isTrue();
		assertThat(combo.requiresLunar()).isFalse();
	}

	@Example
	void humidifyRequiresOnlyHumidifyOnLunar()
	{
		CoxSpellRequirement humidify = CoxSpellRequirement.HUMIDIFY;
		assertThat(humidify.requiresHumidify()).isTrue();
		assertThat(humidify.requiresVengeance()).isFalse();
		assertThat(humidify.requiresLunar()).isTrue();
		assertThat(humidify.requiresArceuus()).isFalse();
	}

	@Example
	void vengeanceRequiresOnlyVengeanceOnLunar()
	{
		CoxSpellRequirement veng = CoxSpellRequirement.VENGEANCE;
		assertThat(veng.requiresVengeance()).isTrue();
		assertThat(veng.requiresHumidify()).isFalse();
		assertThat(veng.requiresLunar()).isTrue();
		assertThat(veng.requiresArceuus()).isFalse();
	}

	@Example
	void humidifyAndVengeanceRequiresBothLunarSpells()
	{
		CoxSpellRequirement combo = CoxSpellRequirement.HUMIDIFY_AND_VENGEANCE;
		assertThat(combo.requiresHumidify()).isTrue();
		assertThat(combo.requiresVengeance()).isTrue();
		assertThat(combo.requiresLunar()).isTrue();
		assertThat(combo.requiresArceuus()).isFalse();
	}

	/**
	 * The invariant the whole redesign relies on: a single value can never demand spells
	 * from both spellbooks, so an unsatisfiable cross-spellbook conflict cannot be expressed.
	 */
	@Example
	void noValueRequiresBothSpellbooks()
	{
		for (CoxSpellRequirement value : CoxSpellRequirement.values())
		{
			assertThat(value.requiresArceuus() && value.requiresLunar())
				.as(value + " must not require both Arceuus and Lunar spellbooks").isFalse();
		}
	}

	@Example
	void displayNamesAreHumanReadable()
	{
		assertThat(CoxSpellRequirement.NONE.toString()).isEqualTo("None");
		assertThat(CoxSpellRequirement.THRALLS.toString()).isEqualTo("Thralls");
		assertThat(CoxSpellRequirement.DEATH_CHARGE.toString()).isEqualTo("Death Charge");
		assertThat(CoxSpellRequirement.THRALLS_AND_DEATH_CHARGE.toString()).isEqualTo("Thralls + Death Charge");
		assertThat(CoxSpellRequirement.HUMIDIFY.toString()).isEqualTo("Humidify");
		assertThat(CoxSpellRequirement.VENGEANCE.toString()).isEqualTo("Vengeance");
		assertThat(CoxSpellRequirement.HUMIDIFY_AND_VENGEANCE.toString()).isEqualTo("Humidify + Vengeance");
	}
}
