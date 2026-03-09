package com.osrs.accessdenied;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.HashMap;
import java.util.Map;

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

	@Inject
	private ClientThread clientThread;

	// Current location tracking
	private BossLocation currentLocation;
	private int[] currentRegions;

	// Validation state per location
	private final Map<String, ValidationResult> validationCache = new HashMap<>();
	private final Map<String, Long> validationCacheTimestamp = new HashMap<>();
	private final Map<String, Boolean> menuModifiedState = new HashMap<>();
	
	// Cache expiration time in milliseconds (30 seconds)
	private static final long CACHE_EXPIRATION_MS = 30_000;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Access Denied started!");
		// Reset state on startup
		currentLocation = null;
		currentRegions = null;
		validationCache.clear();
		validationCacheTimestamp.clear();
		menuModifiedState.clear();
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Access Denied stopped!");
		// Clear state on shutdown
		currentLocation = null;
		currentRegions = null;
		validationCache.clear();
		validationCacheTimestamp.clear();
		menuModifiedState.clear();
	}

	/**
	 * Detect when the player enters a new region and validate if it's a boss location.
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// Only process on logged in or loading states
		if (event.getGameState() != GameState.LOGGED_IN && event.getGameState() != GameState.LOADING)
		{
			return;
		}

		// Ensure player exists
		if (client.getLocalPlayer() == null)
		{
			return;
		}

		// Get current regions
		int[] newRegions = client.getMapRegions();
		log.debug("Current regions: {}", java.util.Arrays.toString(newRegions));

		// Check if regions have changed
		if (regionsEqual(currentRegions, newRegions))
		{
			return;
		}

		// Update regions and check for location change
		currentRegions = newRegions;
		BossLocation newLocation = BossLocations.findByAnyRegion(newRegions);

		if (newLocation == currentLocation)
		{
			return;
		}

		// Location changed
		log.debug("Region changed - Old location: {}, New location: {}", 
			currentLocation != null ? currentLocation.getDisplayName() : "none",
			newLocation != null ? newLocation.getDisplayName() : "none");

		currentLocation = newLocation;

		// Validate if we entered a boss location
		if (currentLocation != null)
		{
			log.debug("Entered {} region, performing validation", currentLocation.getDisplayName());
			validateCurrentLocation();
		}
	}

	/**
	 * Listen for item container changes to detect when items are added/removed.
	 * This handles:
	 * - Book of the Dead changes (for thralls)
	 * - Runes added/removed directly to inventory (not in rune pouch)
	 * 
	 * Note: Rune pouch changes are handled by onVarbitChanged for better efficiency.
	 */
	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		// Only revalidate if we're currently in a boss location
		if (currentLocation == null)
		{
			return;
		}

		// Check if validation is required for this location
		if (!isValidationRequired(currentLocation))
		{
			return;
		}

		// Only care about inventory changes
		int containerId = event.getContainerId();
		if (containerId != InventoryID.INV)
		{
			return;
		}

		log.debug("Inventory changed while in {} region, revalidating", 
			currentLocation.getDisplayName());
		
		// Clear the menu modified state so the message can be shown again if validation state changed
		menuModifiedState.remove(currentLocation.getId());
		
		// Schedule validation on client thread to avoid race conditions
		clientThread.invokeLater(this::validateCurrentLocation);
	}

	/**
	 * Listen for varbit changes to detect when rune pouch contents or spellbook are updated.
	 * This is the primary mechanism for detecting rune-related changes.
	 * 
	 * Handles:
	 * - Rune pouch contents changes (adding/removing runes)
	 * - Spellbook changes (switching between spellbooks)
	 * - Rune pouch being deposited/withdrawn (varbits clear/populate)
	 */
	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// Only revalidate if we're currently in a boss location
		if (currentLocation == null)
		{
			return;
		}

		// Check if validation is required for this location
		if (!isValidationRequired(currentLocation))
		{
			return;
		}

		// Only revalidate if the changed varbit is related to rune pouch or spellbook
		int varbitId = event.getVarbitId();
		if (!isRunePouchVarbit(varbitId))
		{
			return;
		}

		log.debug("Rune pouch/spellbook varbit {} changed while in {} region, revalidating", 
			varbitId, currentLocation.getDisplayName());
		
		// Schedule validation on client thread to avoid race conditions
		clientThread.invokeLater(this::validateCurrentLocation);
	}

	/**
	 * Listen for config changes to revalidate when the user enables/disables requirements.
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// Only care about our config group
		if (!"accessdenied".equals(event.getGroup()))
		{
			return;
		}

		// Check for invalid configuration (location enabled but no requirements set)
		validateConfiguration(event.getKey());

		// Only revalidate if we're currently in a boss location
		if (currentLocation == null)
		{
			return;
		}

		// Check if the config change affects the current location
		boolean shouldRevalidate = false;
		String locationId = currentLocation.getId();
		String configKey = event.getKey();
		
		switch (locationId)
		{
			case "nex":
				shouldRevalidate = "nexEnabled".equals(configKey)
					|| "nexRequireSpell".equals(configKey) 
					|| "nexRequireDeathCharge".equals(configKey);
				break;
			case "tob":
				shouldRevalidate = "tobEnabled".equals(configKey)
					|| "tobRequireSpell".equals(configKey) 
					|| "tobRequireDeathCharge".equals(configKey);
				break;
			case "toa":
				shouldRevalidate = "toaEnabled".equals(configKey)
					|| "toaRequireSpell".equals(configKey) 
					|| "toaRequireDeathCharge".equals(configKey);
				break;
			case "cox":
				shouldRevalidate = "coxEnabled".equals(configKey)
					|| "coxRequireSpell".equals(configKey) 
					|| "coxRequireDeathCharge".equals(configKey);
				break;
		}

		if (shouldRevalidate)
		{
			handleConfigRevalidation();
		}
	}

	/**
	 * Validate configuration to ensure at least one requirement is enabled when a location is enabled.
	 * Shows a warning message if configuration is invalid.
	 * 
	 * @param configKey The configuration key that was changed
	 */
	private void validateConfiguration(String configKey)
	{
		// Check if a master toggle was just enabled
		boolean isMasterToggle = configKey.endsWith("Enabled");
		if (!isMasterToggle)
		{
			return;
		}

		// Determine which location and check if it has any requirements enabled
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

		// Show warning if location is enabled but no requirements are set
		if (locationName != null && !hasRequirements)
		{
			String warningMessage = String.format(
				"<col=ff0000>Warning: %s validation is enabled but no requirements are configured. " +
				"Enable at least one requirement (Thralls or Death Charge) for validation to work.</col>",
				locationName
			);
			
			client.addChatMessage(
				ChatMessageType.GAMEMESSAGE,
				"",
				warningMessage,
				null
			);
			
			log.debug("Configuration warning: {} enabled with no requirements", locationName);
		}
	}

	/**
	 * Handle revalidation when config changes.
	 */
	private void handleConfigRevalidation()
	{
		log.debug("{} requirement config changed, clearing cache and revalidating", 
			currentLocation.getDisplayName());
		
		// Clear the cached validation result and timestamp
		validationCache.remove(currentLocation.getId());
		validationCacheTimestamp.remove(currentLocation.getId());
		
		// Clear the menu modified state so the message can be shown again if needed
		menuModifiedState.remove(currentLocation.getId());
		
		// Schedule validation to run on the client thread (required for client API calls)
		clientThread.invokeLater(() -> {
			validateCurrentLocation();
			
			// Log the new validation state
			ValidationResult result = validationCache.get(currentLocation.getId());
			if (result != null)
			{
				log.debug("New validation state - Valid: {}, Message: {}", 
					result.isValid(), result.getFeedbackMessage());
			}
		});
	}

	/**
	 * Check if a varbit ID is related to rune pouch contents or spellbook.
	 */
	private boolean isRunePouchVarbit(int varbitId)
	{
		return varbitId == VarbitID.RUNE_POUCH_TYPE_1
			|| varbitId == VarbitID.RUNE_POUCH_TYPE_2
			|| varbitId == VarbitID.RUNE_POUCH_TYPE_3
			|| varbitId == VarbitID.RUNE_POUCH_TYPE_4
			|| varbitId == VarbitID.RUNE_POUCH_QUANTITY_1
			|| varbitId == VarbitID.RUNE_POUCH_QUANTITY_2
			|| varbitId == VarbitID.RUNE_POUCH_QUANTITY_3
			|| varbitId == VarbitID.RUNE_POUCH_QUANTITY_4
			|| varbitId == Varbits.SPELLBOOK;
	}

	/**
	 * Intercept menu entries to modify them based on cached validation results.
	 * Optimized to only process validated objects in boss locations.
	 */
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		// Early exit: Only process if we're in a boss location
		if (currentLocation == null)
		{
			return;
		}

		// Early exit: Check if validation is required for this location
		if (!isValidationRequired(currentLocation))
		{
			return;
		}

		// Early exit: Filter for game object interactions only
		int eventType = event.getType();
		if (eventType != MenuAction.GAME_OBJECT_FIRST_OPTION.getId()
			&& eventType != MenuAction.GAME_OBJECT_SECOND_OPTION.getId())
		{
			return;
		}

		// Early exit: Check if this specific object requires validation
		int objectId = event.getIdentifier();
		Integer validatedObjectId = BossLocations.getObjectForLocation(currentLocation);
		if (validatedObjectId == null || validatedObjectId != objectId)
		{
			return;
		}

		// At this point, we know we're hovering over a validated object
		// Get cached validation result (should already exist from region entry)
		ValidationResult validationResult = getCachedValidationResult();
		
		if (validationResult == null)
		{
			// Cache expired or doesn't exist, validate now
			log.debug("No valid cached result for {}, validating now", currentLocation.getDisplayName());
			validateCurrentLocation();
			validationResult = validationCache.get(currentLocation.getId());
			
			if (validationResult == null)
			{
				log.error("Still no validation result after validating, this shouldn't happen");
				return;
			}
		}

		// Only modify menu if validation failed
		if (validationResult.isValid())
		{
			// Validation passed - reset menu modified state
			menuModifiedState.remove(currentLocation.getId());
			return;
		}

		// Validation failed - deprioritize the entrance interaction
		MenuEntry[] menuEntries = client.getMenuEntries();
		if (menuEntries == null || menuEntries.length == 0)
		{
			return;
		}

		// Find and move "Walk here" to the end (default left-click position)
		reorderMenuToWalkHere(menuEntries);

		// Display feedback message once per validation state
		if (!Boolean.TRUE.equals(menuModifiedState.get(currentLocation.getId())))
		{
			String message = validationResult.getFeedbackMessage();
			if (message != null && !message.isEmpty())
			{
				client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					message,
					null
				);
			}
			menuModifiedState.put(currentLocation.getId(), true);
		}
	}

	/**
	 * Reorder menu entries to make "Walk here" the default left-click action.
	 * This effectively deprioritizes other interactions when validation fails.
	 * 
	 * The menu system in RuneLite uses the last entry as the default left-click action.
	 * By moving "Walk here" to the end, we prevent accidental boss entrance clicks
	 * when the player doesn't meet requirements.
	 * 
	 * @param menuEntries The current menu entries
	 */
	private void reorderMenuToWalkHere(MenuEntry[] menuEntries)
	{
		// Find the "Walk here" entry
		int walkHereIndex = -1;
		for (int i = 0; i < menuEntries.length; i++)
		{
			if (menuEntries[i].getType() == MenuAction.WALK)
			{
				walkHereIndex = i;
				break;
			}
		}

		// If "Walk here" not found or already at the end, nothing to do
		if (walkHereIndex == -1 || walkHereIndex == menuEntries.length - 1)
		{
			return;
		}

		// Move "Walk here" to the end by shifting other entries
		MenuEntry walkHereEntry = menuEntries[walkHereIndex];
		MenuEntry[] reorderedEntries = new MenuEntry[menuEntries.length];
		
		int newIndex = 0;
		for (int i = 0; i < menuEntries.length; i++)
		{
			if (i != walkHereIndex)
			{
				reorderedEntries[newIndex++] = menuEntries[i];
			}
		}
		reorderedEntries[menuEntries.length - 1] = walkHereEntry;
		
		client.setMenuEntries(reorderedEntries);
	}

	/**
	 * Get cached validation result if it exists and hasn't expired.
	 * 
	 * The cache prevents redundant validation checks when hovering over the same
	 * entrance multiple times. Cache expires after 30 seconds to ensure validation
	 * stays reasonably up-to-date with player state changes.
	 * 
	 * @return ValidationResult if valid cache exists, null otherwise
	 */
	private ValidationResult getCachedValidationResult()
	{
		if (currentLocation == null)
		{
			return null;
		}

		String locationId = currentLocation.getId();
		ValidationResult cached = validationCache.get(locationId);
		
		if (cached == null)
		{
			return null;
		}

		// Check if cache has expired
		Long timestamp = validationCacheTimestamp.get(locationId);
		if (timestamp == null)
		{
			// No timestamp, cache is invalid
			validationCache.remove(locationId);
			return null;
		}

		long age = System.currentTimeMillis() - timestamp;
		if (age > CACHE_EXPIRATION_MS)
		{
			// Cache expired
			log.debug("Validation cache expired for {} (age: {}ms)", locationId, age);
			validationCache.remove(locationId);
			validationCacheTimestamp.remove(locationId);
			return null;
		}

		return cached;
	}

	/**
	 * Validate requirements for the current location.
	 * This delegates to location-specific validation methods.
	 */
	private void validateCurrentLocation()
	{
		if (currentLocation == null)
		{
			return;
		}

		log.debug("=== Validating requirements for {} ===", currentLocation.getDisplayName());

		// Check if validation is required for this location
		boolean validationRequired = isValidationRequired(currentLocation);
		log.debug("Validation required: {}", validationRequired);

		if (!validationRequired)
		{
			log.debug("No validation required for {}, allowing entry", currentLocation.getDisplayName());
			ValidationResult allowResult = new ValidationResult(
				true, 
				java.util.Collections.emptySet(), 
				"No requirements configured"
			);
			validationCache.put(currentLocation.getId(), allowResult);
			validationCacheTimestamp.put(currentLocation.getId(), System.currentTimeMillis());
			return;
		}

		// Delegate to location-specific validation
		ValidationResult validationResult = validateLocationRequirements(currentLocation);

		// Cache the result with timestamp
		validationCache.put(currentLocation.getId(), validationResult);
		validationCacheTimestamp.put(currentLocation.getId(), System.currentTimeMillis());
		log.debug("=== Validation complete, result cached ===");
	}

	/**
	 * Validate requirements for a specific location.
	 * This method delegates to location-specific validation logic.
	 * 
	 * @param location The location to validate
	 * @return ValidationResult indicating whether requirements are met
	 */
	private ValidationResult validateLocationRequirements(BossLocation location)
	{
		log.debug("Delegating validation for location: {}", location.getId());
		
		// Delegate to location-specific validation methods
		switch (location.getId())
		{
			case "nex":
				log.debug("Validating Nex requirements");
				return validateRaidRequirements(
					location,
					config.nexRequireSpell(),
					config.nexRequireDeathCharge()
				);
			
			case "tob":
				log.debug("Validating Theatre of Blood requirements");
				return validateRaidRequirements(
					location,
					config.tobRequireSpell(),
					config.tobRequireDeathCharge()
				);
			
			case "toa":
				log.debug("Validating Tombs of Amascut requirements");
				return validateRaidRequirements(
					location,
					config.toaRequireSpell(),
					config.toaRequireDeathCharge()
				);
			
			case "cox":
				log.debug("Validating Chambers of Xeric requirements");
				return validateRaidRequirements(
					location,
					config.coxRequireSpell(),
					config.coxRequireDeathCharge()
				);
			
			default:
				log.warn("No validation logic implemented for location: {}", location.getId());
				return new ValidationResult(
					true,
					java.util.Collections.emptySet(),
					"No validation logic implemented"
				);
		}
	}

	/**
	 * Validate raid requirements (generic for all raids with thralls/death charge).
	 * Can require either Resurrect Greater Ghost spell or Death Charge spell (or both).
	 * - Resurrect Greater Ghost requires: 4 Soul runes, 2 Blood runes, 1 Cosmic rune, Book of the Dead, Arceuus spellbook
	 * - Death Charge requires: 1 Death rune, 1 Blood rune, 1 Soul rune, Arceuus spellbook
	 * Aether runes count as both Soul and Cosmic runes.
	 * 
	 * @param location The location being validated
	 * @param requireThralls Whether to require Resurrect Greater Ghost
	 * @param requireDeathCharge Whether to require Death Charge
	 * @return ValidationResult indicating whether requirements are met
	 */
	private ValidationResult validateRaidRequirements(BossLocation location, boolean requireThralls, boolean requireDeathCharge)
	{
		log.debug("Checking {} requirements - Thralls: {}, Death Charge: {}", 
			location.getDisplayName(), requireThralls, requireDeathCharge);

		// Check spellbook (required for both)
		boolean hasSpellbook = playerStateValidator.isOnArceuusSpellbook();
		log.debug("On Arceuus spellbook: {}", hasSpellbook);

		// Track what's missing
		java.util.List<String> missing = new java.util.ArrayList<>();

		// Check thrall requirements if enabled
		boolean thrallsValid = true;
		boolean hasBook = false;
		if (requireThralls)
		{
			boolean hasThralRunes = playerStateValidator.hasResurrectGreaterGhostRunes();
			hasBook = playerStateValidator.hasBookOfTheDead();

			log.debug("Thralls - Has runes: {}, Has Book: {}", hasThralRunes, hasBook);

			if (!hasThralRunes)
			{
				missing.add("runes for Resurrect Greater Ghost");
				thrallsValid = false;
			}
			if (!hasBook)
			{
				missing.add("Book of the Dead");
				thrallsValid = false;
			}
		}

		// Check death charge requirements if enabled
		boolean deathChargeValid = true;
		if (requireDeathCharge)
		{
			boolean hasDeathChargeRunes = playerStateValidator.hasDeathChargeRunes();
			log.debug("Death Charge - Has runes: {}", hasDeathChargeRunes);

			if (!hasDeathChargeRunes)
			{
				missing.add("runes for Death Charge");
				deathChargeValid = false;
			}
		}

		// Check spellbook
		if (!hasSpellbook)
		{
			missing.add("Arceuus spellbook");
		}

		// Determine if validation passed
		boolean allValid = hasSpellbook;
		if (requireThralls)
		{
			allValid = allValid && thrallsValid;
		}
		if (requireDeathCharge)
		{
			allValid = allValid && deathChargeValid;
		}

		if (allValid)
		{
			log.debug("Validation PASSED for {}: Has all requirements", location.getDisplayName());
			return new ValidationResult(
				true,
				java.util.Collections.emptySet(),
				"All requirements met"
			);
		}
		else
		{
			// Build specific failure message
			String failureMessage = "Missing: " + String.join(", ", missing);

			log.debug("Validation FAILED for {}: {}", location.getDisplayName(), failureMessage);
			return new ValidationResult(
				false,
				java.util.Collections.singleton(failureMessage),
				failureMessage
			);
		}
	}

	/**
	 * Check if validation is required for a specific location based on config.
	 * Validation is required when:
	 * 1. The location's master toggle is enabled, AND
	 * 2. At least one specific requirement (thralls or death charge) is enabled
	 * 
	 * This prevents pointless validation when a location is enabled but no requirements are set.
	 * 
	 * @param location The location to check
	 * @return true if validation should be performed, false otherwise
	 */
	private boolean isValidationRequired(BossLocation location)
	{
		if (location == null)
		{
			return false;
		}

		// Check config for each location (master toggle AND specific requirements)
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
			default:
				return false;
		}
	}

	/**
	 * Check if two region arrays are equal.
	 * Uses Set comparison to handle regions in any order.
	 * 
	 * This is important because the client may return regions in different orders
	 * depending on the player's position, but the actual set of regions is what matters.
	 * 
	 * @param regions1 First region array
	 * @param regions2 Second region array
	 * @return true if both arrays contain the same regions (order-independent)
	 */
	private boolean regionsEqual(int[] regions1, int[] regions2)
	{
		if (regions1 == null && regions2 == null)
		{
			return true;
		}
		if (regions1 == null || regions2 == null)
		{
			return false;
		}
		if (regions1.length != regions2.length)
		{
			return false;
		}
		
		// Convert to sets for order-independent comparison
		java.util.Set<Integer> set1 = new java.util.HashSet<>();
		java.util.Set<Integer> set2 = new java.util.HashSet<>();
		
		for (int region : regions1)
		{
			set1.add(region);
		}
		for (int region : regions2)
		{
			set2.add(region);
		}
		
		return set1.equals(set2);
	}

	@Provides
	AccessDeniedConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AccessDeniedConfig.class);
	}
}

