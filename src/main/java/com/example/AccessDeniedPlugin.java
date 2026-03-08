package com.example;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
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
	private final Map<String, Boolean> menuModifiedState = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Access Denied started!");
		// Reset state on startup
		currentLocation = null;
		currentRegions = null;
		validationCache.clear();
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
		menuModifiedState.clear();
	}

	/**
	 * Detect when the player enters a new region and validate if it's a boss location.
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN || event.getGameState() == GameState.LOADING)
		{
			// Update current regions
			if (client.getLocalPlayer() != null)
			{
				int[] newRegions = client.getMapRegions();
				log.debug("Current regions: {}", java.util.Arrays.toString(newRegions));
				
				// Check if we've changed regions
				if (!regionsEqual(currentRegions, newRegions))
				{
					currentRegions = newRegions;
					BossLocation newLocation = BossLocations.findByAnyRegion(newRegions);
					
					if (newLocation != currentLocation)
					{
						log.info("Region changed - Old location: {}, New location: {}", 
							currentLocation != null ? currentLocation.getDisplayName() : "none",
							newLocation != null ? newLocation.getDisplayName() : "none");
						
						currentLocation = newLocation;
						
						// If we entered a boss location, validate immediately
						if (currentLocation != null)
						{
							log.info("Entered {} region, performing validation", currentLocation.getDisplayName());
							validateCurrentLocation();
						}
					}
				}
			}
		}
	}

	/**
	 * Listen for item container changes to detect when runes are added/removed.
	 * This triggers revalidation when the player's inventory or equipment changes.
	 * Also listens for bank changes to detect when rune pouch is deposited.
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

		// Check for inventory or bank changes
		// Bank changes are important because depositing rune pouch clears the varbits
		int containerId = event.getContainerId();
		if (containerId == InventoryID.INV || containerId == InventoryID.BANK)
		{
			log.debug("Inventory/Bank changed (container: {}) while in {} region, revalidating", 
				containerId, currentLocation.getDisplayName());
			
			// Clear the menu modified state so the message can be shown again if validation state changed
			menuModifiedState.remove(currentLocation.getId());
			
			validateCurrentLocation();
		}
	}

	/**
	 * Listen for varbit changes to detect when rune pouch contents or spellbook are updated.
	 * This is the most reliable way to detect rune pouch and spellbook changes.
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
		validateCurrentLocation();
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

		// Only revalidate if we're currently in a boss location
		if (currentLocation == null)
		{
			return;
		}

		// If the config changed for the current location, clear cache and revalidate on client thread
		if ("nexRequireSpell".equals(event.getKey()) && "nex".equals(currentLocation.getId()))
		{
			log.info("Nex requirement config changed to: {}, clearing cache and revalidating", config.nexRequireSpell());
			
			// Clear the cached validation result immediately so menu entries will trigger revalidation
			validationCache.remove(currentLocation.getId());
			
			// Clear the menu modified state so the message can be shown again if needed
			menuModifiedState.remove(currentLocation.getId());
			
			// Schedule validation to run on the client thread (required for client API calls)
			clientThread.invokeLater(() -> {
				validateCurrentLocation();
				
				// Log the new validation state
				ValidationResult result = validationCache.get(currentLocation.getId());
				if (result != null)
				{
					log.info("New validation state - Valid: {}, Message: {}", 
						result.isValid(), result.getFeedbackMessage());
				}
			});
		}
	}

	/**
	 * Check if a varbit ID is related to rune pouch contents or spellbook.
	 */
	private boolean isRunePouchVarbit(int varbitId)
	{
		// Varbit 4070 tracks the current spellbook (0=Standard, 1=Ancient, 2=Lunar, 3=Arceuus)
		final int SPELLBOOK_VARBIT = 4070;
		
		return varbitId == VarbitID.RUNE_POUCH_TYPE_1
			|| varbitId == VarbitID.RUNE_POUCH_TYPE_2
			|| varbitId == VarbitID.RUNE_POUCH_TYPE_3
			|| varbitId == VarbitID.RUNE_POUCH_TYPE_4
			|| varbitId == VarbitID.RUNE_POUCH_QUANTITY_1
			|| varbitId == VarbitID.RUNE_POUCH_QUANTITY_2
			|| varbitId == VarbitID.RUNE_POUCH_QUANTITY_3
			|| varbitId == VarbitID.RUNE_POUCH_QUANTITY_4
			|| varbitId == SPELLBOOK_VARBIT;
	}

	/**
	 * Intercept menu entries to modify them based on cached validation results.
	 * This runs on every mouseover but uses cached validation results.
	 */
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		// Only process if we're in a boss location
		if (currentLocation == null)
		{
			return;
		}

		// Check if validation is required for this location
		if (!isValidationRequired(currentLocation))
		{
			return;
		}

		// Filter for game object interactions
		if (event.getType() != MenuAction.GAME_OBJECT_FIRST_OPTION.getId()
			&& event.getType() != MenuAction.GAME_OBJECT_SECOND_OPTION.getId())
		{
			return;
		}

		int objectId = event.getIdentifier();
		
		// Check if this object requires validation in the current location
		if (!BossLocations.isValidatedObject(currentLocation, objectId))
		{
			return;
		}

		// Get cached validation result for this location
		ValidationResult validationResult = validationCache.get(currentLocation.getId());
		
		if (validationResult == null)
		{
			log.debug("No cached validation result for {}, validating now", currentLocation.getDisplayName());
			validateCurrentLocation();
			validationResult = validationCache.get(currentLocation.getId());
			
			if (validationResult == null)
			{
				log.error("Still no validation result after validating, this shouldn't happen");
				return;
			}
		}

		// If validation failed, modify the menu entry to "Walk here"
		if (!validationResult.isValid())
		{
			// Get the current menu entries
			MenuEntry[] menuEntries = client.getMenuEntries();

			// Find the "Walk here" entry (if it exists)
			MenuEntry walkHereEntry = null;
			int walkHereIndex = -1;
			
			for (int i = 0; i < menuEntries.length; i++)
			{
				MenuEntry entry = menuEntries[i];
				if (entry.getType() == MenuAction.WALK)
				{
					walkHereEntry = entry;
					walkHereIndex = i;
					break;
				}
			}

			// If we found a "Walk here" entry and it's not already last, move it to the end
			if (walkHereEntry != null && walkHereIndex < menuEntries.length - 1)
			{
				// Create a new array with "Walk here" at the end (last = default left-click)
				MenuEntry[] reorderedEntries = new MenuEntry[menuEntries.length];
				
				// Copy all other entries, skipping the original "Walk here" position
				int newIndex = 0;
				for (int i = 0; i < menuEntries.length; i++)
				{
					if (i != walkHereIndex)
					{
						reorderedEntries[newIndex++] = menuEntries[i];
					}
				}
				
				// Put "Walk here" at the end (default left-click position)
				reorderedEntries[menuEntries.length - 1] = walkHereEntry;
				
				client.setMenuEntries(reorderedEntries);
			}

			// Only display feedback message once per validation
			if (!Boolean.TRUE.equals(menuModifiedState.get(currentLocation.getId())))
			{
				// Display simple message since we're not tracking individual runes
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
		else
		{
			// Validation passed - reset the menu modified state so if it fails later, message shows again
			menuModifiedState.remove(currentLocation.getId());
		}
	}

	/**
	 * Validate requirements for the current location.
	 * This is called on every menu entry to ensure real-time validation.
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
				"No requirements configured", 
				new HashMap<>()
			);
			validationCache.put(currentLocation.getId(), allowResult);
			return;
		}

		// Check if player has required runes by counting them directly
		log.debug("Checking requirements for Resurrect Greater Ghost spell");
		
		boolean hasRunes = playerStateValidator.hasResurrectGreaterGhostRunes();
		boolean hasBook = playerStateValidator.hasBookOfTheDead();
		boolean hasSpellbook = playerStateValidator.isOnArceuusSpellbook();

		log.debug("Has required runes: {}", hasRunes);
		log.debug("Has Book of the Dead: {}", hasBook);
		log.debug("On Arceuus spellbook: {}", hasSpellbook);

		ValidationResult validationResult;
		if (hasRunes && hasBook && hasSpellbook)
		{
			log.debug("Validation PASSED for {}: Has all requirements for Resurrect Greater Ghost", currentLocation.getDisplayName());
			validationResult = new ValidationResult(
				true,
				java.util.Collections.emptySet(),
				"All requirements met",
				new HashMap<>()
			);
		}
		else
		{
			// Build specific failure message
			StringBuilder failureMessage = new StringBuilder("Missing: ");
			java.util.List<String> missing = new java.util.ArrayList<>();
			
			if (!hasRunes)
			{
				missing.add("required runes");
			}
			if (!hasBook)
			{
				missing.add("Book of the Dead");
			}
			if (!hasSpellbook)
			{
				missing.add("Arceuus spellbook");
			}
			
			failureMessage.append(String.join(", ", missing));
			failureMessage.append(" for Resurrect Greater Ghost");

			log.debug("Validation FAILED for {}: {}", currentLocation.getDisplayName(), failureMessage);
			validationResult = new ValidationResult(
				false,
				java.util.Collections.singleton(failureMessage.toString()),
				failureMessage.toString(),
				new HashMap<>()
			);
		}

		// Cache the result
		validationCache.put(currentLocation.getId(), validationResult);
		log.debug("=== Validation complete, result cached ===");
	}

	/**
	 * Check if validation is required for a specific location based on config.
	 */
	private boolean isValidationRequired(BossLocation location)
	{
		// For now, only Nex has configurable validation
		if ("nex".equals(location.getId()))
		{
			return config.nexRequireSpell();
		}

		// Future locations can be added here
		return false;
	}

	/**
	 * Check if two region arrays are equal.
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
		for (int i = 0; i < regions1.length; i++)
		{
			if (regions1[i] != regions2[i])
			{
				return false;
			}
		}
		return true;
	}

	@Provides
	AccessDeniedConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AccessDeniedConfig.class);
	}
}

