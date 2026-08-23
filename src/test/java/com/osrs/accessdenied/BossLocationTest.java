package com.osrs.accessdenied;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the BossLocation enum: region lookup, guarded object IDs, and the
 * config-driven requirement predicates.
 */
public class BossLocationTest
{
	@Test
	public void findsEachLocationByItsRegions()
	{
		assertSame(BossLocation.NEX, BossLocation.findByRegions(new int[]{11601}));
		assertSame(BossLocation.TOB, BossLocation.findByRegions(new int[]{14642}));
		assertSame(BossLocation.TOA, BossLocation.findByRegions(new int[]{13454}));
		assertSame(BossLocation.COX, BossLocation.findByRegions(new int[]{13393}));
		assertSame(BossLocation.COX, BossLocation.findByRegions(new int[]{13137}));
		assertSame(BossLocation.INFERNO, BossLocation.findByRegions(new int[]{10063}));
		assertSame(BossLocation.INFERNO, BossLocation.findByRegions(new int[]{9807}));
	}

	@Test
	public void findsLocationAmongUnrelatedRegions()
	{
		assertSame(BossLocation.NEX, BossLocation.findByRegions(new int[]{99999, 11601, 12345}));
	}

	@Test
	public void findsNothingForUnknownOrMissingRegions()
	{
		assertNull(BossLocation.findByRegions(new int[]{99999}));
		assertNull(BossLocation.findByRegions(new int[0]));
		assertNull(BossLocation.findByRegions(null));
	}

	@Test
	public void guardsTheExpectedObjectIds()
	{
		assertEquals(42967, BossLocation.NEX.getObjectId());
		assertEquals(32653, BossLocation.TOB.getObjectId());
		assertEquals(46089, BossLocation.TOA.getObjectId());
		assertEquals(29789, BossLocation.COX.getObjectId());
		assertEquals(30352, BossLocation.INFERNO.getObjectId());
		assertEquals(49999, BossLocation.COX_RELOAD_OBJECT);
	}

	/**
	 * Config prefixes are derived from the constant names, so a rename would silently
	 * stop the "enabled but nothing configured" warning from matching its config key.
	 */
	@Test
	public void configPrefixMatchesARealConfigKey() throws Exception
	{
		for (BossLocation location : BossLocation.values())
		{
			AccessDeniedConfig.class.getMethod(location.getConfigPrefix() + "Enabled");
		}
	}

	@Test
	public void validationNeedsBothTheMasterToggleAndARequirement()
	{
		AccessDeniedConfig config = mock(AccessDeniedConfig.class);
		when(config.coxSpellRequirement()).thenReturn(CoxSpellRequirement.NONE);

		when(config.nexEnabled()).thenReturn(true);
		assertFalse("enabled with nothing configured validates nothing",
			BossLocation.NEX.requiresValidation(config));

		when(config.nexBanSaturatedHeart()).thenReturn(true);
		assertTrue(BossLocation.NEX.requiresValidation(config));

		when(config.nexEnabled()).thenReturn(false);
		assertFalse("master toggle off disables validation",
			BossLocation.NEX.requiresValidation(config));
	}

	@Test
	public void coxSpellRequirementCountsAsARequirement()
	{
		AccessDeniedConfig config = mock(AccessDeniedConfig.class);
		when(config.coxEnabled()).thenReturn(true);

		when(config.coxSpellRequirement()).thenReturn(CoxSpellRequirement.NONE);
		assertFalse(BossLocation.COX.requiresValidation(config));

		when(config.coxSpellRequirement()).thenReturn(CoxSpellRequirement.HUMIDIFY);
		assertTrue(BossLocation.COX.requiresValidation(config));
	}

	@Test
	public void coxRenamesTheChugJug()
	{
		assertEquals("Chugging Barrel", BossLocation.COX.getChugJugLabel());
		assertEquals("Chug Jug", BossLocation.NEX.getChugJugLabel());
	}
}
