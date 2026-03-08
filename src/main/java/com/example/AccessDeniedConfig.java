package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("accessdenied")
public interface AccessDeniedConfig extends Config
{
	@ConfigSection(
		name = "Nex",
		description = "Configuration for Nex boss requirements",
		position = 0
	)
	String nexSection = "nex";

	@ConfigItem(
		keyName = "nexRequireSpell",
		name = "Require Resurrect Greater Ghost",
		description = "Require the ability to cast Resurrect Greater Ghost spell (checks if you have the required runes)",
		section = nexSection,
		position = 1
	)
	default boolean nexRequireSpell()
	{
		return false;
	}

	@ConfigItem(
		keyName = "nexRequireDeathCharge",
		name = "Require Death Charge",
		description = "Require the ability to cast Death Charge spell (checks if you have the required runes)",
		section = nexSection,
		position = 2
	)
	default boolean nexRequireDeathCharge()
	{
		return false;
	}

	@ConfigSection(
		name = "Theatre of Blood",
		description = "Configuration for Theatre of Blood requirements",
		position = 1
	)
	String tobSection = "tob";

	@ConfigItem(
		keyName = "tobRequireSpell",
		name = "Require Resurrect Greater Ghost",
		description = "Require the ability to cast Resurrect Greater Ghost spell (checks if you have the required runes)",
		section = tobSection,
		position = 1
	)
	default boolean tobRequireSpell()
	{
		return false;
	}

	@ConfigItem(
		keyName = "tobRequireDeathCharge",
		name = "Require Death Charge",
		description = "Require the ability to cast Death Charge spell (checks if you have the required runes)",
		section = tobSection,
		position = 2
	)
	default boolean tobRequireDeathCharge()
	{
		return false;
	}

	@ConfigSection(
		name = "Tombs of Amascut",
		description = "Configuration for Tombs of Amascut requirements",
		position = 2
	)
	String toaSection = "toa";

	@ConfigItem(
		keyName = "toaRequireSpell",
		name = "Require Resurrect Greater Ghost",
		description = "Require the ability to cast Resurrect Greater Ghost spell (checks if you have the required runes)",
		section = toaSection,
		position = 1
	)
	default boolean toaRequireSpell()
	{
		return false;
	}

	@ConfigItem(
		keyName = "toaRequireDeathCharge",
		name = "Require Death Charge",
		description = "Require the ability to cast Death Charge spell (checks if you have the required runes)",
		section = toaSection,
		position = 2
	)
	default boolean toaRequireDeathCharge()
	{
		return false;
	}

	@ConfigSection(
		name = "Chambers of Xeric",
		description = "Configuration for Chambers of Xeric requirements",
		position = 3
	)
	String coxSection = "cox";

	@ConfigItem(
		keyName = "coxRequireSpell",
		name = "Require Resurrect Greater Ghost",
		description = "Require the ability to cast Resurrect Greater Ghost spell (checks if you have the required runes)",
		section = coxSection,
		position = 1
	)
	default boolean coxRequireSpell()
	{
		return false;
	}

	@ConfigItem(
		keyName = "coxRequireDeathCharge",
		name = "Require Death Charge",
		description = "Require the ability to cast Death Charge spell (checks if you have the required runes)",
		section = coxSection,
		position = 2
	)
	default boolean coxRequireDeathCharge()
	{
		return false;
	}
}
