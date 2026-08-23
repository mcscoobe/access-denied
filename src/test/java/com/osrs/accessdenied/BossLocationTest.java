package com.osrs.accessdenied;

import net.jqwik.api.Example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the BossLocation enum: region lookup, guarded object IDs, and the
 * config-driven requirement predicates.
 */
class BossLocationTest
{
	@Example
	void findsEachLocationByItsRegions()
	{
		assertThat(BossLocation.findByRegions(new int[]{11601})).isSameAs(BossLocation.NEX);
		assertThat(BossLocation.findByRegions(new int[]{14642})).isSameAs(BossLocation.TOB);
		assertThat(BossLocation.findByRegions(new int[]{13454})).isSameAs(BossLocation.TOA);
		assertThat(BossLocation.findByRegions(new int[]{13393})).isSameAs(BossLocation.COX);
		assertThat(BossLocation.findByRegions(new int[]{13137})).isSameAs(BossLocation.COX);
		assertThat(BossLocation.findByRegions(new int[]{10063})).isSameAs(BossLocation.INFERNO);
		assertThat(BossLocation.findByRegions(new int[]{9807})).isSameAs(BossLocation.INFERNO);
	}

	@Example
	void findsLocationAmongUnrelatedRegions()
	{
		assertThat(BossLocation.findByRegions(new int[]{99999, 11601, 12345})).isSameAs(BossLocation.NEX);
	}

	@Example
	void findsNothingForUnknownOrMissingRegions()
	{
		assertThat(BossLocation.findByRegions(new int[]{99999})).isNull();
		assertThat(BossLocation.findByRegions(new int[0])).isNull();
		assertThat(BossLocation.findByRegions(null)).isNull();
	}

	@Example
	void guardsTheExpectedObjectIds()
	{
		assertThat(BossLocation.NEX.getObjectId()).isEqualTo(42967);
		assertThat(BossLocation.TOB.getObjectId()).isEqualTo(32653);
		assertThat(BossLocation.TOA.getObjectId()).isEqualTo(46089);
		assertThat(BossLocation.COX.getObjectId()).isEqualTo(29789);
		assertThat(BossLocation.INFERNO.getObjectId()).isEqualTo(30352);
		assertThat(BossLocation.COX_RELOAD_OBJECT).isEqualTo(49999);
	}

	/**
	 * Config prefixes are derived from the constant names, so a rename would silently
	 * stop the "enabled but nothing configured" warning from matching its config key.
	 */
	@Example
	void configPrefixMatchesARealConfigKey() throws Exception
	{
		for (BossLocation location : BossLocation.values())
		{
			AccessDeniedConfig.class.getMethod(location.getConfigPrefix() + "Enabled");
		}
	}

	@Example
	void validationNeedsBothTheMasterToggleAndARequirement()
	{
		AccessDeniedConfig config = mock(AccessDeniedConfig.class);
		when(config.coxSpellRequirement()).thenReturn(CoxSpellRequirement.NONE);

		when(config.nexEnabled()).thenReturn(true);
		assertThat(BossLocation.NEX.requiresValidation(config))
			.as("enabled with nothing configured validates nothing").isFalse();

		when(config.nexBanSaturatedHeart()).thenReturn(true);
		assertThat(BossLocation.NEX.requiresValidation(config)).isTrue();

		when(config.nexEnabled()).thenReturn(false);
		assertThat(BossLocation.NEX.requiresValidation(config)).as("master toggle off disables validation").isFalse();
	}

	@Example
	void coxSpellRequirementCountsAsARequirement()
	{
		AccessDeniedConfig config = mock(AccessDeniedConfig.class);
		when(config.coxEnabled()).thenReturn(true);

		when(config.coxSpellRequirement()).thenReturn(CoxSpellRequirement.NONE);
		assertThat(BossLocation.COX.requiresValidation(config)).isFalse();

		when(config.coxSpellRequirement()).thenReturn(CoxSpellRequirement.HUMIDIFY);
		assertThat(BossLocation.COX.requiresValidation(config)).isTrue();
	}

	@Example
	void coxRenamesTheChugJug()
	{
		assertThat(BossLocation.COX.getChugJugLabel()).isEqualTo("Chugging Barrel");
		assertThat(BossLocation.NEX.getChugJugLabel()).isEqualTo("Chug Jug");
	}
}
