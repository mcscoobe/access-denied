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
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.raids.Raid;
import net.runelite.client.plugins.raids.RaidRoom;
import net.runelite.client.plugins.raids.RoomType;
import net.runelite.client.plugins.raids.events.RaidReset;
import net.runelite.client.plugins.raids.events.RaidScouted;
import net.runelite.client.plugins.raids.solver.Room;
import net.runelite.client.util.Text;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@PluginDescriptor(
	name = "Access Denied"
)
public class AccessDeniedPlugin extends Plugin
{
	private static final String RELEASE_LOCK_OPTION = "Release raid lock";

	@Inject
	private Client client;

	@Inject
	private AccessDeniedConfig config;

	@Inject
	private PlayerStateValidator playerStateValidator;

	@Inject
	private ConfigManager configManager;

	private BossLocation currentLocation;

	/**
	 * Requirements the player failed at {@link #currentLocation} as of the last check.
	 * Null means "not checked yet", empty means everything is satisfied.
	 */
	private List<String> lastMissing;
	private boolean lastResultWasValid = true;
	private Raid currentCoxRaid;
	private boolean coxScoutingRaidGood = false;
	private String coxScoutingReleasedLayout;

	@Override
	protected void startUp()
	{
		resetState();
		migrateLegacyCoxSpellConfig();
	}

	@Override
	protected void shutDown()
	{
		resetState();
	}

	private void resetState()
	{
		currentLocation = null;
		lastMissing = null;
		lastResultWasValid = true;
		currentCoxRaid = null;
		coxScoutingRaidGood = false;
		coxScoutingReleasedLayout = null;
	}

	/**
	 * Carries forward the four legacy CoX boolean toggles (coxRequireSpell/
	 * coxRequireDeathCharge/coxRequireHumidify/coxRequireVengeance) into the
	 * coxSpellRequirement enum they were replaced by, then removes the old keys. Without
	 * this, users who had one of those toggles enabled would silently lose their CoX
	 * requirement on upgrade, since nothing reads the old keys any more.
	 */
	private void migrateLegacyCoxSpellConfig()
	{
		String legacySpell = configManager.getConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxRequireSpell");
		String legacyDeathCharge = configManager.getConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxRequireDeathCharge");
		String legacyHumidify = configManager.getConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxRequireHumidify");
		String legacyVengeance = configManager.getConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxRequireVengeance");

		if (legacySpell == null && legacyDeathCharge == null && legacyHumidify == null && legacyVengeance == null)
		{
			return;
		}

		if (configManager.getConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxSpellRequirement") == null)
		{
			configManager.setConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxSpellRequirement",
				CoxSpellRequirement.fromLegacyFlags(
					Boolean.parseBoolean(legacySpell),
					Boolean.parseBoolean(legacyDeathCharge),
					Boolean.parseBoolean(legacyHumidify),
					Boolean.parseBoolean(legacyVengeance)));
		}

		configManager.unsetConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxRequireSpell");
		configManager.unsetConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxRequireDeathCharge");
		configManager.unsetConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxRequireHumidify");
		configManager.unsetConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxRequireVengeance");
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

		BossLocation newLocation = BossLocation.findByRegions(client.getTopLevelWorldView().getMapRegions());

		if (newLocation != currentLocation)
		{
			currentLocation = newLocation;
			lastMissing = null;
			lastResultWasValid = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (coxRaidInProgress() || currentLocation == null || !currentLocation.requiresValidation(config))
		{
			lastMissing = null;
			lastResultWasValid = true;
			return;
		}

		lastMissing = findMissingRequirements(currentLocation);

		boolean valid = lastMissing.isEmpty();
		if (!valid && lastResultWasValid)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", describe(lastMissing), null);
		}

		lastResultWasValid = valid;
	}

	@Subscribe
	public void onRaidScouted(RaidScouted event)
	{
		currentCoxRaid = event.getRaid();
		evaluateCoxScoutingRaid();
	}

