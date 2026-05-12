package com.osrs.accessdenied;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Access Denied"
)
public class AccessDeniedPlugin extends Plugin
{
	@SuppressWarnings("unused")
	@Inject
	private Client client;

	@SuppressWarnings("unused")
	@Inject
	private AccessDeniedConfig config;

	@SuppressWarnings("unused")
	@Inject
	private PlayerStateValidator playerStateValidator;

	private BossLocation currentLocation;
	private int[] currentRegions;
	private ValidationResult lastResult;
	private boolean lastResultWasValid = true;
	private boolean coxRaidActive = false;

	@Override
	protected void startUp()
	{
		currentLocation = null;
		currentRegions = null;
		lastResult = null;
		lastResultWasValid = true;
		coxRaidActive = false;
	}

	@Override
	protected void shutDown()
	{
		currentLocation = null;
		currentRegions = null;
		lastResult = null;
		lastResultWasValid = true;
		coxRaidActive = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN && event.getGameState() != GameState.LOADING)
		{
			return;
		}

		if (client.getLocalPlayer() == null)
		{
			return;
		}

		int[] newRegions = client.getTopLevelWorldView().getMapRegions();

		if (regionsEqual(currentRegions, newRegions))
		{
			return;
		}

		currentRegions = newRegions;
		BossLocation newLocation = BossLocations.findByAnyRegion(newRegions);

