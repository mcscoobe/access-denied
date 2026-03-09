package com.osrs.accessdenied;

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
		keyName = "nexEnabled",
		name = "Enable Validation",
		description = "Enable validation for Nex (master toggle). At least one requirement below must be enabled for validation to work.",
		section = nexSection,
		position = 0
	)
	default boolean nexEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "nexRequireSpell",
		name = "Require Resurrect Greater Ghost",
		description = "Require the ability to cast Resurrect Greater Ghost spell. Checks for: 4 Soul runes, 2 Blood runes, 1 Cosmic rune, Book of the Dead, and Arceuus spellbook. Aether runes can substitute for Soul/Cosmic runes.",
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
		description = "Require the ability to cast Death Charge spell. Checks for: 1 Death rune, 1 Blood rune, 1 Soul rune, and Arceuus spellbook. Aether runes can substitute for Soul runes.",
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
		keyName = "tobEnabled",
		name = "Enable Validation",
		description = "Enable validation for Theatre of Blood (master toggle). At least one requirement below must be enabled for validation to work.",
		section = tobSection,
		position = 0
	)
	default boolean tobEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tobRequireSpell",
		name = "Require Resurrect Greater Ghost",
		description = "Require the ability to cast Resurrect Greater Ghost spell. Checks for: 4 Soul runes, 2 Blood runes, 1 Cosmic rune, Book of the Dead, and Arceuus spellbook. Aether runes can substitute for Soul/Cosmic runes.",
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
		description = "Require the ability to cast Death Charge spell. Checks for: 1 Death rune, 1 Blood rune, 1 Soul rune, and Arceuus spellbook. Aether runes can substitute for Soul runes.",
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
		keyName = "toaEnabled",
		name = "Enable Validation",
		description = "Enable validation for Tombs of Amascut (master toggle). At least one requirement below must be enabled for validation to work.",
		section = toaSection,
		position = 0
	)
	default boolean toaEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "toaRequireSpell",
		name = "Require Resurrect Greater Ghost",
		description = "Require the ability to cast Resurrect Greater Ghost spell. Checks for: 4 Soul runes, 2 Blood runes, 1 Cosmic rune, Book of the Dead, and Arceuus spellbook. Aether runes can substitute for Soul/Cosmic runes.",
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
		description = "Require the ability to cast Death Charge spell. Checks for: 1 Death rune, 1 Blood rune, 1 Soul rune, and Arceuus spellbook. Aether runes can substitute for Soul runes.",
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
		keyName = "coxEnabled",
		name = "Enable Validation",
		description = "Enable validation for Chambers of Xeric (master toggle). At least one requirement below must be enabled for validation to work.",
		section = coxSection,
		position = 0
	)
	default boolean coxEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "coxRequireSpell",
		name = "Require Resurrect Greater Ghost",
		description = "Require the ability to cast Resurrect Greater Ghost spell. Checks for: 4 Soul runes, 2 Blood runes, 1 Cosmic rune, Book of the Dead, and Arceuus spellbook. Aether runes can substitute for Soul/Cosmic runes.",
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
		description = "Require the ability to cast Death Charge spell. Checks for: 1 Death rune, 1 Blood rune, 1 Soul rune, and Arceuus spellbook. Aether runes can substitute for Soul runes.",
		section = coxSection,
		position = 2
	)
	default boolean coxRequireDeathCharge()
	{
		return false;
	}
}
