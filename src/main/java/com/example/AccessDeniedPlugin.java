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
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
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

		// Check for inventory changes (runes can be in inventory or rune pouch)
		int containerId = event.getContainerId();
		if (containerId == InventoryID.INV)
		{
			log.debug("Inventory changed while in {} region, revalidating", 
				currentLocation.getDisplayName());
			validateCurrentLocation();
		}
	}

	/**
	 * Listen for varbit changes to detect when rune pouch contents are updated.
	 * This is the most reliable way to detect rune pouch changes.
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

		// Only revalidate if the changed varbit is related to rune pouch
		int varbitId = event.getVarbitId();
		if (!isRunePouchVarbit(varbitId))
		{
			return;
		}

		log.debug("Rune pouch varbit {} changed while in {} region, revalidating", 
			varbitId, currentLocation.getDisplayName());
		validateCurrentLocation();
	}

	/**
	 * Check if a varbit ID is related to rune pouch contents.
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
			|| varbitId == VarbitID.RUNE_POUCH_QUANTITY_4;
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

		log.debug("Menu entry detected - Location: {}, ObjectID: {}, Option: {}, Target: {}", 
			currentLocation.getDisplayName(), objectId, event.getOption(), event.getTarget());

		// Get cached validation result for this location
		ValidationResult validationResult = validationCache.get(currentLocation.getId());
		
		if (validationResult == null)
		{
			log.warn("No cached validation result for {}, this shouldn't happen", currentLocation.getDisplayName());
			return;
		}

		log.debug("Validation result - Valid: {}, Message: {}", 
			validationResult.isValid(), validationResult.getFeedbackMessage());

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
				log.debug("Reordered menu entries - 'Walk here' is now the default option (last in array)");
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
		log.debug("Checking rune counts for Resurrect Greater Ghost spell");
		
		boolean hasRunes = playerStateValidator.hasResurrectGreaterGhostRunes();
		boolean hasBook = playerStateValidator.hasBookOfTheDead();

		log.debug("Has required runes: {}", hasRunes);
		log.debug("Has Book of the Dead: {}", hasBook);

		ValidationResult validationResult;
		if (hasRunes && hasBook)
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
			String failureMessage;
			if (!hasRunes && !hasBook)
			{
				failureMessage = "Missing required runes and Book of the Dead for Resurrect Greater Ghost";
			}
			else if (!hasRunes)
			{
				failureMessage = "Missing required runes for Resurrect Greater Ghost";
			}
			else
			{
				failureMessage = "Missing Book of the Dead for Resurrect Greater Ghost";
			}

			log.debug("Validation FAILED for {}: {}", currentLocation.getDisplayName(), failureMessage);
			validationResult = new ValidationResult(
				false,
				java.util.Collections.singleton(failureMessage),
				failureMessage,
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

