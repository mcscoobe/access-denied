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
import net.runelite.client.plugins.raids.Raid;
import net.runelite.client.plugins.raids.RaidRoom;
import net.runelite.client.plugins.raids.RoomType;
import net.runelite.client.plugins.raids.events.RaidReset;
import net.runelite.client.plugins.raids.events.RaidScouted;
import net.runelite.client.plugins.raids.solver.Room;
import net.runelite.client.util.Text;
import java.util.HashSet;
import java.util.Set;

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

	@Inject
	private ConfigManager configManager;

	private BossLocation currentLocation;
	private int[] currentRegions;
	private ValidationResult lastResult;
	private boolean lastResultWasValid = true;
	private boolean coxRaidActive = false;
	private Raid currentCoxRaid;
	private boolean coxScoutingRaidGood = false;

	@Override
	protected void startUp()
	{
		currentLocation = null;
		currentRegions = null;
		lastResult = null;
		lastResultWasValid = true;
		coxRaidActive = false;
		currentCoxRaid = null;
		coxScoutingRaidGood = false;
	}

	@Override
	protected void shutDown()
	{
		currentLocation = null;
		currentRegions = null;
		lastResult = null;
		lastResultWasValid = true;
		coxRaidActive = false;
		currentCoxRaid = null;
		coxScoutingRaidGood = false;
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
			coxScoutingRaidGood = false;
			lastResult = null;
			lastResultWasValid = true;
		}
	}

	@SuppressWarnings("unused")
	@Subscribe
	public void onRaidScouted(RaidScouted event)
	{
		currentCoxRaid = event.getRaid();
		log.debug("RaidScouted: layout={}", currentCoxRaid.getLayout().toCodeString());
		for (Room layoutRoom : currentCoxRaid.getLayout().getRooms())
		{
			RaidRoom room = currentCoxRaid.getRoom(layoutRoom.getPosition());
			if (room == null) continue;
			log.debug("  room position={} type={} name={}", layoutRoom.getPosition(), room.getType(), room.getName());
		}
		evaluateCoxScoutingRaid();
	}

	@SuppressWarnings("unused")
	@Subscribe
	public void onRaidReset(RaidReset event)
	{
		currentCoxRaid = null;
		coxScoutingRaidGood = false;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!AccessDeniedConfig.CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		if (resolveConflictingCoxSpellbookToggle(event))
		{
			return;
		}

		validateConfiguration(event.getKey());

		// Reset state so the next tick re-evaluates with the new config
		lastResult = null;
		lastResultWasValid = true;

		if (event.getKey().startsWith("coxScout"))
		{
			evaluateCoxScoutingRaid();
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		int eventType = event.getType();
		if (eventType != MenuAction.GAME_OBJECT_FIRST_OPTION.getId()
			&& eventType != MenuAction.GAME_OBJECT_SECOND_OPTION.getId())
		{
			return;
		}

		int objectId = event.getIdentifier();

		if (!coxRaidActive && currentLocation != null)
		{
			Integer validatedObjectId = BossLocations.getObjectForLocation(currentLocation);
			if (validatedObjectId != null && validatedObjectId == objectId)
			{
				// The first menu open can happen before the first game tick has validated;
				// compute on demand so right-clicking immediately on arrival is still protected.
				// When validation isn't required, lastResult stays null and this cheap
				// isValidationRequired check simply re-runs per event — onGameTick owns the
				// cache lifecycle (it nulls lastResult every tick when validation is off), so
				// there is no cache to populate here.
				if (lastResult == null && isValidationRequired(currentLocation))
				{
					lastResult = validateLocationRequirements(currentLocation);
					lastResultWasValid = lastResult.isValid();
				}

				if (lastResult != null && !lastResult.isValid())
				{
					reorderMenuToWalkHere(client.getMenu().getMenuEntries());
					return;
				}
			}
		}

		if (objectId == BossLocations.COX_RELOAD_OBJECT)
		{
			log.debug("onMenuEntryAdded: reload object seen — coxScoutingRaidGood={}", coxScoutingRaidGood);
			if (coxScoutingRaidGood)
			{
				removeGameObjectEntriesForObject(BossLocations.COX_RELOAD_OBJECT);
			}
		}
	}

	private void evaluateCoxScoutingRaid()
	{
		log.debug("evaluateCoxScoutingRaid: enabled={} raidPresent={}", config.coxScoutingEnabled(), currentCoxRaid != null);

		if (!config.coxScoutingEnabled() || currentCoxRaid == null)
		{
			coxScoutingRaidGood = false;
			return;
		}

		Set<String> roomWhitelist = new HashSet<>(Text.fromCSV(config.coxScoutWhitelistedRooms().toLowerCase()));
		Set<String> layoutWhitelist = new HashSet<>(Text.fromCSV(config.coxScoutWhitelistedLayouts().toLowerCase()));

		log.debug("evaluateCoxScoutingRaid: roomWhitelist={} layoutWhitelist={}", roomWhitelist, layoutWhitelist);

		if (roomWhitelist.isEmpty() && layoutWhitelist.isEmpty())
		{
			log.debug("evaluateCoxScoutingRaid: both lists empty — no protection");
			coxScoutingRaidGood = false;
			return;
		}

		if (!roomWhitelist.isEmpty())
		{
			for (Room layoutRoom : currentCoxRaid.getLayout().getRooms())
			{
				RaidRoom room = currentCoxRaid.getRoom(layoutRoom.getPosition());
				if (room == null || (room.getType() != RoomType.COMBAT && room.getType() != RoomType.PUZZLE))
				{
					continue;
				}
				String roomName = room.getName().toLowerCase();
				boolean inWhitelist = roomWhitelist.contains(roomName);
				log.debug("evaluateCoxScoutingRaid: room '{}' (type={}) inWhitelist={}", roomName, room.getType(), inWhitelist);
				if (!inWhitelist)
				{
					log.debug("evaluateCoxScoutingRaid: room '{}' not in whitelist — raid is bad", roomName);
					coxScoutingRaidGood = false;
					return;
				}
			}
		}

		if (!layoutWhitelist.isEmpty())
		{
			String layoutCode = currentCoxRaid.getLayout().toCodeString().toLowerCase();
			boolean inWhitelist = layoutWhitelist.contains(layoutCode);
			log.debug("evaluateCoxScoutingRaid: layoutCode='{}' inWhitelist={}", layoutCode, inWhitelist);
			if (!inWhitelist)
			{
				log.debug("evaluateCoxScoutingRaid: layout '{}' not in whitelist — raid is bad", layoutCode);
				coxScoutingRaidGood = false;
				return;
			}
		}

		boolean wasGood = coxScoutingRaidGood;
		coxScoutingRaidGood = true;
		log.debug("evaluateCoxScoutingRaid: raid is GOOD (wasGood={})", wasGood);

		if (!wasGood)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				"Good raid found — reload protection active.", null);
		}
	}

	private void removeGameObjectEntriesForObject(int objectId)
	{
		MenuEntry[] entries = client.getMenu().getMenuEntries();
		if (entries == null || entries.length == 0)
		{
			return;
		}

		java.util.List<MenuEntry> kept = new java.util.ArrayList<>();
		for (MenuEntry entry : entries)
		{
			if (entry.getIdentifier() == objectId
				&& (entry.getType() == MenuAction.GAME_OBJECT_FIRST_OPTION
					|| entry.getType() == MenuAction.GAME_OBJECT_SECOND_OPTION))
			{
				log.debug("removeGameObjectEntries: removing '{}' for object {}", entry.getOption(), objectId);
				continue;
			}
			kept.add(entry);
		}

		if (kept.size() < entries.length)
		{
			client.getMenu().setMenuEntries(kept.toArray(new MenuEntry[0]));
		}
	}

	private void reorderMenuToWalkHere(MenuEntry[] menuEntries)
	{
		if (menuEntries == null || menuEntries.length == 0)
		{
			log.debug("reorderMenuToWalkHere: no menu entries");
			return;
		}

		int walkHereIndex = -1;
		for (int i = 0; i < menuEntries.length; i++)
		{
			log.debug("reorderMenuToWalkHere: entry[{}] type={} option='{}'", i, menuEntries[i].getType(), menuEntries[i].getOption());
			if (menuEntries[i].getType() == MenuAction.WALK)
			{
				walkHereIndex = i;
				break;
			}
		}

		if (walkHereIndex == -1)
		{
			log.debug("reorderMenuToWalkHere: no Walk Here entry found");
			return;
		}

		if (walkHereIndex == menuEntries.length - 1)
		{
			log.debug("reorderMenuToWalkHere: Walk Here is already the top option");
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

	/**
	 * Keep the CoX config on a single spellbook. Thralls/Death Charge need Arceuus while
	 * Humidify/Vengeance need Lunar, so requiring spells from both groups can never be
	 * satisfied. When a CoX spell toggle is turned on while the opposite group has toggles
	 * enabled, the just-enabled toggle wins: every enabled toggle in the opposite group is
	 * switched off and the player is told which ones. Each switch-off re-fires
	 * onConfigChanged with {@code false}, which passes through harmlessly.
	 *
	 * @return true if a conflicting opposite-group toggle was switched off
	 */
	private boolean resolveConflictingCoxSpellbookToggle(ConfigChanged event)
	{
		String key = event.getKey();
		boolean arceuusKey = "coxRequireSpell".equals(key) || "coxRequireDeathCharge".equals(key);
		boolean lunarKey = "coxRequireHumidify".equals(key) || "coxRequireVengeance".equals(key);

		if ((!arceuusKey && !lunarKey) || !Boolean.parseBoolean(event.getNewValue()))
		{
			return false;
		}

		String[] oppositeKeys = arceuusKey
			? new String[]{"coxRequireHumidify", "coxRequireVengeance"}
			: new String[]{"coxRequireSpell", "coxRequireDeathCharge"};
		boolean[] oppositeEnabled = arceuusKey
			? new boolean[]{config.coxRequireHumidify(), config.coxRequireVengeance()}
			: new boolean[]{config.coxRequireSpell(), config.coxRequireDeathCharge()};

		StringBuilder disabledNames = new StringBuilder();
		for (int i = 0; i < oppositeKeys.length; i++)
		{
			if (oppositeEnabled[i])
			{
				configManager.setConfiguration(AccessDeniedConfig.CONFIG_GROUP, oppositeKeys[i], false);
				if (disabledNames.length() > 0)
				{
					disabledNames.append(", ");
				}
				disabledNames.append(coxSpellName(oppositeKeys[i]));
			}
		}

		if (disabledNames.length() == 0)
		{
			return false;
		}

		String oppositeBook = arceuusKey ? "Lunar" : "Arceuus";
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
			"<col=ff0000>Chambers of Xeric can only require one spellbook. Enabling "
			+ coxSpellName(key) + " disabled the conflicting " + oppositeBook + " spell(s): "
			+ disabledNames + ".</col>",
			null);
		return true;
	}

	private static String coxSpellName(String key)
	{
		switch (key)
		{
			case "coxRequireSpell":
				return "Thralls";
			case "coxRequireDeathCharge":
				return "Death Charge";
			case "coxRequireHumidify":
				return "Humidify";
			case "coxRequireVengeance":
				return "Vengeance";
			default:
				return key;
		}
	}

	private void validateConfiguration(String configKey)
	{
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
