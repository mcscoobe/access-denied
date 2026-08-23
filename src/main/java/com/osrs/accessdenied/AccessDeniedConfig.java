package com.osrs.accessdenied;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(AccessDeniedConfig.CONFIG_GROUP)
public interface AccessDeniedConfig extends Config
{
	String CONFIG_GROUP = "accessdenied";

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
		description = "Require the ability to cast Thralls spell. Checks for: 10 Fire runes, 5 Blood runes, 1 Cosmic rune, Book of the Dead, and Arceuus spellbook. Aether runes can substitute for Cosmic runes; fire staves and the Tome of Fire cover the Fire runes.",
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
		description = "Require the ability to cast Thralls spell. Checks for: 10 Fire runes, 5 Blood runes, 1 Cosmic rune, Book of the Dead, and Arceuus spellbook. Aether runes can substitute for Cosmic runes; fire staves and the Tome of Fire cover the Fire runes.",
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
		description = "Require the ability to cast Thralls spell. Checks for: 10 Fire runes, 5 Blood runes, 1 Cosmic rune, Book of the Dead, and Arceuus spellbook. Aether runes can substitute for Cosmic runes; fire staves and the Tome of Fire cover the Fire runes.",
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
		keyName = "coxSpellRequirement",
		name = "Required spell",
		description = "Which spell to require at Chambers of Xeric. Thralls/Death Charge need the Arceuus spellbook; "
			+ "Humidify/Vengeance need the Lunar spellbook. Only one spellbook can be required at a time, so the "
			+ "choices are mutually exclusive. Thralls: 10 Fire, 5 Blood, 1 Cosmic rune + Book of the Dead. "
			+ "Death Charge: 1 Death, 1 Blood, 1 Soul rune. Humidify: 1 Astral, 1 Fire, 1 Water rune. "
			+ "Vengeance: 10 Earth, 4 Astral, 2 Death runes. Staves/tomes cover their element's rune; Aether substitutes for Soul/Cosmic.",
		section = coxSection,
		position = 1
	)
	default CoxSpellRequirement coxSpellRequirement()
	{
		return CoxSpellRequirement.NONE;
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
		name = "Chambers of Xeric Scouting",
		description = "Protect against accidentally reloading a good CoX raid layout during scouting",
		position = 4
	)
	String coxScoutingSection = "coxScouting";

	@ConfigItem(
		keyName = "coxScoutingEnabled",
		name = "Enable Scouting Protection",
		description = "When a scouted raid passes all configured room and layout checks, replaces the left-click on the reload object with Walk Here to prevent accidental reloads.",
		section = coxScoutingSection,
		position = 0
	)
	default boolean coxScoutingEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "coxScoutRequiredRooms",
		name = "Required Rooms",
		description = "Rooms the raid must all contain. Combat/puzzle only, exact names (e.g. Tightrope, Vasa). "
			+ "Empty = no check.",
		section = coxScoutingSection,
		position = 1
	)
	default String coxScoutRequiredRooms()
	{
		return "";
	}

	@ConfigItem(
		keyName = "coxScoutDisallowedRooms",
		name = "Disallowed Rooms",
		description = "Rooms the raid must not contain. Combat/puzzle only, exact names (e.g. Crabs, Thieving). "
			+ "Empty = no check.",
		section = coxScoutingSection,
		position = 2
	)
	default String coxScoutDisallowedRooms()
	{
		return "";
	}

	@ConfigItem(
		keyName = "coxScoutAllowedLayouts",
		name = "Allowed Layouts",
		description = "Layout codes the raid must start with. Full code (fsccppcscf) = one layout, partial "
			+ "(fscc, scpf) = a family. Empty = no check.",
		section = coxScoutingSection,
		position = 3
	)
	default String coxScoutAllowedLayouts()
	{
		return "";
	}

	@ConfigSection(
		name = "Inferno",
		description = "Configuration for Inferno requirements",
		position = 5
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
		description = "Require the ability to cast Ice Barrage spell. Checks for: 6 Water runes, 2 Death runes, 4 Blood runes, and Ancient spellbook. The Kodai wand, water staves, and the Tome of Water cover the Water runes.",
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
		description = "Require the ability to cast Blood Barrage spell. Checks for: 4 Blood runes, 1 Soul rune, 4 Death runes, and Ancient spellbook. Aether runes can substitute for Soul runes.",
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
