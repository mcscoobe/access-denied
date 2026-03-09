package com.osrs.accessdenied;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Varbits;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class responsible for validating player state against area requirements.
 * Checks actual rune counts from inventory, equipment, and rune pouch.
 */
@Slf4j
@Singleton
public class PlayerStateValidator
{
	private final Client client;

	@Inject
	public PlayerStateValidator(Client client)
	{
		this.client = client;
	}

	/**
	 * Check if the player is on the Arceuus spellbook.
	 * Arceuus spellbook is required to cast Resurrect Greater Ghost.
	 * 
	 * @return true if the player is on the Arceuus spellbook, false otherwise
	 */
	public boolean isOnArceuusSpellbook()
	{
		// Varbits.SPELLBOOK tracks the current spellbook
		// 0 = Standard, 1 = Ancient, 2 = Lunar, 3 = Arceuus
		final int ARCEUUS_SPELLBOOK = 3;

		int currentSpellbook = client.getVarbitValue(Varbits.SPELLBOOK);
		log.debug("Current spellbook varbit value: {} (3 = Arceuus)", currentSpellbook);

		return currentSpellbook == ARCEUUS_SPELLBOOK;
	}

	/**
	 * Check if the player has the required runes to cast Resurrect Greater Ghost.
	 * Resurrect Greater Ghost requires: 4 Soul runes, 2 Blood runes, 1 Cosmic rune
	 * Aether runes count as both Soul and Cosmic runes.
	 * 
	 * @return true if the player has sufficient runes, false otherwise
	 */
	public boolean hasResurrectGreaterGhostRunes()
	{
		// Required runes for Resurrect Greater Ghost
		Map<Integer, Integer> requiredRunes = new HashMap<>();
		requiredRunes.put(566, 4);  // Soul rune
		requiredRunes.put(565, 2);  // Blood rune
		requiredRunes.put(564, 1);  // Cosmic rune

		log.debug("Checking Resurrect Greater Ghost runes:");
		log.debug("  Required: Soul x4, Blood x2, Cosmic x1");

		return hasRequiredRunesWithAether(requiredRunes, "Resurrect Greater Ghost");
	}

	/**
	 * Check if the player has the required runes to cast Death Charge.
	 * Death Charge requires: 1 Death rune, 1 Blood rune, 1 Soul rune
	 * Aether runes count as both Soul and Cosmic runes.
	 * 
	 * @return true if the player has sufficient runes, false otherwise
	 */
	public boolean hasDeathChargeRunes()
	{
		// Required runes for Death Charge
		Map<Integer, Integer> requiredRunes = new HashMap<>();
		requiredRunes.put(560, 1);  // Death rune
		requiredRunes.put(565, 1);  // Blood rune
		requiredRunes.put(566, 1);  // Soul rune

		log.debug("Checking Death Charge runes:");
		log.debug("  Required: Death x1, Blood x1, Soul x1");

		return hasRequiredRunesWithAether(requiredRunes, "Death Charge");
	}

