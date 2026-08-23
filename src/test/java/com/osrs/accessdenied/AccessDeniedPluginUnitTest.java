package com.osrs.accessdenied;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.raids.Raid;
import net.runelite.client.plugins.raids.solver.Layout;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccessDeniedPlugin event handlers and core logic.
 */
public class AccessDeniedPluginUnitTest
{
	@Mock
	private Client client;

	@Mock
	private AccessDeniedConfig config;

	@Mock
	private PlayerStateValidator validator;

	@Mock
	private ConfigManager configManager;

	@Mock
	private Player player;

	@Mock
	private Menu menu;

	@Mock
	private WorldView worldView;

	private AccessDeniedPlugin plugin;
	private AutoCloseable mocks;

	@After
	public void tearDown() throws Exception
	{
		mocks.close();
	}

	@Before
	public void setUp() throws Exception
	{
		mocks = MockitoAnnotations.openMocks(this);
		plugin = new AccessDeniedPlugin();

		// Inject mocked dependencies
		setField(plugin, "client", client);
		setField(plugin, "config", config);
		setField(plugin, "playerStateValidator", validator);
		setField(plugin, "configManager", configManager);

		// Setup default mocks
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getTopLevelWorldView()).thenReturn(worldView);
		when(client.getMenu()).thenReturn(menu);
	}

	@Test
	public void testStartUpClearsState() throws Exception
	{
		plugin.startUp();

		// Verify state is cleared
		assertNull(getField(plugin, "currentLocation"));
		assertNull(getField(plugin, "currentRegions"));
		assertNull(getField(plugin, "lastResult"));
		assertTrue(getField(plugin, "lastResultWasValid"));
	}

	@Test
	public void testStartUpMigratesLegacyCoxSpellConfig() throws Exception
	{
		when(configManager.getConfiguration("accessdenied", "coxRequireSpell")).thenReturn("true");
		when(configManager.getConfiguration("accessdenied", "coxRequireDeathCharge")).thenReturn("true");
		when(configManager.getConfiguration("accessdenied", "coxRequireHumidify")).thenReturn("false");
		when(configManager.getConfiguration("accessdenied", "coxRequireVengeance")).thenReturn("false");
		when(configManager.getConfiguration("accessdenied", "coxSpellRequirement")).thenReturn(null);

		plugin.startUp();

		verify(configManager).setConfiguration("accessdenied", "coxSpellRequirement", CoxSpellRequirement.THRALLS_AND_DEATH_CHARGE);
		verify(configManager).unsetConfiguration("accessdenied", "coxRequireSpell");
		verify(configManager).unsetConfiguration("accessdenied", "coxRequireDeathCharge");
		verify(configManager).unsetConfiguration("accessdenied", "coxRequireHumidify");
		verify(configManager).unsetConfiguration("accessdenied", "coxRequireVengeance");
	}

	@Test
	public void testStartUpMigrationResolvesCrossSpellbookLegacyConflictToArceuus() throws Exception
	{
		// Saved before the mutual-exclusion fix (commit 2666029): both an Arceuus and a
		// Lunar toggle are enabled at once. The migration must not silently drop everything.
		when(configManager.getConfiguration("accessdenied", "coxRequireSpell")).thenReturn("true");
		when(configManager.getConfiguration("accessdenied", "coxRequireDeathCharge")).thenReturn("false");
		when(configManager.getConfiguration("accessdenied", "coxRequireHumidify")).thenReturn("true");
		when(configManager.getConfiguration("accessdenied", "coxRequireVengeance")).thenReturn("false");
		when(configManager.getConfiguration("accessdenied", "coxSpellRequirement")).thenReturn(null);

		plugin.startUp();

		verify(configManager).setConfiguration("accessdenied", "coxSpellRequirement", CoxSpellRequirement.THRALLS);
		verify(client).addChatMessage(eq(ChatMessageType.GAMEMESSAGE), eq(""), anyString(), isNull());
		verify(configManager).unsetConfiguration("accessdenied", "coxRequireSpell");
		verify(configManager).unsetConfiguration("accessdenied", "coxRequireDeathCharge");
		verify(configManager).unsetConfiguration("accessdenied", "coxRequireHumidify");
		verify(configManager).unsetConfiguration("accessdenied", "coxRequireVengeance");
	}

	@Test
	public void testStartUpSkipsMigrationWhenNoLegacyKeysPresent() throws Exception
	{
		plugin.startUp();

		verify(configManager, never()).setConfiguration(anyString(), anyString(), any());
		verify(configManager, never()).unsetConfiguration(anyString(), anyString());
	}

	@Test
	public void testStartUpMigrationDoesNotOverwriteExplicitCoxSpellRequirement() throws Exception
	{
		when(configManager.getConfiguration("accessdenied", "coxRequireSpell")).thenReturn("true");
		when(configManager.getConfiguration("accessdenied", "coxSpellRequirement")).thenReturn("VENGEANCE");

		plugin.startUp();

		verify(configManager, never()).setConfiguration(eq("accessdenied"), eq("coxSpellRequirement"), any());
		verify(configManager).unsetConfiguration("accessdenied", "coxRequireSpell");
	}

	@Test
	public void testOnProfileChangedRerunsLegacyCoxSpellMigration() throws Exception
	{
		// ConfigManager.switchProfile() never re-invokes startUp(), so a profile switch
		// must independently re-trigger migration for the newly active profile's data.
		when(configManager.getConfiguration("accessdenied", "coxRequireVengeance")).thenReturn("true");
		when(configManager.getConfiguration("accessdenied", "coxSpellRequirement")).thenReturn(null);

		ProfileChanged event = mock(ProfileChanged.class);
		plugin.onProfileChanged(event);

		verify(configManager).setConfiguration("accessdenied", "coxSpellRequirement", CoxSpellRequirement.VENGEANCE);
		verify(configManager).unsetConfiguration("accessdenied", "coxRequireVengeance");
	}

	@Test
	public void testShutDownClearsState() throws Exception
	{
		plugin.shutDown();

		// Verify state is cleared
		assertNull(getField(plugin, "currentLocation"));
		assertNull(getField(plugin, "currentRegions"));
		assertNull(getField(plugin, "lastResult"));
		assertTrue(getField(plugin, "lastResultWasValid"));
	}

	@Test
	public void testOnGameStateChangedIgnoresNonLoggedInStates()
	{
		GameStateChanged event = mock(GameStateChanged.class);
		when(event.getGameState()).thenReturn(GameState.LOGIN_SCREEN);

		plugin.onGameStateChanged(event);

		// Should not interact with client
		verify(client, never()).getTopLevelWorldView();
	}

	@Test
	public void testOnGameStateChangedIgnoresWhenNoPlayer()
	{
		GameStateChanged event = mock(GameStateChanged.class);
		when(event.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getLocalPlayer()).thenReturn(null);

		plugin.onGameStateChanged(event);

		// Should not proceed without player
		verify(client, never()).getTopLevelWorldView();
	}

	@Test
	public void testOnGameStateChangedDetectsRegionChange()
	{
		GameStateChanged event = mock(GameStateChanged.class);
		when(event.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(worldView.getMapRegions()).thenReturn(new int[]{11601});

		plugin.onGameStateChanged(event);

		// Should detect region change
		verify(client, atLeastOnce()).getTopLevelWorldView();
	}

	@Test
	public void testRegionsEqualWithSameRegions() throws Exception
	{
		Method regionsEqual = plugin.getClass().getDeclaredMethod("regionsEqual", int[].class, int[].class);
		regionsEqual.setAccessible(true);

		int[] regions1 = {11601, 11602};
		int[] regions2 = {11601, 11602};

		boolean result = (boolean) regionsEqual.invoke(plugin, regions1, regions2);
		assertTrue(result);
	}

	@Test
	public void testRegionsEqualWithDifferentOrder() throws Exception
	{
		Method regionsEqual = plugin.getClass().getDeclaredMethod("regionsEqual", int[].class, int[].class);
		regionsEqual.setAccessible(true);

		int[] regions1 = {11601, 11602};
		int[] regions2 = {11602, 11601};

		boolean result = (boolean) regionsEqual.invoke(plugin, regions1, regions2);
		assertTrue(result);
	}

	@Test
	public void testRegionsEqualWithDifferentRegions() throws Exception
	{
		Method regionsEqual = plugin.getClass().getDeclaredMethod("regionsEqual", int[].class, int[].class);
		regionsEqual.setAccessible(true);

		int[] regions1 = {11601, 11602};
		int[] regions2 = {11601, 11603};

		boolean result = (boolean) regionsEqual.invoke(plugin, regions1, regions2);
		assertFalse(result);
	}

	@Test
	public void testRegionsEqualWithNulls() throws Exception
	{
		Method regionsEqual = plugin.getClass().getDeclaredMethod("regionsEqual", int[].class, int[].class);
		regionsEqual.setAccessible(true);

		boolean result1 = (boolean) regionsEqual.invoke(plugin, null, null);
		assertTrue(result1);

		int[] regions = {11601};
		boolean result2 = (boolean) regionsEqual.invoke(plugin, regions, null);
		assertFalse(result2);

		boolean result3 = (boolean) regionsEqual.invoke(plugin, null, regions);
		assertFalse(result3);
	}

	@Test
	public void testIsValidationRequiredWithNexEnabled() throws Exception
	{
		when(config.nexEnabled()).thenReturn(true);
		when(config.nexRequireSpell()).thenReturn(true);
		when(config.nexRequireDeathCharge()).thenReturn(false);

		Method isValidationRequired = plugin.getClass().getDeclaredMethod("isValidationRequired", BossLocation.class);
		isValidationRequired.setAccessible(true);

		boolean result = (boolean) isValidationRequired.invoke(plugin, BossLocations.NEX);
		assertTrue(result);
	}

	@Test
	public void testIsValidationRequiredWithNexDisabled() throws Exception
	{
		when(config.nexEnabled()).thenReturn(false);
		when(config.nexRequireSpell()).thenReturn(true);
		when(config.nexRequireDeathCharge()).thenReturn(true);

		Method isValidationRequired = plugin.getClass().getDeclaredMethod("isValidationRequired", BossLocation.class);
		isValidationRequired.setAccessible(true);

		boolean result = (boolean) isValidationRequired.invoke(plugin, BossLocations.NEX);
		assertFalse(result);
	}

	@Test
	public void testIsValidationRequiredWithNoRequirements() throws Exception
	{
		when(config.nexEnabled()).thenReturn(true);
		when(config.nexRequireSpell()).thenReturn(false);
		when(config.nexRequireDeathCharge()).thenReturn(false);

		Method isValidationRequired = plugin.getClass().getDeclaredMethod("isValidationRequired", BossLocation.class);
		isValidationRequired.setAccessible(true);

		boolean result = (boolean) isValidationRequired.invoke(plugin, BossLocations.NEX);
		assertFalse(result);
	}

	@Test
	public void testValidateConfigurationShowsWarning() throws Exception
	{
		when(config.nexEnabled()).thenReturn(true);
		when(config.nexRequireSpell()).thenReturn(false);
		when(config.nexRequireDeathCharge()).thenReturn(false);

		Method validateConfiguration = plugin.getClass().getDeclaredMethod("validateConfiguration", String.class);
		validateConfiguration.setAccessible(true);

		validateConfiguration.invoke(plugin, "nexEnabled");

		// Verify warning message was sent
		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(client).addChatMessage(
			eq(ChatMessageType.GAMEMESSAGE),
			eq(""),
			messageCaptor.capture(),
			isNull()
		);

		String message = messageCaptor.getValue();
		assertTrue(message.contains("Warning"));
		assertTrue(message.contains("Nex"));
		assertTrue(message.contains("no requirements"));
	}

	@Test
	public void testValidateConfigurationNoWarningWhenRequirementsSet() throws Exception
	{
		when(config.nexEnabled()).thenReturn(true);
		when(config.nexRequireSpell()).thenReturn(true);
		when(config.nexRequireDeathCharge()).thenReturn(false);

		Method validateConfiguration = plugin.getClass().getDeclaredMethod("validateConfiguration", String.class);
		validateConfiguration.setAccessible(true);

		validateConfiguration.invoke(plugin, "nexEnabled");

		// Verify no warning message was sent
		verify(client, never()).addChatMessage(any(), any(), any(), any());
	}

	@Test
	public void testIsValidationRequiredWithCoxHumidify() throws Exception
	{
		when(config.coxEnabled()).thenReturn(true);
		when(config.coxSpellRequirement()).thenReturn(CoxSpellRequirement.HUMIDIFY);

		Method isValidationRequired = plugin.getClass().getDeclaredMethod("isValidationRequired", BossLocation.class);
		isValidationRequired.setAccessible(true);

		boolean result = (boolean) isValidationRequired.invoke(plugin, BossLocations.CHAMBERS_OF_XERIC);
		assertTrue(result);
	}

	@Test
	public void testIsValidationRequiredWithCoxVengeance() throws Exception
	{
		when(config.coxEnabled()).thenReturn(true);
		when(config.coxSpellRequirement()).thenReturn(CoxSpellRequirement.VENGEANCE);

		Method isValidationRequired = plugin.getClass().getDeclaredMethod("isValidationRequired", BossLocation.class);
		isValidationRequired.setAccessible(true);

		boolean result = (boolean) isValidationRequired.invoke(plugin, BossLocations.CHAMBERS_OF_XERIC);
		assertTrue(result);
	}

	@Test
	public void testIsValidationRequiredWithCoxNoRequirements() throws Exception
	{
		when(config.coxEnabled()).thenReturn(true);
		when(config.coxSpellRequirement()).thenReturn(CoxSpellRequirement.NONE);

		Method isValidationRequired = plugin.getClass().getDeclaredMethod("isValidationRequired", BossLocation.class);
		isValidationRequired.setAccessible(true);

		boolean result = (boolean) isValidationRequired.invoke(plugin, BossLocations.CHAMBERS_OF_XERIC);
		assertFalse(result);
	}

	@Test
	public void testCoxValidationPassesForArceuusComboWhenSatisfied() throws Exception
	{
		// THRALLS_AND_DEATH_CHARGE requires both Arceuus spells plus the Arceuus spellbook.
		when(config.coxSpellRequirement()).thenReturn(CoxSpellRequirement.THRALLS_AND_DEATH_CHARGE);
		when(validator.hasResurrectGreaterGhostRunes()).thenReturn(true);
		when(validator.hasBookOfTheDead()).thenReturn(true);
		when(validator.hasDeathChargeRunes()).thenReturn(true);
		when(validator.isOnArceuusSpellbook()).thenReturn(true);

		ValidationResult result = invokeValidateLocation(BossLocations.CHAMBERS_OF_XERIC);

		assertTrue("Arceuus combo should pass when fully satisfied", result.isValid());
		verify(validator).hasDeathChargeRunes();
		verify(validator).isOnArceuusSpellbook();
		// Lunar checks must not run for an Arceuus selection.
		verify(validator, never()).hasHumidifyRunes();
		verify(validator, never()).isOnLunarSpellbook();
	}

	@Test
	public void testCoxValidationFailsWhenArceuusSpellbookMissing() throws Exception
	{
		when(config.coxSpellRequirement()).thenReturn(CoxSpellRequirement.THRALLS);
		when(validator.hasResurrectGreaterGhostRunes()).thenReturn(true);
		when(validator.hasBookOfTheDead()).thenReturn(true);
		when(validator.isOnArceuusSpellbook()).thenReturn(false);

		ValidationResult result = invokeValidateLocation(BossLocations.CHAMBERS_OF_XERIC);

		assertFalse("Should fail when not on the Arceuus spellbook", result.isValid());
	}

	@Test
	public void testCoxValidationChecksLunarPathForVengeance() throws Exception
	{
		when(config.coxSpellRequirement()).thenReturn(CoxSpellRequirement.VENGEANCE);
		when(validator.hasVengeanceRunes()).thenReturn(true);
		when(validator.isOnLunarSpellbook()).thenReturn(true);

		ValidationResult result = invokeValidateLocation(BossLocations.CHAMBERS_OF_XERIC);

		assertTrue("Vengeance should pass when runes and Lunar spellbook are present", result.isValid());
		verify(validator).hasVengeanceRunes();
		verify(validator).isOnLunarSpellbook();
		// Arceuus checks must not run for a Lunar selection.
		verify(validator, never()).hasResurrectGreaterGhostRunes();
		verify(validator, never()).isOnArceuusSpellbook();
	}

	@Test
	public void testCoxValidationPassesWithNoneAndNoBans() throws Exception
	{
		// NONE with no bans is satisfiable and must not query any spell requirements.
		when(config.coxSpellRequirement()).thenReturn(CoxSpellRequirement.NONE);

		ValidationResult result = invokeValidateLocation(BossLocations.CHAMBERS_OF_XERIC);

		assertTrue("NONE with no bans should be valid", result.isValid());
		verify(validator, never()).hasResurrectGreaterGhostRunes();
		verify(validator, never()).hasDeathChargeRunes();
		verify(validator, never()).hasHumidifyRunes();
		verify(validator, never()).hasVengeanceRunes();
		verify(validator, never()).isOnArceuusSpellbook();
		verify(validator, never()).isOnLunarSpellbook();
	}

	private ValidationResult invokeValidateLocation(BossLocation location) throws Exception
	{
		Method validate = plugin.getClass().getDeclaredMethod("validateLocationRequirements", BossLocation.class);
		validate.setAccessible(true);
		return (ValidationResult) validate.invoke(plugin, location);
	}

	@Test
	public void testOnConfigChangedIgnoresOtherGroups()
	{
		ConfigChanged event = mock(ConfigChanged.class);
		when(event.getGroup()).thenReturn("othergroup");
		when(event.getKey()).thenReturn("somekey");
		
		plugin.onConfigChanged(event);

		// Should not send any chat messages or interact with client
		verify(client, never()).addChatMessage(any(), any(), any(), any());
	}

	@Test
	public void testOnMenuEntryAddedIgnoresWhenNotInBossLocation() throws Exception
	{
		setField(plugin, "currentLocation", null);

		MenuEntryAdded event = mock(MenuEntryAdded.class);
		when(event.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION.getId());

		plugin.onMenuEntryAdded(event);

		// Should not modify menu
		verify(menu, never()).getMenuEntries();

	}

	@Test
	public void testOnMenuEntryAddedValidatesOnDemandAndReordersWhenInvalid() throws Exception
	{
		// At a validated object on the arrival tick, lastResult is still null — the handler
		// must validate on demand and, finding the state invalid, reorder the menu.
		setField(plugin, "currentLocation", BossLocations.NEX);
		setField(plugin, "lastResult", null);

		when(config.nexEnabled()).thenReturn(true);
		when(config.nexRequireSpell()).thenReturn(true);
		// validator mock returns false by default, so validation is invalid

		MenuEntry walkEntry = mock(MenuEntry.class);
		when(walkEntry.getType()).thenReturn(MenuAction.WALK);
		MenuEntry objectEntry = mock(MenuEntry.class);
		when(objectEntry.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION);
		when(menu.getMenuEntries()).thenReturn(new MenuEntry[]{walkEntry, objectEntry});

		MenuEntryAdded event = mock(MenuEntryAdded.class);
		when(event.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION.getId());
		when(event.getIdentifier()).thenReturn(BossLocations.NEX_OBJECT);

		plugin.onMenuEntryAdded(event);

		ValidationResult cached = getField(plugin, "lastResult");
		assertNotNull("on-demand validation should cache a result", cached);
		assertFalse("cached result should be invalid", cached.isValid());
		verify(menu).setMenuEntries(any());
	}

	@Test
	public void testOnMenuEntryAddedOnDemandInvalidDoesNotSuppressNextGameTickWarning() throws Exception
	{
		// Regression test: onMenuEntryAdded's on-demand validation must not mark the
		// invalid result as "already warned about" — onGameTick alone decides when the
		// chat warning fires, so it must still fire on the very next tick.
		setField(plugin, "currentLocation", BossLocations.NEX);
		setField(plugin, "lastResult", null);
		setField(plugin, "lastResultWasValid", true);

		when(config.nexEnabled()).thenReturn(true);
		when(config.nexRequireSpell()).thenReturn(true);
		// validator mock returns false by default, so validation is invalid

		MenuEntry walkEntry = mock(MenuEntry.class);
		when(walkEntry.getType()).thenReturn(MenuAction.WALK);
		MenuEntry objectEntry = mock(MenuEntry.class);
		when(objectEntry.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION);
		when(menu.getMenuEntries()).thenReturn(new MenuEntry[]{walkEntry, objectEntry});

		MenuEntryAdded event = mock(MenuEntryAdded.class);
		when(event.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION.getId());
		when(event.getIdentifier()).thenReturn(BossLocations.NEX_OBJECT);

		plugin.onMenuEntryAdded(event);

		assertTrue("lastResultWasValid must stay true so onGameTick still fires the warning",
			(boolean) getField(plugin, "lastResultWasValid"));

		plugin.onGameTick(mock(GameTick.class));

		verify(client).addChatMessage(eq(ChatMessageType.GAMEMESSAGE), eq(""), anyString(), isNull());
	}

	@Test
	public void testOnMenuEntryAddedOnDemandDoesNotReorderWhenValid() throws Exception
	{
		setField(plugin, "currentLocation", BossLocations.NEX);
		setField(plugin, "lastResult", null);

		when(config.nexEnabled()).thenReturn(true);
		when(config.nexRequireSpell()).thenReturn(true);
		// Make the on-demand validation pass
		when(validator.hasResurrectGreaterGhostRunes()).thenReturn(true);
		when(validator.hasBookOfTheDead()).thenReturn(true);
		when(validator.isOnArceuusSpellbook()).thenReturn(true);

		MenuEntryAdded event = mock(MenuEntryAdded.class);
		when(event.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION.getId());
		when(event.getIdentifier()).thenReturn(BossLocations.NEX_OBJECT);

		plugin.onMenuEntryAdded(event);

		ValidationResult cached = getField(plugin, "lastResult");
		assertNotNull("on-demand validation should cache a result", cached);
		assertTrue("cached result should be valid", cached.isValid());
		verify(menu, never()).setMenuEntries(any());
	}

	@Test
	public void testOnMenuEntryAddedSkipsOnDemandDuringActiveCoxRaid() throws Exception
	{
		// Doors inside the raid reuse the entrance object ID, so once the raid has started
		// the handler must short-circuit before any swap.
		setField(plugin, "currentLocation", BossLocations.CHAMBERS_OF_XERIC);
		setField(plugin, "lastResult", null);
		when(client.getVarbitValue(VarbitID.RAIDS_CLIENT_PROGRESS)).thenReturn(1);

		when(config.coxEnabled()).thenReturn(true);
		when(config.coxSpellRequirement()).thenReturn(CoxSpellRequirement.THRALLS);

		MenuEntryAdded event = mock(MenuEntryAdded.class);
		when(event.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION.getId());
		when(event.getIdentifier()).thenReturn(BossLocations.COX_OBJECT);

		plugin.onMenuEntryAdded(event);

		assertNull("no validation should occur during an active raid", getField(plugin, "lastResult"));
		verify(validator, never()).hasResurrectGreaterGhostRunes();
		verify(menu, never()).setMenuEntries(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testReleaseLockEntryUnlocksReloadObject() throws Exception
	{
		// Issue #15: with the reload block active the object has no game options left, so the
		// only way out must be the plugin's own menu entry.
		setField(plugin, "coxScoutingRaidGood", true);

		Layout layout = mock(Layout.class);
		when(layout.toCodeString()).thenReturn("fsccppcscf");
		Raid raid = mock(Raid.class);
		when(raid.getLayout()).thenReturn(layout);
		setField(plugin, "currentCoxRaid", raid);

		MenuEntry reloadEntry = mock(MenuEntry.class);
		when(reloadEntry.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION);
		when(reloadEntry.getIdentifier()).thenReturn(BossLocations.COX_RELOAD_OBJECT);
		MenuEntry walkEntry = mock(MenuEntry.class);
		when(walkEntry.getType()).thenReturn(MenuAction.WALK);
		when(menu.getMenuEntries()).thenReturn(new MenuEntry[]{walkEntry, reloadEntry});

		MenuEntry releaseEntry = mock(MenuEntry.class, RETURNS_SELF);
		when(menu.createMenuEntry(0)).thenReturn(releaseEntry);

		MenuEntryAdded event = mock(MenuEntryAdded.class);
		when(event.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION.getId());
		when(event.getIdentifier()).thenReturn(BossLocations.COX_RELOAD_OBJECT);

		plugin.onMenuEntryAdded(event);

		verify(menu).setMenuEntries(any());
		ArgumentCaptor<Consumer<MenuEntry>> onClick = ArgumentCaptor.forClass(Consumer.class);
		verify(releaseEntry).onClick(onClick.capture());

		onClick.getValue().accept(releaseEntry);

		assertFalse("clicking release must lift the block", (boolean) getField(plugin, "coxScoutingRaidGood"));

		// A re-scout of the same raid must not silently re-lock it.
		when(config.coxScoutingEnabled()).thenReturn(true);
		when(config.coxScoutWhitelistedRooms()).thenReturn("");
		when(config.coxScoutWhitelistedLayouts()).thenReturn("fsccppcscf");
		invokePrivate(plugin, "evaluateCoxScoutingRaid");

		assertFalse("released raid must stay unlocked", (boolean) getField(plugin, "coxScoutingRaidGood"));
	}

	@Test
	public void testReleaseLockEntryNotAddedTwiceForSameMenu() throws Exception
	{
		setField(plugin, "coxScoutingRaidGood", true);

		MenuEntry existing = mock(MenuEntry.class);
		when(existing.getOption()).thenReturn("Release raid lock");
		when(existing.getType()).thenReturn(MenuAction.RUNELITE);
		when(menu.getMenuEntries()).thenReturn(new MenuEntry[]{existing});

		MenuEntryAdded event = mock(MenuEntryAdded.class);
		when(event.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION.getId());
		when(event.getIdentifier()).thenReturn(BossLocations.COX_RELOAD_OBJECT);

		plugin.onMenuEntryAdded(event);

		verify(menu, never()).createMenuEntry(anyInt());
	}

	@Test
	public void testOnMenuEntryAddedStillSwapsInCoxLobbyBeforeRaidStarts() throws Exception
	{
		// Same object ID, raid not started: the entrance swap must still fire.
		setField(plugin, "currentLocation", BossLocations.CHAMBERS_OF_XERIC);
		setField(plugin, "lastResult", null);
		when(client.getVarbitValue(VarbitID.RAIDS_CLIENT_PROGRESS)).thenReturn(0);

		when(config.coxEnabled()).thenReturn(true);
		when(config.coxSpellRequirement()).thenReturn(CoxSpellRequirement.THRALLS);
		when(validator.hasResurrectGreaterGhostRunes()).thenReturn(false);

		MenuEntry walkHere = mock(MenuEntry.class);
		when(walkHere.getType()).thenReturn(MenuAction.WALK);
		MenuEntry enter = mock(MenuEntry.class);
		when(enter.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION);
		when(menu.getMenuEntries()).thenReturn(new MenuEntry[]{walkHere, enter});

		MenuEntryAdded event = mock(MenuEntryAdded.class);
		when(event.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION.getId());
		when(event.getIdentifier()).thenReturn(BossLocations.COX_OBJECT);

		plugin.onMenuEntryAdded(event);

		ArgumentCaptor<MenuEntry[]> captor = ArgumentCaptor.forClass(MenuEntry[].class);
		verify(menu).setMenuEntries(captor.capture());
		MenuEntry[] reordered = captor.getValue();
		assertSame("Walk Here should be the top option", walkHere, reordered[reordered.length - 1]);
	}

	// Helper methods for reflection
	private void setField(Object target, String fieldName, Object value) throws Exception
	{
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private void invokePrivate(Object target, String methodName) throws Exception
	{
		Method method = target.getClass().getDeclaredMethod(methodName);
		method.setAccessible(true);
		method.invoke(target);
	}

	@SuppressWarnings("unchecked")
	private <T> T getField(Object target, String fieldName) throws Exception
	{
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(target);
	}
}
