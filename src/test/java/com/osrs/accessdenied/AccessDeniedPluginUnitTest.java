package com.osrs.accessdenied;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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
	private ClientThread clientThread;

	@Mock
	private ConfigManager configManager;

	@Mock
	private Player player;

	private AccessDeniedPlugin plugin;

	@Before
	public void setUp() throws Exception
	{
		MockitoAnnotations.openMocks(this);
		plugin = new AccessDeniedPlugin();

		// Inject mocked dependencies
		setField(plugin, "client", client);
		setField(plugin, "config", config);
		setField(plugin, "playerStateValidator", validator);
		setField(plugin, "clientThread", clientThread);

		// Setup default mocks
		when(client.getLocalPlayer()).thenReturn(player);
	}

	@Test
	public void testStartUpClearsState() throws Exception
	{
		plugin.startUp();

		// Verify state is cleared
		assertNull(getField(plugin, "currentLocation"));
		assertNull(getField(plugin, "currentRegions"));
		
		Map<String, ValidationResult> validationCache = getField(plugin, "validationCache");
		assertTrue(validationCache.isEmpty());
		
		Map<String, Long> validationCacheTimestamp = getField(plugin, "validationCacheTimestamp");
		assertTrue(validationCacheTimestamp.isEmpty());
		
		Map<String, Boolean> menuModifiedState = getField(plugin, "menuModifiedState");
		assertTrue(menuModifiedState.isEmpty());
	}

	@Test
	public void testShutDownClearsState() throws Exception
	{
		plugin.shutDown();

		// Verify state is cleared
		assertNull(getField(plugin, "currentLocation"));
		assertNull(getField(plugin, "currentRegions"));
		
		Map<String, ValidationResult> validationCache = getField(plugin, "validationCache");
		assertTrue(validationCache.isEmpty());
	}

	@Test
	public void testOnGameStateChangedIgnoresNonLoggedInStates()
	{
		GameStateChanged event = mock(GameStateChanged.class);
		when(event.getGameState()).thenReturn(GameState.LOGIN_SCREEN);

		plugin.onGameStateChanged(event);

		// Should not interact with client
		verify(client, never()).getMapRegions();
	}

	@Test
	public void testOnGameStateChangedIgnoresWhenNoPlayer()
	{
		GameStateChanged event = mock(GameStateChanged.class);
		when(event.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getLocalPlayer()).thenReturn(null);

		plugin.onGameStateChanged(event);

		// Should not proceed without player
		verify(client, never()).getMapRegions();
	}

	@Test
	public void testOnGameStateChangedDetectsRegionChange()
	{
		GameStateChanged event = mock(GameStateChanged.class);
		when(event.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getMapRegions()).thenReturn(new int[]{11601});

		plugin.onGameStateChanged(event);

		// Should detect region change
		verify(client, atLeastOnce()).getMapRegions();
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
	public void testOnConfigChangedIgnoresOtherGroups()
	{
		ConfigChanged event = mock(ConfigChanged.class);
		when(event.getGroup()).thenReturn("othergroup");
		when(event.getKey()).thenReturn("somekey");
		
		plugin.onConfigChanged(event);

		// Should not trigger any validation
		verify(clientThread, never()).invokeLater(any(Runnable.class));
	}

	@Test
	public void testOnMenuEntryAddedIgnoresWhenNotInBossLocation() throws Exception
	{
		setField(plugin, "currentLocation", null);

		MenuEntryAdded event = mock(MenuEntryAdded.class);
		when(event.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION.getId());

		plugin.onMenuEntryAdded(event);

		// Should not modify menu
		verify(client, never()).getMenuEntries();
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
