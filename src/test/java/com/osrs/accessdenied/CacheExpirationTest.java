package com.osrs.accessdenied;

import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Tests for validation cache expiration behavior.
 */
public class CacheExpirationTest
{
	@Mock
	private Client client;

	@Mock
	private AccessDeniedConfig config;

	@Mock
	private PlayerStateValidator validator;

	@Mock
	private ClientThread clientThread;

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
	}

	@Test
	public void testCacheExpirationAfter30Seconds() throws Exception
	{
		// Set current location
		setField(plugin, "currentLocation", BossLocations.NEX);

		// Create a validation result and add to cache
		ValidationResult result = new ValidationResult(true, Collections.emptySet(), "Valid");
		Map<String, ValidationResult> validationCache = new HashMap<>();
		validationCache.put("nex", result);
		setField(plugin, "validationCache", validationCache);

		// Set timestamp to 31 seconds ago (expired)
		Map<String, Long> validationCacheTimestamp = new HashMap<>();
		long expiredTime = System.currentTimeMillis() - 31_000;
		validationCacheTimestamp.put("nex", expiredTime);
		setField(plugin, "validationCacheTimestamp", validationCacheTimestamp);

		// Call getCachedValidationResult
		Method getCachedValidationResult = plugin.getClass().getDeclaredMethod("getCachedValidationResult");
		getCachedValidationResult.setAccessible(true);
		ValidationResult cachedResult = (ValidationResult) getCachedValidationResult.invoke(plugin);

		// Should return null because cache expired
		assertNull(cachedResult);

		// Verify cache was cleared
		Map<String, ValidationResult> cacheAfter = getField(plugin, "validationCache");
		assertFalse(cacheAfter.containsKey("nex"));

		Map<String, Long> timestampAfter = getField(plugin, "validationCacheTimestamp");
		assertFalse(timestampAfter.containsKey("nex"));
	}

	@Test
	public void testCacheValidWithin30Seconds() throws Exception
	{
		// Set current location
		setField(plugin, "currentLocation", BossLocations.NEX);

		// Create a validation result and add to cache
		ValidationResult result = new ValidationResult(true, Collections.emptySet(), "Valid");
		Map<String, ValidationResult> validationCache = new HashMap<>();
		validationCache.put("nex", result);
		setField(plugin, "validationCache", validationCache);

		// Set timestamp to 10 seconds ago (still valid)
		Map<String, Long> validationCacheTimestamp = new HashMap<>();
		long recentTime = System.currentTimeMillis() - 10_000;
		validationCacheTimestamp.put("nex", recentTime);
		setField(plugin, "validationCacheTimestamp", validationCacheTimestamp);

		// Call getCachedValidationResult
		Method getCachedValidationResult = plugin.getClass().getDeclaredMethod("getCachedValidationResult");
		getCachedValidationResult.setAccessible(true);
		ValidationResult cachedResult = (ValidationResult) getCachedValidationResult.invoke(plugin);

		// Should return the cached result
		assertNotNull(cachedResult);
		assertTrue(cachedResult.isValid());
		assertEquals("Valid", cachedResult.getFeedbackMessage());
	}

	@Test
	public void testCacheReturnsNullWhenNoCache() throws Exception
	{
		// Set current location
		setField(plugin, "currentLocation", BossLocations.NEX);

		// Empty cache
		setField(plugin, "validationCache", new HashMap<>());
		setField(plugin, "validationCacheTimestamp", new HashMap<>());

		// Call getCachedValidationResult
		Method getCachedValidationResult = plugin.getClass().getDeclaredMethod("getCachedValidationResult");
		getCachedValidationResult.setAccessible(true);
		ValidationResult cachedResult = (ValidationResult) getCachedValidationResult.invoke(plugin);

		// Should return null
		assertNull(cachedResult);
	}

	@Test
	public void testCacheReturnsNullWhenNoTimestamp() throws Exception
	{
		// Set current location
		setField(plugin, "currentLocation", BossLocations.NEX);

		// Create a validation result but no timestamp
		ValidationResult result = new ValidationResult(true, Collections.emptySet(), "Valid");
		Map<String, ValidationResult> validationCache = new HashMap<>();
		validationCache.put("nex", result);
		setField(plugin, "validationCache", validationCache);
		setField(plugin, "validationCacheTimestamp", new HashMap<>());

		// Call getCachedValidationResult
		Method getCachedValidationResult = plugin.getClass().getDeclaredMethod("getCachedValidationResult");
		getCachedValidationResult.setAccessible(true);
		ValidationResult cachedResult = (ValidationResult) getCachedValidationResult.invoke(plugin);

		// Should return null and clear cache
		assertNull(cachedResult);

		Map<String, ValidationResult> cacheAfter = getField(plugin, "validationCache");
		assertFalse(cacheAfter.containsKey("nex"));
	}

	@Test
	public void testCacheReturnsNullWhenNoCurrentLocation() throws Exception
	{
		// No current location
		setField(plugin, "currentLocation", null);

		// Call getCachedValidationResult
		Method getCachedValidationResult = plugin.getClass().getDeclaredMethod("getCachedValidationResult");
		getCachedValidationResult.setAccessible(true);
		ValidationResult cachedResult = (ValidationResult) getCachedValidationResult.invoke(plugin);

		// Should return null
		assertNull(cachedResult);
	}

	@Test
	public void testCacheExpirationExactly30Seconds() throws Exception
	{
		// Set current location
		setField(plugin, "currentLocation", BossLocations.NEX);

		// Create a validation result and add to cache
		ValidationResult result = new ValidationResult(true, Collections.emptySet(), "Valid");
		Map<String, ValidationResult> validationCache = new HashMap<>();
		validationCache.put("nex", result);
		setField(plugin, "validationCache", validationCache);

		// Set timestamp to exactly 30 seconds ago (boundary case)
		Map<String, Long> validationCacheTimestamp = new HashMap<>();
		long boundaryTime = System.currentTimeMillis() - 30_000;
		validationCacheTimestamp.put("nex", boundaryTime);
		setField(plugin, "validationCacheTimestamp", validationCacheTimestamp);

		// Call getCachedValidationResult
		Method getCachedValidationResult = plugin.getClass().getDeclaredMethod("getCachedValidationResult");
		getCachedValidationResult.setAccessible(true);
		ValidationResult cachedResult = (ValidationResult) getCachedValidationResult.invoke(plugin);

		// At exactly 30 seconds, should still be valid (not expired)
		assertNotNull(cachedResult);
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