	@Subscribe
	public void onRaidReset(RaidReset event)
	{
		currentCoxRaid = null;
		coxScoutingRaidGood = false;
		coxScoutingReleasedLayout = null;
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		// ConfigManager.switchProfile() never re-invokes startUp(), so a profile with
		// un-migrated legacy CoX keys would otherwise keep reading them as unset forever.
		migrateLegacyCoxSpellConfig();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!AccessDeniedConfig.CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		warnIfEnabledWithoutRequirements(event.getKey());

		// Reset state so the next tick re-evaluates with the new config
		lastMissing = null;
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

		// Inside a running raid the entrance object ID is reused by ordinary doors, so
		// every swap from here on would land on the wrong door. Nothing needs blocking
		// once the raid has begun anyway.
		if (coxRaidInProgress())
		{
			return;
		}

		int objectId = event.getIdentifier();

		if (currentLocation != null && currentLocation.getObjectId() == objectId)
		{
			// The first menu open can happen before the first game tick has validated;
			// compute on demand so right-clicking immediately on arrival is still protected.
			// When validation isn't required, lastMissing stays null and this cheap
			// requiresValidation check simply re-runs per event — onGameTick owns the
			// cache lifecycle (it nulls lastMissing every tick when validation is off), so
			// there is no cache to populate here. lastResultWasValid is intentionally left
			// untouched: onGameTick alone decides whether the chat warning fires, so an
			// invalid result computed here doesn't get treated as "already warned about".
			if (lastMissing == null && currentLocation.requiresValidation(config))
			{
				lastMissing = findMissingRequirements(currentLocation);
			}

			if (lastMissing != null && !lastMissing.isEmpty())
			{
				reorderMenuToWalkHere(client.getMenu().getMenuEntries());
				return;
			}
		}

		if (objectId == BossLocation.COX_RELOAD_OBJECT && coxScoutingRaidGood)
		{
			removeGameObjectEntriesForObject(BossLocation.COX_RELOAD_OBJECT);
			addReleaseLockEntry();
		}
	}

	private void evaluateCoxScoutingRaid()
	{
		if (!config.coxScoutingEnabled() || currentCoxRaid == null)
		{
			coxScoutingRaidGood = false;
			return;
		}

		if (currentCoxRaid.getLayout().toCodeString().equalsIgnoreCase(coxScoutingReleasedLayout))
		{
			log.debug("evaluateCoxScoutingRaid: lock released by the player for this layout");
			coxScoutingRaidGood = false;
			return;
		}

		Set<String> roomWhitelist = new HashSet<>(Text.fromCSV(config.coxScoutWhitelistedRooms().toLowerCase()));
		Set<String> layoutWhitelist = new HashSet<>(Text.fromCSV(config.coxScoutWhitelistedLayouts().toLowerCase()));

		if (roomWhitelist.isEmpty() && layoutWhitelist.isEmpty())
		{
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
				if (!roomWhitelist.contains(room.getName().toLowerCase()))
				{
					log.debug("evaluateCoxScoutingRaid: room '{}' not in whitelist — raid is bad", room.getName());
					coxScoutingRaidGood = false;
					return;
				}
			}
		}

		if (!layoutWhitelist.isEmpty())
		{
			String layoutCode = currentCoxRaid.getLayout().toCodeString().toLowerCase();
			if (!layoutWhitelist.contains(layoutCode))
			{
				log.debug("evaluateCoxScoutingRaid: layout '{}' not in whitelist — raid is bad", layoutCode);
				coxScoutingRaidGood = false;
				return;
			}
		}

		boolean wasGood = coxScoutingRaidGood;
		coxScoutingRaidGood = true;

