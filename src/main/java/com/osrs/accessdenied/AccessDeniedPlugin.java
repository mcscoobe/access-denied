package com.osrs.accessdenied;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldView;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
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
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stops accidental clicks on boss entrances and on the Chambers of Xeric reload object.
 * There are two independent mechanisms; don't conflate them.
 *
 * <p><b>Entry validation</b> (every {@link BossLocation}). While the player stands in a
 * guarded location's map regions and fails a configured requirement (spellbook, runes,
 * Book of the Dead, a banned item), Walk here becomes the left-click on that location's
 * entrance object. Fed by {@code GameStateChanged} (which location are we in),
 * {@code GameTick} (re-validate, warn in chat on the valid-to-invalid edge) and
 * {@code MenuEntryAdded} (apply the swap). It stops as soon as the requirements are met,
 * the player leaves the location, or a CoX raid is in progress.
 *
 * <p><b>CoX reload lock.</b> Once a scouted raid passes the required-room, disallowed-room
 * and allowed-layout filters, the reload object's first and second game-object options are
 * removed and a right-click "Release raid lock" entry is offered instead. Fed by
 * {@code RaidScouted} and {@code RaidReset} (state), {@code ConfigChanged} on
 * {@code coxScout*} keys (re-evaluate) and {@code MenuEntryAdded} (apply). It stops when the player releases it, the raid
 * resets, or the filters stop matching. Those raid events come from RuneLite's built-in
 * Chambers of Xeric plugin: with that plugin disabled the lock never arms (the player is
 * warned in chat, see {@link #warnIfRaidsPluginDisabled}), and it never arms in challenge
 * mode because that plugin doesn't fire RaidScouted there.
 *
 * <p>Both mechanisms edit the menu from {@code MenuEntryAdded}, which fires again every
 * time the client rebuilds the menu, so the edits are re-applied each frame rather than
 * persisted.
 *
 * <p><b>State resets:</b> everything on start-up and shutdown; the validation cache on
 * location change, on every game tick where validation isn't required or a CoX raid is in
 * progress, and on any config change in this plugin's group except the hide-NPCs key
 * (config changes for other groups, including {@code runelite.raidsplugin}, and the hide-NPCs
 * key all return early); the scouting state on raid reset. A profile change re-migrates
 * legacy config, re-applies hide-NPCs, resets the validation cache and re-evaluates the
 * reload lock against the new profile.
 *
 * <p><b>Threading:</b> config changes arrive off the client thread (AWT), while tick, menu
 * and raid handlers run on it. All plugin state mutation happens on the client thread, so
 * a config path that changes plugin state or touches the client must go through
 * {@link ClientThread}, as {@link #onConfigChanged} and {@link #sendChatMessage} do.
 */
@Slf4j
@PluginDescriptor(
	name = "Access Denied"
)
public class AccessDeniedPlugin extends Plugin
{
	private static final String RELEASE_LOCK_OPTION = "Release raid lock";

	/**
	 * Where RuneLite stores whether its Chambers of Xeric plugin ({@code RaidsPlugin}) is
	 * enabled: the {@code runelite} group, keyed by the lowercased class name.
	 */
	private static final String RUNELITE_CONFIG_GROUP = "runelite";
	private static final String RAIDS_PLUGIN_KEY = "raidsplugin";

	@Inject
	private Client client;

	@Inject
	private AccessDeniedConfig config;

	@Inject
	private PlayerStateValidator playerStateValidator;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private NpcHider npcHider;

	@Inject
	private AccessDeniedPanel panel;

	private NavigationButton navButton;

	/**
	 * The guarded location whose map regions are loaded, or null outside all of them. Set on
	 * {@code GameStateChanged}; cleared by {@link #resetState}.
	 */
	private BossLocation currentLocation;

	/**
	 * Requirements the player failed at {@link #currentLocation} as of the last check.
	 * Null means "not checked yet", empty means everything is satisfied.
	 */
	private List<String> lastMissing;

	/**
	 * Latch for the "Missing: ..." chat warning: it fires only when this goes from true to
	 * false, so the player is told once per failure rather than every tick. Reset to true
	 * wherever {@link #lastMissing} is cleared, which re-arms the warning.
	 */
	private boolean lastResultWasValid = true;

	/** The latest raid from {@code RaidScouted}. Cleared on raid reset and by {@link #resetState}. */
	private Raid currentCoxRaid;

	/**
	 * True while the reload object's options are being removed. Set only by
	 * {@link #evaluateCoxScoutingRaid}; cleared by a failed evaluation, the player's release,
	 * raid reset and {@link #resetState}. Not cleared when the raid starts: the reload-removal
	 * branch of {@link #onMenuEntryAdded} is skipped while the raid progress varbit is
	 * non-zero instead, in every region, so re-arming it mid-raid has no effect.
	 */
	private boolean coxReloadLocked = false;

	/**
	 * Layout code the player released the lock for, so re-scouting the same layout doesn't
	 * re-lock it. Cleared on raid reset and by {@link #resetState}.
	 */
	private String coxScoutingReleasedLayout;

	/**
	 * Latch for the "needs the Chambers Of Xeric plugin" warning: true while the lock can't
	 * arm and the player has been told, so it's sent once per transition into that state.
	 */
	private boolean raidsPluginWarned = false;

	@Override
	protected void startUp()
	{
		resetState();
		migrateLegacyConfig();

		npcHider.startUp();
		applyHideNpcs();

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "panel_icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Access Denied")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);

		// Starting while already logged in fires no GameStateChanged, so the location would
		// stay unknown until the next loading screen.
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				updateLocation();
				warnIfRaidsPluginDisabled();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		resetState();

		// PluginManager calls shutDown() after a startUp() that threw, so the button may never
		// have been built. ClientUI.removeNavigation dereferences its argument.
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}

		npcHider.shutDown();
	}

	/**
	 * Pushes the stored Hide NPCs value into the renderer and back into the side panel, so
	 * the two agree no matter which of them (or which profile) changed it.
	 */
	private void applyHideNpcs()
	{
		boolean hideNpcs = config.hideNpcs();
		npcHider.setHideNpcs(hideNpcs);
		panel.setHideNpcs(hideNpcs);
	}

	private void resetState()
	{
		currentLocation = null;
		lastMissing = null;
		lastResultWasValid = true;
		currentCoxRaid = null;
		coxReloadLocked = false;
		coxScoutingReleasedLayout = null;
		raidsPluginWarned = false;
	}

	/**
	 * Brings a profile written by an older version up to date. Runs on start-up and on every
	 * profile switch, since ConfigManager.switchProfile() never re-invokes startUp().
	 */
	private void migrateLegacyConfig()
	{
		// coxScoutWhitelistedRooms listed the rooms a raid was permitted to contain;
		// coxScoutRequiredRooms lists the ones it must contain. Carrying the value across
		// would reverse its meaning and produce a filter that can never match, so the old
		// key is dropped rather than migrated.
		if (configManager.getConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxScoutWhitelistedRooms") != null)
		{
			configManager.unsetConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxScoutWhitelistedRooms");
		}

		// The layout key was only renamed — its values still mean exactly what they did.
		String legacyLayouts = configManager.getConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxScoutWhitelistedLayouts");
		if (legacyLayouts != null)
		{
			if (configManager.getConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxScoutAllowedLayouts") == null)
			{
				configManager.setConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxScoutAllowedLayouts", legacyLayouts);
			}
			configManager.unsetConfiguration(AccessDeniedConfig.CONFIG_GROUP, "coxScoutWhitelistedLayouts");
		}

		migrateLegacyCoxSpellConfig();
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

		// No local-player check: on a fresh login LOGGED_IN fires before the player spawns,
		// and the map regions don't depend on it.
		updateLocation();

		if (event.getGameState() == GameState.LOGGED_IN)
		{
			warnIfRaidsPluginDisabled();
		}
	}

	private void updateLocation()
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}

		BossLocation newLocation = BossLocation.findByRegions(worldView.getMapRegions());

		if (newLocation != currentLocation)
		{
			currentLocation = newLocation;
			lastMissing = null;
			lastResultWasValid = true;
			warnOnceOnArrivalWithoutRequirements(newLocation);
		}
	}

	/**
	 * Master toggles default to on and requirements to off, so a player who never opens the
	 * settings would never see the toggle-time warning. Tells them once per location, ever:
	 * the "already warned" flag lives in the profile's config, not in plugin state, so it
	 * survives logins and restarts and follows profile switches. It isn't declared in
	 * {@link AccessDeniedConfig}, so it never shows in the config panel.
	 */
	private void warnOnceOnArrivalWithoutRequirements(BossLocation location)
	{
		if (location == null || !location.isEnabled(config) || location.hasRequirements(config))
		{
			return;
		}

		String warnedKey = location.getConfigPrefix() + "NoRequirementsWarned";
		if (configManager.getConfiguration(AccessDeniedConfig.CONFIG_GROUP, warnedKey) != null)
		{
			return;
		}

		configManager.setConfiguration(AccessDeniedConfig.CONFIG_GROUP, warnedKey, true);
		sendNoRequirementsWarning(location);
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
			sendChatMessage(describe(lastMissing));
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
		coxReloadLocked = false;
		coxScoutingReleasedLayout = null;
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		// ConfigManager.switchProfile() never re-invokes startUp(), so a profile with
		// un-migrated legacy CoX keys would otherwise keep reading them as unset forever.
		migrateLegacyConfig();

		// A profile switch replaces every value at once without raising a ConfigChanged per
		// key, so the panel and renderer have to be re-read from the new profile here.
		applyHideNpcs();

		// For the same reason, do what a coxScout* ConfigChanged would: re-arm the
		// "Missing: ..." warning and judge the scouted raid by the new profile's filters.
		// Like ConfigChanged, this arrives off the client thread.
		clientThread.invoke(() ->
		{
			lastMissing = null;
			lastResultWasValid = true;
			evaluateCoxScoutingRaid();
			warnIfRaidsPluginDisabled();
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (RUNELITE_CONFIG_GROUP.equals(event.getGroup()) && RAIDS_PLUGIN_KEY.equals(event.getKey()))
		{
			clientThread.invoke(this::warnIfRaidsPluginDisabled);
			return;
		}

		if (!AccessDeniedConfig.CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		// Returns rather than falling through: the reset below re-arms the "Missing: ..."
		// chat warning, so without this every click of the sidebar toggle would repeat a
		// warning the player has already been given.
		if (AccessDeniedConfig.HIDE_NPCS_KEY.equals(event.getKey()))
		{
			applyHideNpcs();
			return;
		}

		// ConfigChanged arrives on the AWT event thread, but the tick, menu and raid handlers
		// read and write this state on the client thread, so the work is handed over there.
		clientThread.invoke(() ->
		{
			warnIfEnabledWithoutRequirements(event.getKey());

			// Reset state so the next tick re-evaluates with the new config
			lastMissing = null;
			lastResultWasValid = true;

			if (event.getKey().startsWith("coxScout"))
			{
				evaluateCoxScoutingRaid();
				warnIfRaidsPluginDisabled();
			}
		});
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		// Only first and second game-object options are handled, for both the entrance swap
		// and the reload removal. An action exposed as a third-to-fifth option is not covered.
		int eventType = event.getType();
		if (eventType != MenuAction.GAME_OBJECT_FIRST_OPTION.getId()
			&& eventType != MenuAction.GAME_OBJECT_SECOND_OPTION.getId())
		{
			return;
		}

		// Inside a running raid the entrance object ID is reused by ordinary doors, so
		// every swap from here on would land on the wrong door. Nothing needs blocking
		// once the raid has begun anyway. This only holds while currentLocation is COX:
		// raid rooms outside its registered regions leave the location null, so the reload
		// branch below checks the varbit itself.
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

		if (objectId == BossLocation.COX_RELOAD_OBJECT && coxReloadLocked
			&& client.getVarbitValue(VarbitID.RAIDS_CLIENT_PROGRESS) == 0)
		{
			removeGameObjectEntriesForObject(BossLocation.COX_RELOAD_OBJECT);
			addReleaseLockEntry();
		}
	}

	private void evaluateCoxScoutingRaid()
	{
		if (!config.coxScoutingEnabled() || currentCoxRaid == null)
		{
			coxReloadLocked = false;
			return;
		}

		if (currentCoxRaid.getLayout().toCodeString().equalsIgnoreCase(coxScoutingReleasedLayout))
		{
			log.debug("evaluateCoxScoutingRaid: lock released by the player for this layout");
			coxReloadLocked = false;
			return;
		}

		Set<String> requiredRooms = new HashSet<>(Text.fromCSV(config.coxScoutRequiredRooms().toLowerCase()));
		Set<String> disallowedRooms = new HashSet<>(Text.fromCSV(config.coxScoutDisallowedRooms().toLowerCase()));
		List<String> allowedLayouts = Text.fromCSV(config.coxScoutAllowedLayouts().toLowerCase());

		if (requiredRooms.isEmpty() && disallowedRooms.isEmpty() && allowedLayouts.isEmpty())
		{
			coxReloadLocked = false;
			return;
		}

		if (!requiredRooms.isEmpty() || !disallowedRooms.isEmpty())
		{
			Set<RaidRoom> rooms = combatAndPuzzleRooms();

			// A room the player has not walked to yet still reports type COMBAT/PUZZLE, so it
			// would slip past the disallowed check and lock a raid that may yet turn out bad.
			// Wait for the scout to finish instead.
			if (rooms.contains(RaidRoom.UNKNOWN_COMBAT) || rooms.contains(RaidRoom.UNKNOWN_PUZZLE))
			{
				log.debug("evaluateCoxScoutingRaid: raid is not fully scouted yet — raid is bad");
				coxReloadLocked = false;
				return;
			}

			Set<String> raidRooms = new HashSet<>();
			for (RaidRoom room : rooms)
			{
				raidRooms.add(room.getName().toLowerCase());
			}

			// Required rooms must all be present; disallowed rooms must all be absent. Anything
			// in neither list is ignored, so a raid is judged only on what was asked about.
			Set<String> missing = new HashSet<>(requiredRooms);
			missing.removeAll(raidRooms);

			Set<String> disallowed = new HashSet<>(raidRooms);
			disallowed.retainAll(disallowedRooms);

			if (!missing.isEmpty())
			{
				log.debug("evaluateCoxScoutingRaid: raid is missing required rooms {} — raid is bad", missing);
				coxReloadLocked = false;
				return;
			}

			if (!disallowed.isEmpty())
			{
				log.debug("evaluateCoxScoutingRaid: raid contains disallowed rooms {} — raid is bad", disallowed);
				coxReloadLocked = false;
				return;
			}
		}

		if (!allowedLayouts.isEmpty())
		{
			// Entries match by prefix so a partial code like "fscc" selects a family of layouts.
			// A full code is its own prefix and still matches only itself: no layout in the
			// client's table is a prefix of a different one.
			String layoutCode = currentCoxRaid.getLayout().toCodeString().toLowerCase();
			if (allowedLayouts.stream().noneMatch(layoutCode::startsWith))
			{
				log.debug("evaluateCoxScoutingRaid: layout '{}' matches no allowed layout or prefix — raid is bad", layoutCode);
				coxReloadLocked = false;
				return;
			}
		}

		boolean wasLocked = coxReloadLocked;
		coxReloadLocked = true;

		if (!wasLocked)
		{
			sendChatMessage("Good raid found — reload protection active.");
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

	/**
	 * The raid's combat and puzzle rooms. Farming, scavenger and empty rooms are deliberately
	 * excluded: they are not what players filter raids on.
	 */
	private Set<RaidRoom> combatAndPuzzleRooms()
	{
		Set<RaidRoom> rooms = new HashSet<>();
		for (Room layoutRoom : currentCoxRaid.getLayout().getRooms())
		{
			RaidRoom room = currentCoxRaid.getRoom(layoutRoom.getPosition());
			if (room != null && (room.getType() == RoomType.COMBAT || room.getType() == RoomType.PUZZLE))
			{
				rooms.add(room);
			}
		}
		return rooms;
	}

	/**
	 * Chat output must happen on the client thread, but config edits arrive on the AWT event
	 * thread — sending from there trips the client's own assertion and aborts the caller
	 * mid-evaluation. invoke() still runs inline when already on the client thread.
	 */
	private void sendChatMessage(String message)
	{
		clientThread.invoke(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null));
	}

	/**
	 * Keyed on the layout code only, so a later scout with the same code but different rooms
	 * stays unlocked. Lasts until the raid resets.
	 */
	private void releaseCoxScoutingLock()
	{
		coxReloadLocked = false;
		coxScoutingReleasedLayout = currentCoxRaid != null ? currentCoxRaid.getLayout().toCodeString() : null;
		sendChatMessage("Reload protection released — the reload option is available again.");
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

	/**
	 * Makes Walk here the left-click by moving it to the end of the menu, leaving every other
	 * entry in place. Right-click entry stays possible on purpose: the protection targets
	 * accidental left-clicks, not a deliberate choice. Does nothing when the menu has no Walk
	 * here entry.
	 *
	 * <p>This reorders where the reload lock removes. Menu Entry Swapper can undo a reorder
	 * but not a removal (see commit 9f1e429), so a user's own MES swap on an entrance can
	 * override this; removing would also take away the deliberate right-click entry.
	 */
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
				sendNoRequirementsWarning(location);
			}
			return;
		}
	}

	private void sendNoRequirementsWarning(BossLocation location)
	{
		sendChatMessage(String.format(
			"<col=ff0000>Warning: %s validation is enabled but no requirements are configured. "
				+ "Enable at least one requirement for validation to work.</col>",
			location.getDisplayName()));
	}

	/**
	 * Warns once when Scouting Protection is on with a filter set but RuneLite's Chambers of
	 * Xeric plugin is disabled, since without its RaidScouted events the reload lock can't
	 * arm. Re-arms when any of those conditions stops holding.
	 */
	private void warnIfRaidsPluginDisabled()
	{
		// RaidsPlugin is enabled by default, so a missing value means enabled.
		boolean cannotArm = "false".equals(configManager.getConfiguration(RUNELITE_CONFIG_GROUP, RAIDS_PLUGIN_KEY))
			&& config.coxScoutingEnabled()
			&& (!Text.fromCSV(config.coxScoutRequiredRooms()).isEmpty()
				|| !Text.fromCSV(config.coxScoutDisallowedRooms()).isEmpty()
				|| !Text.fromCSV(config.coxScoutAllowedLayouts()).isEmpty());

		if (cannotArm && !raidsPluginWarned)
		{
			sendChatMessage("<col=ff0000>Warning: the CoX reload lock needs RuneLite's \"Chambers Of Xeric\" "
				+ "plugin enabled.</col>");
		}
		raidsPluginWarned = cannotArm;
	}

	private List<String> findMissingRequirements(BossLocation location)
	{
		List<String> missing = new ArrayList<>();

		switch (location)
		{
			case NEX:
				addArceuusChecks(missing, config.nexRequireSpell(), config.nexRequireDeathCharge());
				break;
			case TFA:
				addArceuusChecks(missing, config.tfaRequireSpell(), config.tfaRequireDeathCharge());
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
	 *
	 * <p>Only true while {@link #currentLocation} is COX. In raid rooms outside its
	 * registered regions the location is null, so this returns false there. The entrance
	 * swap is unaffected (it needs a current location anyway), and the reload-removal branch
	 * of {@link #onMenuEntryAdded} reads the varbit directly, so it is suppressed there too.
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
