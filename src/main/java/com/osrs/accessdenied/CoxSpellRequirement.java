package com.osrs.accessdenied;

/**
 * Mutually exclusive Chambers of Xeric spell requirement.
 *
 * <p>Thralls and Death Charge live on the Arceuus spellbook while Humidify and Vengeance
 * live on Lunar, so a CoX setup can only ever sit on one spellbook at a time. Modelling the
 * choice as a single value (rendered as a dropdown) makes the cross-spellbook conflict
 * impossible to express, so the config never has to be programmatically corrected — which in
 * turn avoids the stale-checkbox lag of RuneLite's config panel, which does not refresh on
 * programmatic config changes.
 */
public enum CoxSpellRequirement
{
	NONE("None", false, false, false, false),
	THRALLS("Thralls", true, false, false, false),
	DEATH_CHARGE("Death Charge", false, true, false, false),
	THRALLS_AND_DEATH_CHARGE("Thralls + Death Charge", true, true, false, false),
	HUMIDIFY("Humidify", false, false, true, false),
	VENGEANCE("Vengeance", false, false, false, true),
	HUMIDIFY_AND_VENGEANCE("Humidify + Vengeance", false, false, true, true);

	private final String displayName;
	private final boolean thralls;
	private final boolean deathCharge;
	private final boolean humidify;
	private final boolean vengeance;

	CoxSpellRequirement(String displayName, boolean thralls, boolean deathCharge, boolean humidify, boolean vengeance)
	{
		this.displayName = displayName;
		this.thralls = thralls;
		this.deathCharge = deathCharge;
		this.humidify = humidify;
		this.vengeance = vengeance;
	}

	public boolean requiresThralls()
	{
		return thralls;
	}

	public boolean requiresDeathCharge()
	{
		return deathCharge;
	}

	public boolean requiresHumidify()
	{
		return humidify;
	}

	public boolean requiresVengeance()
	{
		return vengeance;
	}

	public boolean requiresArceuus()
	{
		return thralls || deathCharge;
	}

	public boolean requiresLunar()
	{
		return humidify || vengeance;
	}

	/**
	 * @return the label shown in the config dropdown
	 */
	@Override
	public String toString()
	{
		return displayName;
	}

	/**
	 * Maps the four legacy boolean toggles (coxRequireSpell/coxRequireDeathCharge/
	 * coxRequireHumidify/coxRequireVengeance) to the equivalent enum value, for migrating
	 * configs saved before this enum existed.
	 *
	 * @return the matching value, or {@link #NONE} if the flags don't correspond to any
	 * value (e.g. a cross-spellbook combination that should never have been saved)
	 */
	public static CoxSpellRequirement fromLegacyFlags(boolean thralls, boolean deathCharge, boolean humidify, boolean vengeance)
	{
		for (CoxSpellRequirement requirement : values())
		{
			if (requirement.thralls == thralls && requirement.deathCharge == deathCharge
				&& requirement.humidify == humidify && requirement.vengeance == vengeance)
			{
				return requirement;
			}
		}
		return NONE;
	}
}