	/**
	 * Check if the player has the required runes, accounting for Aether runes.
	 * Aether runes (ID 30843) count as both Soul runes (566) and Cosmic runes (564).
	 * 
	 * Algorithm:
	 * 1. Check each required rune type
	 * 2. If Soul or Cosmic runes are short, try to make up the difference with Aether runes
	 * 3. Track how many Aether runes have been "spent" on substitutions
	 * 4. Fail if any requirement can't be met even with Aether substitution
	 * 
	 * Example: Need 4 Soul + 1 Cosmic, have 2 Soul + 3 Aether
	 * - Soul: need 4, have 2, short by 2 → use 2 Aether (1 Aether remaining)
	 * - Cosmic: need 1, have 0, short by 1 → use 1 Aether (0 Aether remaining)
	 * - Result: PASS (all requirements met with Aether substitution)
	 * 
	 * @param requiredRunes Map of rune ID to required quantity
	 * @param spellName Name of the spell for logging purposes
	 * @return true if the player has sufficient runes, false otherwise
	 */
	private boolean hasRequiredRunesWithAether(Map<Integer, Integer> requiredRunes, String spellName)
	{
		final int SOUL_RUNE_ID = 566;
		final int COSMIC_RUNE_ID = 564;
		final int AETHER_RUNE_ID = 30843;

		// Get total rune counts from all sources
		Map<Integer, Integer> totalRunes = getTotalRuneCounts();

		// Get aether rune count
		int aetherCount = totalRunes.getOrDefault(AETHER_RUNE_ID, 0);

		// Track how many aether runes we've allocated
		int aetherUsed = 0;

		// Check each required rune
		for (Map.Entry<Integer, Integer> entry : requiredRunes.entrySet())
		{
			int runeId = entry.getKey();
			int required = entry.getValue();
			int available = totalRunes.getOrDefault(runeId, 0);

			// For Soul and Cosmic runes, aether runes can substitute
			if ((runeId == SOUL_RUNE_ID || runeId == COSMIC_RUNE_ID) && available < required)
			{
				// Calculate how many aether runes we can still use
				int aetherAvailable = aetherCount - aetherUsed;
				int shortage = required - available;
				int aetherToUse = Math.min(shortage, aetherAvailable);
				available += aetherToUse;
				aetherUsed += aetherToUse;
			}

			log.debug("  Rune {} - need {}, have {} (aether used so far: {})", 
				runeId, required, available, aetherUsed);

			if (available < required)
			{
				log.debug("  Missing rune {} - need {}, have {}", runeId, required, available);
				return false;
			}
		}

		log.debug("  All runes available for {}! (used {} aether runes)", spellName, aetherUsed);
		return true;
	}

	/**
	 * Check if the player has a Book of the Dead in their inventory.
	 * Book of the Dead is required to cast Resurrect Greater Ghost.
	 * 
	 * @return true if the player has a Book of the Dead, false otherwise
	 */
	public boolean hasBookOfTheDead()
	{
		final int BOOK_OF_THE_DEAD_ID = 25818;

		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			log.debug("Checking Book of the Dead: Inventory is null");
			return false;
		}

		Item[] items = inventory.getItems();
		if (items == null)
		{
			log.debug("Checking Book of the Dead: Items array is null");
			return false;
		}

		for (Item item : items)
		{
			if (item != null && item.getId() == BOOK_OF_THE_DEAD_ID)
			{
				log.debug("Book of the Dead found in inventory");
				return true;
			}
		}

