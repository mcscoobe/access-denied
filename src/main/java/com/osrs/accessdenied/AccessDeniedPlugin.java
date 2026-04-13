package com.osrs.accessdenied;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
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
	@Inject
	private Client client;

	@Inject
	private AccessDeniedConfig config;

	@Inject
	private PlayerStateValidator playerStateValidator;

	private BossLocation currentLocation;
	private int[] currentRegions;
	private ValidationResult lastResult;
	private boolean lastResultWasValid = true;

	@Override
	protected void startUp()
	{
		currentLocation = null;
		currentRegions = null;
		lastResult = null;
		lastResultWasValid = true;
	}

	@Override
	protected void shutDown()
	{
		currentLocation = null;
		currentRegions = null;
		lastResult = null;
		lastResultWasValid = true;
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

		int[] newRegions = client.getMapRegions();
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
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (currentLocation == null || !isValidationRequired(currentLocation))
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
		if (currentLocation == null || lastResult == null || lastResult.isValid())
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

		reorderMenuToWalkHere(client.getMenuEntries());
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

		client.setMenuEntries(reordered);
	}

	private void validateConfiguration(String configKey)
	{
		if (!configKey.endsWith("Enabled"))
		{
			return;
		}

		String locationName = null;
		boolean hasRequirements = false;

		if ("nexEnabled".equals(configKey) && config.nexEnabled())
		{
			locationName = "Nex";
			hasRequirements = config.nexRequireSpell() || config.nexRequireDeathCharge();
		}
		else if ("tobEnabled".equals(configKey) && config.tobEnabled())
		{
			locationName = "Theatre of Blood";
			hasRequirements = config.tobRequireSpell() || config.tobRequireDeathCharge();
		}
		else if ("toaEnabled".equals(configKey) && config.toaEnabled())
		{
			locationName = "Tombs of Amascut";
			hasRequirements = config.toaRequireSpell() || config.toaRequireDeathCharge();
		}
		else if ("coxEnabled".equals(configKey) && config.coxEnabled())
		{
			locationName = "Chambers of Xeric";
			hasRequirements = config.coxRequireSpell() || config.coxRequireDeathCharge();
		}
		else if ("infernoEnabled".equals(configKey) && config.infernoEnabled())
		{
			locationName = "Inferno";
			hasRequirements = config.infernoRequireIceBarrage() || config.infernoRequireBloodBarrage();
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
				return validateRaidRequirements(location, config.nexRequireSpell(), config.nexRequireDeathCharge());
			case "tob":
				return validateRaidRequirements(location, config.tobRequireSpell(), config.tobRequireDeathCharge());
			case "toa":
				return validateRaidRequirements(location, config.toaRequireSpell(), config.toaRequireDeathCharge());
			case "cox":
				return validateRaidRequirements(location, config.coxRequireSpell(), config.coxRequireDeathCharge());
			case "inferno":
				return validateInfernoRequirements();
			default:
				return new ValidationResult(true, java.util.Collections.emptySet(), "No validation logic implemented");
		}
	}

	private ValidationResult validateRaidRequirements(BossLocation location, boolean requireThralls, boolean requireDeathCharge)
	{
		boolean hasSpellbook = playerStateValidator.isOnArceuusSpellbook();
		java.util.List<String> missing = new java.util.ArrayList<>();

		boolean thrallsValid = true;
		if (requireThralls)
		{
			boolean hasRunes = playerStateValidator.hasResurrectGreaterGhostRunes();
			boolean hasBook = playerStateValidator.hasBookOfTheDead();
			if (!hasRunes) { missing.add("runes for Thralls"); thrallsValid = false; }
			if (!hasBook) { missing.add("Book of the Dead"); thrallsValid = false; }
		}

		boolean deathChargeValid = true;
		if (requireDeathCharge)
		{
			boolean hasRunes = playerStateValidator.hasDeathChargeRunes();
			if (!hasRunes) { missing.add("runes for Death Charge"); deathChargeValid = false; }
		}

		if (!hasSpellbook)
		{
			missing.add("Arceuus spellbook");
		}

		boolean allValid = hasSpellbook
			&& (!requireThralls || thrallsValid)
			&& (!requireDeathCharge || deathChargeValid);

		if (allValid)
		{
			return new ValidationResult(true, java.util.Collections.emptySet(), "All requirements met");
		}

		String msg = "Missing: " + String.join(", ", missing);
		return new ValidationResult(false, java.util.Collections.singleton(msg), msg);
	}

	private ValidationResult validateInfernoRequirements()
	{
		boolean hasSpellbook = playerStateValidator.isOnAncientSpellbook();
		java.util.List<String> missing = new java.util.ArrayList<>();

		boolean iceValid = true;
		if (config.infernoRequireIceBarrage())
		{
			if (!playerStateValidator.hasIceBarrageRunes()) { missing.add("runes for Ice Barrage"); iceValid = false; }
		}

		boolean bloodValid = true;
		if (config.infernoRequireBloodBarrage())
		{
			if (!playerStateValidator.hasBloodBarrageRunes()) { missing.add("runes for Blood Barrage"); bloodValid = false; }
		}

		if (!hasSpellbook)
		{
			missing.add("Ancient spellbook");
		}

		boolean allValid = hasSpellbook
			&& (!config.infernoRequireIceBarrage() || iceValid)
			&& (!config.infernoRequireBloodBarrage() || bloodValid);

		if (allValid)
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
				return config.nexEnabled() && (config.nexRequireSpell() || config.nexRequireDeathCharge());
			case "tob":
				return config.tobEnabled() && (config.tobRequireSpell() || config.tobRequireDeathCharge());
			case "toa":
				return config.toaEnabled() && (config.toaRequireSpell() || config.toaRequireDeathCharge());
			case "cox":
				return config.coxEnabled() && (config.coxRequireSpell() || config.coxRequireDeathCharge());
			case "inferno":
				return config.infernoEnabled() && (config.infernoRequireIceBarrage() || config.infernoRequireBloodBarrage());
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

	@Provides
	AccessDeniedConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AccessDeniedConfig.class);
	}
}
