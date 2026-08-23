package com.osrs.accessdenied;

import java.util.function.Predicate;

/**
 * The boss locations the plugin guards: the map regions that identify each one, the game
 * object whose menu entry is protected, and the config accessors for its requirements.
 *
 * <p>Constant names double as the config key prefix, so {@code NEX} owns {@code nexEnabled},
 * {@code nexBanChugJug} and so on.
 */
public enum BossLocation
{
	/**
	 * Object 42967 is the barrier while the room is available; 42968 (busy) is deliberately
	 * not guarded, since entering is impossible then anyway.
	 */
	NEX("Nex", 42967, new int[]{11601},
		AccessDeniedConfig::nexEnabled,
		c -> c.nexRequireSpell() || c.nexRequireDeathCharge(),
		AccessDeniedConfig::nexBanChugJug,
		AccessDeniedConfig::nexBanSaturatedHeart,
		"Chug Jug"),

	TOB("Theatre of Blood", 32653, new int[]{14642},
		AccessDeniedConfig::tobEnabled,
		c -> c.tobRequireSpell() || c.tobRequireDeathCharge(),
		AccessDeniedConfig::tobBanChugJug,
		AccessDeniedConfig::tobBanSaturatedHeart,
		"Chug Jug"),

	TOA("Tombs of Amascut", 46089, new int[]{13454},
		AccessDeniedConfig::toaEnabled,
		c -> c.toaRequireSpell() || c.toaRequireDeathCharge(),
		AccessDeniedConfig::toaBanChugJug,
		AccessDeniedConfig::toaBanSaturatedHeart,
		"Chug Jug"),

	COX("Chambers of Xeric", 29789, new int[]{13393, 13137},
		AccessDeniedConfig::coxEnabled,
		c -> c.coxSpellRequirement() != CoxSpellRequirement.NONE,
		AccessDeniedConfig::coxBanChugJug,
		AccessDeniedConfig::coxBanSaturatedHeart,
		"Chugging Barrel"),

	INFERNO("Inferno", 30352, new int[]{10063, 9807},
		AccessDeniedConfig::infernoEnabled,
		c -> c.infernoRequireIceBarrage() || c.infernoRequireBloodBarrage(),
		AccessDeniedConfig::infernoBanChugJug,
		AccessDeniedConfig::infernoBanSaturatedHeart,
		"Chug Jug");

	/**
	 * The CoX raid reload interaction, guarded during scouting rather than on entry, so it
	 * belongs to no single location's validation.
	 */
	public static final int COX_RELOAD_OBJECT = 49999;

	private final String displayName;
	private final int objectId;
	private final int[] regions;
	private final Predicate<AccessDeniedConfig> enabled;
	private final Predicate<AccessDeniedConfig> spellRequired;
	private final Predicate<AccessDeniedConfig> bansChugJug;
	private final Predicate<AccessDeniedConfig> bansSaturatedHeart;
	private final String chugJugLabel;

	BossLocation(String displayName, int objectId, int[] regions,
		Predicate<AccessDeniedConfig> enabled,
		Predicate<AccessDeniedConfig> spellRequired,
		Predicate<AccessDeniedConfig> bansChugJug,
		Predicate<AccessDeniedConfig> bansSaturatedHeart,
		String chugJugLabel)
	{
		this.displayName = displayName;
		this.objectId = objectId;
		this.regions = regions;
		this.enabled = enabled;
		this.spellRequired = spellRequired;
		this.bansChugJug = bansChugJug;
		this.bansSaturatedHeart = bansSaturatedHeart;
		this.chugJugLabel = chugJugLabel;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public int getObjectId()
	{
		return objectId;
	}

	/**
	 * @return the config key prefix for this location, e.g. {@code "nex"} for {@code nexEnabled}
	 */
	public String getConfigPrefix()
	{
		return name().toLowerCase();
	}

	public boolean isEnabled(AccessDeniedConfig config)
	{
		return enabled.test(config);
	}

	/**
	 * @return whether anything at all is configured to be checked here — a master toggle on
	 * its own validates nothing
	 */
	public boolean hasRequirements(AccessDeniedConfig config)
	{
		return spellRequired.test(config) || bansChugJug.test(config) || bansSaturatedHeart.test(config);
	}

	public boolean requiresValidation(AccessDeniedConfig config)
	{
		return isEnabled(config) && hasRequirements(config);
	}

	public boolean bansChugJug(AccessDeniedConfig config)
	{
		return bansChugJug.test(config);
	}

	public boolean bansSaturatedHeart(AccessDeniedConfig config)
	{
		return bansSaturatedHeart.test(config);
	}

	/**
	 * @return what the Chug Jug is called here — CoX renames it to the Chugging Barrel
	 */
	public String getChugJugLabel()
	{
		return chugJugLabel;
	}

	public boolean isInRegion(int regionId)
	{
		for (int region : regions)
		{
			if (region == regionId)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the location covering any of the given map regions, or null if none do
	 */
	public static BossLocation findByRegions(int[] mapRegions)
	{
		if (mapRegions == null)
		{
			return null;
		}

		for (BossLocation location : values())
		{
			for (int region : mapRegions)
			{
				if (location.isInRegion(region))
				{
					return location;
				}
			}
		}
		return null;
	}
}