		log.debug("Book of the Dead NOT found in inventory");
		return false;
	}

	/**
	 * Get total rune counts from inventory, equipment, and rune pouch.
	 * This method is called multiple times during validation, so results should be cached
	 * at the validation level to avoid redundant inventory scans.
	 * 
	 * Sources checked:
	 * 1. Inventory - loose runes and items
	 * 2. Rune pouch - stored runes (if pouch is in inventory)
	 * 
	 * @return Map of rune ID to total quantity across all sources
	 */
	private Map<Integer, Integer> getTotalRuneCounts()
	{
		Map<Integer, Integer> runeCounts = new HashMap<>();

		// Count runes in inventory
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory != null)
		{
			Item[] items = inventory.getItems();
			if (items != null)
			{
				for (Item item : items)
				{
					if (item != null && item.getId() > 0)
					{
						runeCounts.merge(item.getId(), item.getQuantity(), Integer::sum);
					}
				}
			}
		}

		// Count runes in rune pouch
		Map<Integer, Integer> runePouchRunes = getRunePouchContents();
		for (Map.Entry<Integer, Integer> entry : runePouchRunes.entrySet())
		{
			runeCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
		}

		return runeCounts;
	}

	/**
	 * Get rune pouch contents from varbits.
	 * The rune pouch uses varbits to store contents (not varps).
	 * The varbit values are enum keys that need to be mapped to actual item IDs.
	 * 
	 * IMPORTANT: Only returns contents if the rune pouch is actually in the player's inventory.
	 * This prevents reading stale varbit data when the pouch has been deposited.
	 * 
	 * Rune Pouch Storage:
	 * - Up to 4 different rune types can be stored
	 * - Each slot has a TYPE varbit (enum key) and QUANTITY varbit
	 * - TYPE varbit of 0 means the slot is empty
	 * - The enum maps varbit values to actual rune item IDs
	 * 
	 * @return Map of rune ID to quantity in the rune pouch
	 */
	private Map<Integer, Integer> getRunePouchContents()
	{
		Map<Integer, Integer> contents = new HashMap<>();

		// First, check if the player actually has a rune pouch in their inventory
		if (!hasRunePouchInInventory())
		{
			log.debug("Rune pouch not found in inventory, ignoring varbits");
			return contents;
		}

		// Get the rune pouch enum to map varbit values to item IDs
		EnumComposition runepouchEnum = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		if (runepouchEnum == null)
		{
			log.debug("Rune pouch enum not available");
			return contents;
		}

		// Rune pouch uses varbits to store contents
		// Use VarbitID constants from RuneLite API
		int[] runeVarbits = {
			VarbitID.RUNE_POUCH_TYPE_1,
			VarbitID.RUNE_POUCH_TYPE_2,
			VarbitID.RUNE_POUCH_TYPE_3,
			VarbitID.RUNE_POUCH_TYPE_4
		};
		int[] amountVarbits = {
			VarbitID.RUNE_POUCH_QUANTITY_1,
			VarbitID.RUNE_POUCH_QUANTITY_2,
			VarbitID.RUNE_POUCH_QUANTITY_3,
			VarbitID.RUNE_POUCH_QUANTITY_4
		};

		log.debug("Reading rune pouch contents from varbits:");
		for (int i = 0; i < 4; i++)
		{
			int runeEnumKey = client.getVarbitValue(runeVarbits[i]);
			int amount = client.getVarbitValue(amountVarbits[i]);

			log.debug("  Slot {} - Varbit value (enum key): {}, Amount: {}", i + 1, runeEnumKey, amount);

			if (runeEnumKey <= 0 || amount <= 0)
			{
				continue;
			}

			// Map the enum key to the actual item ID
			int itemId = runepouchEnum.getIntValue(runeEnumKey);
			log.debug("    -> Mapped to ItemID: {}", itemId);
			contents.put(itemId, amount);
		}

		log.debug("Rune pouch total: {} different rune types", contents.size());
		
		return contents;
	}

	/**
	 * Check if the player has a rune pouch in their inventory.
	 * Rune pouch item IDs:
	 * - 12791: Regular rune pouch
	 * - 27281: Divine rune pouch
	 * - 27086: Divine rune pouch (old ID, may be deprecated)
	 * - 27509: Divine rune pouch (locked)
	 * 
	 * This check is critical because rune pouch varbits persist even after
	 * the pouch is deposited, so we must verify the pouch is actually present.
	 * 
	 * @return true if any rune pouch variant is in inventory, false otherwise
	 */
	private boolean hasRunePouchInInventory()
	{
		final int RUNE_POUCH_ID = 12791;
		final int DIVINE_RUNE_POUCH_ID = 27281;
		final int DIVINE_RUNE_POUCH_OLD_ID = 27086;
		final int DIVINE_RUNE_POUCH_LOCKED_ID = 27509;

		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			log.debug("Inventory container is null");
			return false;
		}

		Item[] items = inventory.getItems();
		if (items == null)
		{
			log.debug("Inventory items array is null");
			return false;
		}

		for (Item item : items)
		{
			if (item == null)
			{
				continue;
			}
			
			int itemId = item.getId();
			if (itemId == RUNE_POUCH_ID 
				|| itemId == DIVINE_RUNE_POUCH_ID 
				|| itemId == DIVINE_RUNE_POUCH_OLD_ID
				|| itemId == DIVINE_RUNE_POUCH_LOCKED_ID)
			{
				log.debug("Rune pouch found in inventory (ID: {})", itemId);
				return true;
			}
		}

		log.debug("No rune pouch found in inventory");
		return false;
	}
}
