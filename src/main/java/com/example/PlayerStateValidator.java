package com.example;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
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
		// Varbit 4070 tracks the current spellbook
		// 0 = Standard, 1 = Ancient, 2 = Lunar, 3 = Arceuus
		final int SPELLBOOK_VARBIT = 4070;
		final int ARCEUUS_SPELLBOOK = 3;

		int currentSpellbook = client.getVarbitValue(SPELLBOOK_VARBIT);
		log.debug("Current spellbook varbit value: {} (3 = Arceuus)", currentSpellbook);

		return currentSpellbook == ARCEUUS_SPELLBOOK;
	}

	/**
	 * Check if the player has the required runes to cast Resurrect Greater Ghost.
	 * Resurrect Greater Ghost requires: 4 Soul runes, 2 Blood runes, 1 Cosmic rune
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

		// Get total rune counts from all sources
		Map<Integer, Integer> totalRunes = getTotalRuneCounts();

		log.debug("Checking Resurrect Greater Ghost runes:");
		log.debug("  Required: Soul x4, Blood x2, Cosmic x1");
		log.debug("  Available: Soul x{}, Blood x{}, Cosmic x{}", 
			totalRunes.getOrDefault(566, 0),
			totalRunes.getOrDefault(565, 0),
			totalRunes.getOrDefault(564, 0));

		// Check if we have enough of each required rune
		for (Map.Entry<Integer, Integer> entry : requiredRunes.entrySet())
		{
			int runeId = entry.getKey();
			int required = entry.getValue();
			int available = totalRunes.getOrDefault(runeId, 0);

			if (available < required)
			{
				log.debug("  Missing rune {} - need {}, have {}", runeId, required, available);
				return false;
			}
		}

		log.debug("  All runes available!");
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

		for (Item item : inventory.getItems())
		{
			if (item.getId() == BOOK_OF_THE_DEAD_ID)
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
	 */
	private Map<Integer, Integer> getTotalRuneCounts()
	{
		Map<Integer, Integer> runeCounts = new HashMap<>();

		// Count runes in inventory
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory != null)
		{
			for (Item item : inventory.getItems())
			{
				if (item.getId() > 0)
				{
					runeCounts.merge(item.getId(), item.getQuantity(), Integer::sum);
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

			if (runeEnumKey > 0 && amount > 0)
			{
				// Map the enum key to the actual item ID
				int itemId = runepouchEnum.getIntValue(runeEnumKey);
				log.debug("    -> Mapped to ItemID: {}", itemId);
				contents.put(itemId, amount);
			}
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

		log.debug("Checking for rune pouch in inventory:");
		for (Item item : inventory.getItems())
		{
			int itemId = item.getId();
			if (itemId > 0)
			{
				log.debug("  Found item ID: {}", itemId);
			}
			if (itemId == RUNE_POUCH_ID 
				|| itemId == DIVINE_RUNE_POUCH_ID 
				|| itemId == DIVINE_RUNE_POUCH_OLD_ID
				|| itemId == DIVINE_RUNE_POUCH_LOCKED_ID)
			{
				log.debug("  -> Rune pouch found! (ID: {})", itemId);
				return true;
			}
		}

		log.debug("  -> No rune pouch found in inventory");
		return false;
	}
}