		if (!wasGood)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				"Good raid found — reload protection active.", null);
		}
	}

	/**
	 * Adds the escape hatch for the reload block: right-clicking the reload object still
	 * offers a way out, since the block itself removes every game option on it. Inserted at
	 * index 0 (the bottom of the menu) so it can never become the left-click action — a
	 * stray click must not be able to unlock and then reload a good raid.
	 */
	private void addReleaseLockEntry()
	{
		MenuEntry[] entries = client.getMenu().getMenuEntries();
		for (MenuEntry entry : entries == null ? new MenuEntry[0] : entries)
		{
			if (RELEASE_LOCK_OPTION.equals(entry.getOption()))
			{
				// The object can contribute several entries, one event each — only one hatch.
				return;
			}
		}

		client.getMenu().createMenuEntry(0)
			.setOption(RELEASE_LOCK_OPTION)
			.setType(MenuAction.RUNELITE)
			.onClick(e -> releaseCoxScoutingLock());
	}

	private void releaseCoxScoutingLock()
	{
		coxScoutingRaidGood = false;
		coxScoutingReleasedLayout = currentCoxRaid != null ? currentCoxRaid.getLayout().toCodeString() : null;
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
			"Reload protection released — the reload option is available again.", null);
	}

	private void removeGameObjectEntriesForObject(int objectId)
	{
		MenuEntry[] entries = client.getMenu().getMenuEntries();
		if (entries == null || entries.length == 0)
		{
			return;
		}

		List<MenuEntry> kept = new ArrayList<>();
		for (MenuEntry entry : entries)
		{
			if (entry.getIdentifier() == objectId
				&& (entry.getType() == MenuAction.GAME_OBJECT_FIRST_OPTION
					|| entry.getType() == MenuAction.GAME_OBJECT_SECOND_OPTION))
			{
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

		// The last entry is the left-click option, so an absent or already-last Walk Here
		// leaves nothing to do.
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

	/**
	 * Warns when a location's master toggle is switched on but nothing below it is, which
	 * would otherwise look enabled while validating nothing.
	 */
	private void warnIfEnabledWithoutRequirements(String configKey)
	{
		if (!configKey.endsWith("Enabled"))
		{
			return;
		}

		for (BossLocation location : BossLocation.values())
		{
			if (!configKey.equals(location.getConfigPrefix() + "Enabled"))
			{
				continue;
			}

			if (location.isEnabled(config) && !location.hasRequirements(config))
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", String.format(
					"<col=ff0000>Warning: %s validation is enabled but no requirements are configured. "
						+ "Enable at least one requirement for validation to work.</col>",
					location.getDisplayName()), null);
			}
			return;
		}
	}

	private List<String> findMissingRequirements(BossLocation location)
	{
		List<String> missing = new ArrayList<>();

		switch (location)
		{
			case NEX:
				addArceuusChecks(missing, config.nexRequireSpell(), config.nexRequireDeathCharge());
				break;
			case TOB:
				addArceuusChecks(missing, config.tobRequireSpell(), config.tobRequireDeathCharge());
				break;
			case TOA:
				addArceuusChecks(missing, config.toaRequireSpell(), config.toaRequireDeathCharge());
				break;
			case COX:
				addCoxSpellChecks(missing);
				break;
			case INFERNO:
				addInfernoChecks(missing);
				break;
		}

		if (location.bansChugJug(config) && playerStateValidator.hasChugJug())
		{
			missing.add("remove " + location.getChugJugLabel());
		}

		if (location.bansSaturatedHeart(config) && playerStateValidator.hasSaturatedHeart())
		{
			missing.add("remove Saturated Heart");
		}

		return missing;
	}

	private void addArceuusChecks(List<String> missing, boolean requireThralls, boolean requireDeathCharge)
	{
		if (!requireThralls && !requireDeathCharge)
		{
			return;
		}

		if (requireThralls)
		{
			if (!playerStateValidator.hasResurrectGreaterGhostRunes()) { missing.add("runes for Thralls"); }
			if (!playerStateValidator.hasBookOfTheDead()) { missing.add("Book of the Dead"); }
		}

		if (requireDeathCharge && !playerStateValidator.hasDeathChargeRunes())
		{
			missing.add("runes for Death Charge");
		}

		if (!playerStateValidator.isOnArceuusSpellbook())
		{
			missing.add("Arceuus spellbook");
		}
	}

	private void addCoxSpellChecks(List<String> missing)
	{
		CoxSpellRequirement requirement = config.coxSpellRequirement();

		addArceuusChecks(missing, requirement.requiresThralls(), requirement.requiresDeathCharge());

		if (!requirement.requiresLunar())
		{
			return;
		}

		if (requirement.requiresHumidify() && !playerStateValidator.hasHumidifyRunes())
		{
			missing.add("runes for Humidify");
		}

		if (requirement.requiresVengeance() && !playerStateValidator.hasVengeanceRunes())
		{
			missing.add("runes for Vengeance");
		}

		if (!playerStateValidator.isOnLunarSpellbook())
		{
			missing.add("Lunar spellbook");
		}
	}

	private void addInfernoChecks(List<String> missing)
	{
		boolean ice = config.infernoRequireIceBarrage();
		boolean blood = config.infernoRequireBloodBarrage();

		if (ice && !playerStateValidator.hasIceBarrageRunes())
		{
			missing.add("runes for Ice Barrage");
		}

		if (blood && !playerStateValidator.hasBloodBarrageRunes())
		{
			missing.add("runes for Blood Barrage");
		}

		if ((ice || blood) && !playerStateValidator.isOnAncientSpellbook())
		{
			missing.add("Ancient spellbook");
		}
	}

	/**
	 * True once a Chambers of Xeric raid has started. Read from the varbit rather than
	 * tracked from the "The raid has begun!" message: map region churn while moving
	 * between rooms (and any relog inside the raid) used to clear a tracked flag, which
	 * brought the entrance swap back on in-raid doors sharing the entrance object ID.
	 */
	private boolean coxRaidInProgress()
	{
		return currentLocation == BossLocation.COX
			&& client.getVarbitValue(VarbitID.RAIDS_CLIENT_PROGRESS) > 0;
	}

	private static String describe(List<String> missing)
	{
		return "Missing: " + String.join(", ", missing);
	}

	@Provides
	AccessDeniedConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AccessDeniedConfig.class);
	}
}
