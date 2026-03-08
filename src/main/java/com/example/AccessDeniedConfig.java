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
}
