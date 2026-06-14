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
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
		when(config.coxRequireSpell()).thenReturn(false);
		when(config.coxRequireDeathCharge()).thenReturn(false);
		when(config.coxRequireHumidify()).thenReturn(true);
		when(config.coxRequireVengeance()).thenReturn(false);

		Method isValidationRequired = plugin.getClass().getDeclaredMethod("isValidationRequired", BossLocation.class);
		isValidationRequired.setAccessible(true);

		boolean result = (boolean) isValidationRequired.invoke(plugin, BossLocations.CHAMBERS_OF_XERIC);
		assertTrue(result);
	}

	@Test
	public void testIsValidationRequiredWithCoxVengeance() throws Exception
	{
		when(config.coxEnabled()).thenReturn(true);
		when(config.coxRequireSpell()).thenReturn(false);
		when(config.coxRequireDeathCharge()).thenReturn(false);
		when(config.coxRequireHumidify()).thenReturn(false);
		when(config.coxRequireVengeance()).thenReturn(true);

		Method isValidationRequired = plugin.getClass().getDeclaredMethod("isValidationRequired", BossLocation.class);
		isValidationRequired.setAccessible(true);

		boolean result = (boolean) isValidationRequired.invoke(plugin, BossLocations.CHAMBERS_OF_XERIC);
		assertTrue(result);
	}

	@Test
	public void testIsValidationRequiredWithCoxNoRequirements() throws Exception
	{
		when(config.coxEnabled()).thenReturn(true);
		when(config.coxRequireSpell()).thenReturn(false);
		when(config.coxRequireDeathCharge()).thenReturn(false);
		when(config.coxRequireHumidify()).thenReturn(false);
		when(config.coxRequireVengeance()).thenReturn(false);

		Method isValidationRequired = plugin.getClass().getDeclaredMethod("isValidationRequired", BossLocation.class);
		isValidationRequired.setAccessible(true);

		boolean result = (boolean) isValidationRequired.invoke(plugin, BossLocations.CHAMBERS_OF_XERIC);
		assertFalse(result);
	}

	@Test
	public void testOnConfigChangedBlocksConflictingCoxLunarToggle()
	{
		// Arceuus spell already required — enabling a Lunar spell must be reverted
		when(config.coxRequireSpell()).thenReturn(true);
		when(config.coxRequireDeathCharge()).thenReturn(false);

		ConfigChanged event = mock(ConfigChanged.class);
		when(event.getGroup()).thenReturn("accessdenied");
		when(event.getKey()).thenReturn("coxRequireHumidify");
		when(event.getNewValue()).thenReturn("true");

		plugin.onConfigChanged(event);

		verify(configManager).setConfiguration("accessdenied", "coxRequireHumidify", false);

		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(client).addChatMessage(
			eq(ChatMessageType.GAMEMESSAGE),
			eq(""),
			messageCaptor.capture(),
			isNull()
		);

		String message = messageCaptor.getValue();
		assertTrue(message.contains("conflicting spellbook"));
		assertTrue(message.contains("Chambers of Xeric"));
	}

	@Test
	public void testOnConfigChangedBlocksConflictingCoxArceuusToggle()
	{
		// Lunar spell already required — enabling an Arceuus spell must be reverted
		when(config.coxRequireHumidify()).thenReturn(false);
		when(config.coxRequireVengeance()).thenReturn(true);

		ConfigChanged event = mock(ConfigChanged.class);
		when(event.getGroup()).thenReturn("accessdenied");
		when(event.getKey()).thenReturn("coxRequireSpell");
		when(event.getNewValue()).thenReturn("true");

		plugin.onConfigChanged(event);

		verify(configManager).setConfiguration("accessdenied", "coxRequireSpell", false);
	}

	@Test
	public void testOnConfigChangedAllowsLunarToggleWhenNoArceuusEnabled()
	{
		when(config.coxRequireSpell()).thenReturn(false);
		when(config.coxRequireDeathCharge()).thenReturn(false);

		ConfigChanged event = mock(ConfigChanged.class);
		when(event.getGroup()).thenReturn("accessdenied");
		when(event.getKey()).thenReturn("coxRequireHumidify");
		when(event.getNewValue()).thenReturn("true");

		plugin.onConfigChanged(event);

		verify(configManager, never()).setConfiguration(anyString(), anyString(), any());
		verify(client, never()).addChatMessage(any(), any(), any(), any());
	}

	@Test
	public void testOnConfigChangedAllowsDisablingToggleDespiteConflict()
	{
		// Turning a toggle OFF is never blocked, even when the opposite group is enabled
		when(config.coxRequireSpell()).thenReturn(true);

		ConfigChanged event = mock(ConfigChanged.class);
		when(event.getGroup()).thenReturn("accessdenied");
		when(event.getKey()).thenReturn("coxRequireHumidify");
		when(event.getNewValue()).thenReturn("false");

		plugin.onConfigChanged(event);

		verify(configManager, never()).setConfiguration(anyString(), anyString(), any());
		verify(client, never()).addChatMessage(any(), any(), any(), any());
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
		// An active CoX raid short-circuits the on-demand block entirely.
		setField(plugin, "currentLocation", BossLocations.NEX);
		setField(plugin, "lastResult", null);
		setField(plugin, "coxRaidActive", true);

		when(config.nexEnabled()).thenReturn(true);
		when(config.nexRequireSpell()).thenReturn(true);

		MenuEntryAdded event = mock(MenuEntryAdded.class);
		when(event.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION.getId());
		when(event.getIdentifier()).thenReturn(BossLocations.NEX_OBJECT);

		plugin.onMenuEntryAdded(event);

		assertNull("no validation should occur during an active raid", getField(plugin, "lastResult"));
		verify(validator, never()).hasResurrectGreaterGhostRunes();
		verify(menu, never()).setMenuEntries(any());
	}

	// Helper methods for reflection
	private void setField(Object target, String fieldName, Object value) throws Exception
	{
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	@SuppressWarnings("unchecked")
	private <T> T getField(Object target, String fieldName) throws Exception
	{
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(target);
	}
}