		if (newLocation != currentLocation)
		{
			currentLocation = newLocation;
			lastResult = null;
			lastResultWasValid = true;
			coxRaidActive = false;
		}
	}

	@SuppressWarnings("unused")
	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (coxRaidActive || !isValidationRequired(currentLocation))
		{
			lastResult = null;
			lastResultWasValid = true;
			return;
		}

		ValidationResult result = validateLocationRequirements(currentLocation);

		if (!result.isValid() && lastResultWasValid)
		{
			String message = result.getFeedbackMessage();
			if (message != null && !message.isEmpty())
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
			}
		}

		lastResultWasValid = result.isValid();
		lastResult = result;
	}

	@SuppressWarnings("unused")
	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (currentLocation == null || !currentLocation.getId().equals("cox"))
		{
			return;
		}

		if (event.getMessage().contains("The raid has begun!"))
		{
			log.debug("CoX raid started — suppressing menu swaps and validation");
			coxRaidActive = true;
			lastResult = null;
			lastResultWasValid = true;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"accessdenied".equals(event.getGroup()))
		{
			return;
		}

		validateConfiguration(event.getKey());

		// Reset state so the next tick re-evaluates with the new config
		lastResult = null;
		lastResultWasValid = true;
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (coxRaidActive || currentLocation == null || lastResult == null || lastResult.isValid())
		{
			return;
		}

		int eventType = event.getType();
		if (eventType != MenuAction.GAME_OBJECT_FIRST_OPTION.getId()
			&& eventType != MenuAction.GAME_OBJECT_SECOND_OPTION.getId())
		{
			return;
		}

		int objectId = event.getIdentifier();
		Integer validatedObjectId = BossLocations.getObjectForLocation(currentLocation);
		if (validatedObjectId == null || validatedObjectId != objectId)
		{
			return;
		}

		reorderMenuToWalkHere(client.getMenu().getMenuEntries());
	}

	private void reorderMenuToWalkHere(MenuEntry[] menuEntries)
	{
		if (menuEntries == null || menuEntries.length == 0)
		{
			return;
		}

		int walkHereIndex = -1;
		for (int i = 0; i < menuEntries.length; i++)
		{
			if (menuEntries[i].getType() == MenuAction.WALK)
			{
				walkHereIndex = i;
				break;
			}
		}

		if (walkHereIndex == -1 || walkHereIndex == menuEntries.length - 1)
		{
			return;
		}

		MenuEntry walkHereEntry = menuEntries[walkHereIndex];
		MenuEntry[] reordered = new MenuEntry[menuEntries.length];

		int newIndex = 0;
		for (int i = 0; i < menuEntries.length; i++)
		{
			if (i != walkHereIndex)
			{
				reordered[newIndex++] = menuEntries[i];
			}
		}
		reordered[menuEntries.length - 1] = walkHereEntry;

		client.getMenu().setMenuEntries(reordered);
	}

	private void validateConfiguration(String configKey)
	{
		// Spellbook conflict check — fires on any CoX require* key change
		if (configKey.startsWith("cox"))
		{
			boolean coxArceuusSpells = config.coxRequireSpell() || config.coxRequireDeathCharge();
			boolean coxLunarSpells = config.coxRequireHumidify() || config.coxRequireVengeance();
			if (coxArceuusSpells && coxLunarSpells)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"<col=ff0000>Warning: Chambers of Xeric has conflicting spellbook requirements. " +
					"Arceuus spells (Thralls/Death Charge) and Lunar spells (Humidify/Vengeance) cannot both be required.</col>",
					null);
			}
		}

		// No-requirements warning — fires only when a master Enabled toggle is turned on
		if (!configKey.endsWith("Enabled"))
		{
			return;
		}

		String locationName = null;
		boolean hasRequirements = false;

		if ("nexEnabled".equals(configKey) && config.nexEnabled())
		{
			locationName = "Nex";
			hasRequirements = config.nexRequireSpell() || config.nexRequireDeathCharge()
				|| config.nexBanChugJug() || config.nexBanSaturatedHeart();
		}
		else if ("tobEnabled".equals(configKey) && config.tobEnabled())
		{
			locationName = "Theatre of Blood";
			hasRequirements = config.tobRequireSpell() || config.tobRequireDeathCharge()
				|| config.tobBanChugJug() || config.tobBanSaturatedHeart();
		}
		else if ("toaEnabled".equals(configKey) && config.toaEnabled())
		{
			locationName = "Tombs of Amascut";
			hasRequirements = config.toaRequireSpell() || config.toaRequireDeathCharge()
				|| config.toaBanChugJug() || config.toaBanSaturatedHeart();
		}
		else if ("coxEnabled".equals(configKey) && config.coxEnabled())
		{
			locationName = "Chambers of Xeric";
			hasRequirements = config.coxRequireSpell() || config.coxRequireDeathCharge()
				|| config.coxRequireHumidify() || config.coxRequireVengeance()
				|| config.coxBanChugJug() || config.coxBanSaturatedHeart();
		}
		else if ("infernoEnabled".equals(configKey) && config.infernoEnabled())
		{
			locationName = "Inferno";
			hasRequirements = config.infernoRequireIceBarrage() || config.infernoRequireBloodBarrage()
				|| config.infernoBanChugJug() || config.infernoBanSaturatedHeart();
		}

		if (locationName != null && !hasRequirements)
		{
			String warningMessage = String.format(
				"<col=ff0000>Warning: %s validation is enabled but no requirements are configured. " +
				"Enable at least one requirement (Thralls or Death Charge) for validation to work.</col>",
				locationName
			);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", warningMessage, null);
		}
	}

	private ValidationResult validateLocationRequirements(BossLocation location)
	{
		switch (location.getId())
		{
			case "nex":
				return validateRaidRequirements(config.nexRequireSpell(), config.nexRequireDeathCharge(), config.nexBanChugJug(), config.nexBanSaturatedHeart());
			case "tob":
				return validateRaidRequirements(config.tobRequireSpell(), config.tobRequireDeathCharge(), config.tobBanChugJug(), config.tobBanSaturatedHeart());
			case "toa":
				return validateRaidRequirements(config.toaRequireSpell(), config.toaRequireDeathCharge(), config.toaBanChugJug(), config.toaBanSaturatedHeart());
			case "cox":
				return validateCoxRequirements();
			case "inferno":
				return validateInfernoRequirements();
			default:
				return new ValidationResult(true, java.util.Collections.emptySet(), "No validation logic implemented");
		}
	}

	private ValidationResult validateRaidRequirements(boolean requireThralls, boolean requireDeathCharge, boolean banChugJug, boolean banSaturatedHeart)
	{
		java.util.List<String> missing = new java.util.ArrayList<>();

		if (requireThralls)
		{
			if (!playerStateValidator.hasResurrectGreaterGhostRunes()) { missing.add("runes for Thralls"); }
			if (!playerStateValidator.hasBookOfTheDead()) { missing.add("Book of the Dead"); }
		}

		if (requireDeathCharge)
		{
			if (!playerStateValidator.hasDeathChargeRunes()) { missing.add("runes for Death Charge"); }
		}

		if (requireThralls || requireDeathCharge)
		{
			if (!playerStateValidator.isOnArceuusSpellbook()) { missing.add("Arceuus spellbook"); }
		}

		if (banChugJug && playerStateValidator.hasChugJug())
		{
			missing.add("remove Chug Jug");
		}

		if (banSaturatedHeart && playerStateValidator.hasSaturatedHeart())
		{
			missing.add("remove Saturated Heart");
		}

		if (missing.isEmpty())
		{
			return new ValidationResult(true, java.util.Collections.emptySet(), "All requirements met");
		}

		String msg = "Missing: " + String.join(", ", missing);
		return new ValidationResult(false, java.util.Collections.singleton(msg), msg);
	}

	private ValidationResult validateCoxRequirements()
	{
		java.util.List<String> missing = new java.util.ArrayList<>();

		boolean requireArceuus = config.coxRequireSpell() || config.coxRequireDeathCharge();
		if (requireArceuus)
		{
			if (config.coxRequireSpell())
			{
				if (!playerStateValidator.hasResurrectGreaterGhostRunes()) { missing.add("runes for Thralls"); }
				if (!playerStateValidator.hasBookOfTheDead()) { missing.add("Book of the Dead"); }
			}
			if (config.coxRequireDeathCharge())
			{
				if (!playerStateValidator.hasDeathChargeRunes()) { missing.add("runes for Death Charge"); }
			}
			if (!playerStateValidator.isOnArceuusSpellbook()) { missing.add("Arceuus spellbook"); }
		}

		boolean requireLunar = config.coxRequireHumidify() || config.coxRequireVengeance();
		if (requireLunar)
		{
			if (config.coxRequireHumidify())
			{
				if (!playerStateValidator.hasHumidifyRunes()) { missing.add("runes for Humidify"); }
			}
			if (config.coxRequireVengeance())
			{
				if (!playerStateValidator.hasVengeanceRunes()) { missing.add("runes for Vengeance"); }
			}
			if (!playerStateValidator.isOnLunarSpellbook()) { missing.add("Lunar spellbook"); }
		}

		if (config.coxBanChugJug() && playerStateValidator.hasChugJug())
		{
			missing.add("remove Chugging Barrel");
		}

		if (config.coxBanSaturatedHeart() && playerStateValidator.hasSaturatedHeart())
		{
			missing.add("remove Saturated Heart");
		}

		if (missing.isEmpty())
		{
			return new ValidationResult(true, java.util.Collections.emptySet(), "All requirements met");
		}

		String msg = "Missing: " + String.join(", ", missing);
		return new ValidationResult(false, java.util.Collections.singleton(msg), msg);
	}

	private ValidationResult validateInfernoRequirements()
	{
		java.util.List<String> missing = new java.util.ArrayList<>();

		if (config.infernoRequireIceBarrage())
		{
			if (!playerStateValidator.hasIceBarrageRunes()) { missing.add("runes for Ice Barrage"); }
		}

		if (config.infernoRequireBloodBarrage())
		{
			if (!playerStateValidator.hasBloodBarrageRunes()) { missing.add("runes for Blood Barrage"); }
		}

		if (config.infernoRequireIceBarrage() || config.infernoRequireBloodBarrage())
		{
			if (!playerStateValidator.isOnAncientSpellbook()) { missing.add("Ancient spellbook"); }
		}

		if (config.infernoBanChugJug() && playerStateValidator.hasChugJug())
		{
			missing.add("remove Chug Jug");
		}

		if (config.infernoBanSaturatedHeart() && playerStateValidator.hasSaturatedHeart())
		{
			missing.add("remove Saturated Heart");
		}

		if (missing.isEmpty())
		{
			return new ValidationResult(true, java.util.Collections.emptySet(), "All requirements met");
		}

		String msg = "Missing: " + String.join(", ", missing);
		return new ValidationResult(false, java.util.Collections.singleton(msg), msg);
	}

	private boolean isValidationRequired(BossLocation location)
	{
		if (location == null)
		{
			return false;
		}

		switch (location.getId())
		{
			case "nex":
				return config.nexEnabled() && (config.nexRequireSpell() || config.nexRequireDeathCharge()
					|| config.nexBanChugJug() || config.nexBanSaturatedHeart());
			case "tob":
				return config.tobEnabled() && (config.tobRequireSpell() || config.tobRequireDeathCharge()
					|| config.tobBanChugJug() || config.tobBanSaturatedHeart());
			case "toa":
				return config.toaEnabled() && (config.toaRequireSpell() || config.toaRequireDeathCharge()
					|| config.toaBanChugJug() || config.toaBanSaturatedHeart());
			case "cox":
				return config.coxEnabled() && (config.coxRequireSpell() || config.coxRequireDeathCharge()
					|| config.coxRequireHumidify() || config.coxRequireVengeance()
					|| config.coxBanChugJug() || config.coxBanSaturatedHeart());
			case "inferno":
				return config.infernoEnabled() && (config.infernoRequireIceBarrage() || config.infernoRequireBloodBarrage()
					|| config.infernoBanChugJug() || config.infernoBanSaturatedHeart());
			default:
				return false;
		}
	}

	private boolean regionsEqual(int[] regions1, int[] regions2)
	{
		if (regions1 == null && regions2 == null) return true;
		if (regions1 == null || regions2 == null) return false;
		if (regions1.length != regions2.length) return false;

		java.util.Set<Integer> set1 = new java.util.HashSet<>();
		java.util.Set<Integer> set2 = new java.util.HashSet<>();
		for (int r : regions1) set1.add(r);
		for (int r : regions2) set2.add(r);
		return set1.equals(set2);
	}

	@SuppressWarnings("unused")
	@Provides
	AccessDeniedConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AccessDeniedConfig.class);
	}
}
