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
		name = "Require Thralls",
		description = "Require the ability to cast Thralls spell. Checks for: 4 Soul runes, 2 Blood runes, 1 Cosmic rune, Book of the Dead, and Arceuus spellbook. Aether runes can substitute for Soul/Cosmic runes.",
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

	@ConfigItem(
		keyName = "nexBanChugJug",
		name = "Disallow Chug Jug",
		description = "Fail validation if a Chug Jug is found in your inventory.",
		section = nexSection,
		position = 3
	)
	default boolean nexBanChugJug()
	{
		return false;
	}

	@ConfigItem(
		keyName = "nexBanSaturatedHeart",
		name = "Disallow Saturated Heart",
		description = "Fail validation if a Saturated Heart is found in your inventory.",
		section = nexSection,
		position = 4
	)
	default boolean nexBanSaturatedHeart()
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
		name = "Require Thralls",
		description = "Require the ability to cast Thralls spell. Checks for: 4 Soul runes, 2 Blood runes, 1 Cosmic rune, Book of the Dead, and Arceuus spellbook. Aether runes can substitute for Soul/Cosmic runes.",
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

	@ConfigItem(
		keyName = "tobBanChugJug",
		name = "Disallow Chug Jug",
		description = "Fail validation if a Chug Jug is found in your inventory.",
		section = tobSection,
		position = 3
	)
	default boolean tobBanChugJug()
	{
		return false;
	}

	@ConfigItem(
		keyName = "tobBanSaturatedHeart",
		name = "Disallow Saturated Heart",
		description = "Fail validation if a Saturated Heart is found in your inventory.",
		section = tobSection,
		position = 4
	)
	default boolean tobBanSaturatedHeart()
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
		name = "Require Thralls",
		description = "Require the ability to cast Thralls spell. Checks for: 4 Soul runes, 2 Blood runes, 1 Cosmic rune, Book of the Dead, and Arceuus spellbook. Aether runes can substitute for Soul/Cosmic runes.",
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

	@ConfigItem(
		keyName = "toaBanChugJug",
		name = "Disallow Chug Jug",
		description = "Fail validation if a Chug Jug is found in your inventory.",
		section = toaSection,
		position = 3
	)
	default boolean toaBanChugJug()
	{
		return false;
	}

	@ConfigItem(
		keyName = "toaBanSaturatedHeart",
		name = "Disallow Saturated Heart",
		description = "Fail validation if a Saturated Heart is found in your inventory.",
		section = toaSection,
		position = 4
	)
	default boolean toaBanSaturatedHeart()
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
		name = "Require Thralls",
		description = "Require the ability to cast Thralls spell. Checks for: 4 Soul runes, 2 Blood runes, 1 Cosmic rune, Book of the Dead, and Arceuus spellbook. Aether runes can substitute for Soul/Cosmic runes.",
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

	@ConfigItem(
		keyName = "coxRequireHumidify",
		name = "Require Humidify",
		description = "Require the ability to cast Humidify spell. Checks for: 1 Astral rune, 1 Fire rune, 1 Water rune, and Lunar spellbook.",
		section = coxSection,
		position = 3
	)
	default boolean coxRequireHumidify()
	{
		return false;
	}

	@ConfigItem(
		keyName = "coxRequireVengeance",
		name = "Require Vengeance",
		description = "Require the ability to cast Vengeance spell. Checks for: 10 Earth runes, 4 Astral runes, 2 Death runes, and Lunar spellbook.",
		section = coxSection,
		position = 4
	)
	default boolean coxRequireVengeance()
	{
		return false;
	}

	@ConfigItem(
		keyName = "coxBanChugJug",
		name = "Disallow Chug Jug",
		description = "Fail validation if a Chug Jug is found in your inventory.",
		section = coxSection,
		position = 5
	)
	default boolean coxBanChugJug()
	{
		return false;
	}

	@ConfigItem(
		keyName = "coxBanSaturatedHeart",
		name = "Disallow Saturated Heart",
		description = "Fail validation if a Saturated Heart is found in your inventory.",
		section = coxSection,
		position = 6
	)
	default boolean coxBanSaturatedHeart()
	{
		return false;
	}

	@ConfigSection(
		name = "Inferno",
		description = "Configuration for Inferno requirements",
		position = 4
	)
	String infernoSection = "inferno";

	@ConfigItem(
		keyName = "infernoEnabled",
		name = "Enable Validation",
		description = "Enable validation for Inferno (master toggle). At least one requirement below must be enabled for validation to work.",
		section = infernoSection,
		position = 0
	)
	default boolean infernoEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "infernoRequireIceBarrage",
		name = "Require Ice Barrage",
		description = "Require the ability to cast Ice Barrage spell. Checks for: 6 Water runes, 2 Death runes, 4 Blood runes, and Ancient spellbook. Kodai wand provides infinite water runes.",
		section = infernoSection,
		position = 1
	)
	default boolean infernoRequireIceBarrage()
	{
		return false;
	}

	@ConfigItem(
		keyName = "infernoRequireBloodBarrage",
		name = "Require Blood Barrage",
		description = "Require the ability to cast Blood Barrage spell. Checks for: 4 Blood runes, 1 Soul rune, 1 Death rune, and Ancient spellbook. Aether runes can substitute for Soul runes.",
		section = infernoSection,
		position = 2
	)
	default boolean infernoRequireBloodBarrage()
	{
		return false;
	}

	@ConfigItem(
		keyName = "infernoBanChugJug",
		name = "Disallow Chug Jug",
		description = "Fail validation if a Chug Jug is found in your inventory.",
		section = infernoSection,
		position = 3
	)
	default boolean infernoBanChugJug()
	{
		return false;
	}

	@ConfigItem(
		keyName = "infernoBanSaturatedHeart",
		name = "Disallow Saturated Heart",
		description = "Fail validation if a Saturated Heart is found in your inventory.",
		section = infernoSection,
		position = 4
	)
	default boolean infernoBanSaturatedHeart()
	{
		return false;
	}
}
