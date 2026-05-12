package com.osrs.accessdenied;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
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
	 * Arceuus spellbook is required to cast Thralls.
	 * 
	 * @return true if the player is on the Arceuus spellbook, false otherwise
	 */
	public boolean isOnArceuusSpellbook()
	{
		// Varbits.SPELLBOOK tracks the current spellbook
		// 0 = Standard, 1 = Ancient, 2 = Lunar, 3 = Arceuus
		final int ARCEUUS_SPELLBOOK = 3;

		int currentSpellbook = getCurrentSpellbook();
		log.debug("Current spellbook varbit value: {} (3 = Arceuus)", currentSpellbook);

		return currentSpellbook == ARCEUUS_SPELLBOOK;
	}

	/**
	 * Check if the player is on the Lunar spellbook.
	 * Lunar spellbook is required to cast Humidify and Vengeance.
	 *
	 * @return true if the player is on the Lunar spellbook, false otherwise
	 */
	public boolean isOnLunarSpellbook()
	{
		// Varbits.SPELLBOOK tracks the current spellbook
		// 0 = Standard, 1 = Ancient, 2 = Lunar, 3 = Arceuus
		final int LUNAR_SPELLBOOK = 2;

		int currentSpellbook = getCurrentSpellbook();
		log.debug("Current spellbook varbit value: {} (2 = Lunar)", currentSpellbook);

		return currentSpellbook == LUNAR_SPELLBOOK;
	}

	/**
	 * Check if the player is on the Ancient spellbook.
	 * Ancient spellbook is required to cast Ice Barrage and Blood Barrage.
	 *
	 * @return true if the player is on the Ancient spellbook, false otherwise
	 */
	public boolean isOnAncientSpellbook()
	{
		// Varbits.SPELLBOOK tracks the current spellbook
		// 0 = Standard, 1 = Ancient, 2 = Lunar, 3 = Arceuus
		final int ANCIENT_SPELLBOOK = 1;

		int currentSpellbook = getCurrentSpellbook();
		log.debug("Current spellbook varbit value: {} (1 = Ancient)", currentSpellbook);

		return currentSpellbook == ANCIENT_SPELLBOOK;
	}

	/**
	 * Check if the player has the required runes to cast Thralls.
	 * Thralls requires: 10 Fire runes, 2 Blood runes, 1 Cosmic rune
	 * Aether runes count as both Soul and Cosmic runes.
	 * 
	 * @return true if the player has sufficient runes, false otherwise
	 */
	public boolean hasResurrectGreaterGhostRunes()
	{
		// Required runes for Thralls
		Map<Integer, Integer> requiredRunes = new HashMap<>();
		requiredRunes.put(554, 10); // Fire rune
		requiredRunes.put(565, 5);  // Blood rune
		requiredRunes.put(564, 1);  // Cosmic rune

		log.debug("Checking Thralls runes:");
		log.debug("  Required: Fire x10, Blood x5, Cosmic x1");

		return hasRequiredRunesWithSubstitution(requiredRunes, "Thralls");
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

		return hasRequiredRunesWithSubstitution(requiredRunes, "Death Charge");
	}

	/**
	 * Check if the player has the required runes to cast Humidify.
	 * Humidify requires: 1 Astral rune, 1 Fire rune, 1 Water rune.
	 * Lunar spellbook only; no Aether substitutions apply.
	 *
	 * @return true if the player has sufficient runes, false otherwise
	 */
	public boolean hasHumidifyRunes()
	{
		Map<Integer, Integer> requiredRunes = new HashMap<>();
		requiredRunes.put(9075, 1); // Astral rune
		requiredRunes.put(554, 1);  // Fire rune
		requiredRunes.put(555, 1);  // Water rune

		log.debug("Checking Humidify runes:");
		log.debug("  Required: Astral x1, Fire x1, Water x1");

		return hasRequiredRunesWithSubstitution(requiredRunes, "Humidify");
	}

	/**
	 * Check if the player has the required runes to cast Vengeance.
	 * Vengeance requires: 10 Earth runes, 4 Astral runes, 2 Death runes.
	 * Lunar spellbook only; no Aether substitutions apply.
	 *
	 * @return true if the player has sufficient runes, false otherwise
	 */
	public boolean hasVengeanceRunes()
	{
		Map<Integer, Integer> requiredRunes = new HashMap<>();
		requiredRunes.put(557, 10);  // Earth rune
		requiredRunes.put(9075, 4);  // Astral rune
		requiredRunes.put(560, 2);   // Death rune

		log.debug("Checking Vengeance runes:");
		log.debug("  Required: Earth x10, Astral x4, Death x2");

		return hasRequiredRunesWithSubstitution(requiredRunes, "Vengeance");
	}

	/**
	 * Check if the player has the required runes to cast Ice Barrage.
	 * Ice Barrage requires: 6 Water runes, 2 Death runes, 4 Blood runes
	 * Note: Ice Barrage does not use Soul or Cosmic runes, so Aether runes don't help.
	 * Special case: Kodai wand provides infinite water runes.
	 * @return true if the player has sufficient runes, false otherwise
	 */
	public boolean hasIceBarrageRunes()
	{
		// Required runes for Ice Barrage
		Map<Integer, Integer> requiredRunes = new HashMap<>();
		requiredRunes.put(555, 6);  // Water rune
		requiredRunes.put(560, 2);  // Death rune
		requiredRunes.put(565, 4);  // Blood rune

		log.debug("Checking Ice Barrage runes:");
		log.debug("  Required: Water x6, Death x2, Blood x4");

		// Check if player has Kodai wand (provides infinite water runes)
		if (hasKodaiWand())
		{
			log.debug("  Kodai wand found - water runes not required");
			// Remove water rune requirement
			requiredRunes.remove(555);
		}

		return hasRequiredRunesWithSubstitution(requiredRunes, "Ice Barrage");
	}

	/**
	 * Check if the player has the required runes to cast Blood Barrage.
	 * Blood Barrage requires: 4 Blood runes, 1 Soul rune, 4 Death rune
	 * Aether runes can substitute for Soul runes.
	 * 
	 * @return true if the player has sufficient runes, false otherwise
	 */
	public boolean hasBloodBarrageRunes()
	{
		// Required runes for Blood Barrage
		Map<Integer, Integer> requiredRunes = new HashMap<>();
		requiredRunes.put(565, 4);  // Blood rune
		requiredRunes.put(566, 1);  // Soul rune
		requiredRunes.put(560, 4);  // Death rune

		log.debug("Checking Blood Barrage runes:");
		log.debug("  Required: Blood x4, Soul x1, Death x4");

		return hasRequiredRunesWithSubstitution(requiredRunes, "Blood Barrage");
	}

	/**
	 * Check if the player has a Kodai wand equipped or in their inventory.
	 * Kodai wand provides infinite water runes for spells.
	 * Item ID: 21006 = Kodai wand
	 * @return true if the player has a Kodai wand, false otherwise
	 */
	private boolean hasKodaiWand()
	{
		final int KODAI_WAND_ID = 21006;
		final int EQUIPMENT_CONTAINER_ID = 94;

		// Check inventory
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory != null)
		{
			for (Item item : inventory.getItems())
			{
				if (item != null && item.getId() == KODAI_WAND_ID)
				{
					log.debug("Kodai wand found in inventory");
					return true;
				}
			}
		}

		// Check equipment (container ID 94)
		ItemContainer equipment = client.getItemContainer(EQUIPMENT_CONTAINER_ID);
		if (equipment != null)
		{
			for (Item item : equipment.getItems())
			{
				if (item != null && item.getId() == KODAI_WAND_ID)
				{
					log.debug("Kodai wand found in equipment");
					return true;
				}
			}
		}

		log.debug("Kodai wand NOT found");
		return false;
	}

	public boolean hasChugJug()
	{
		return hasItemInInventory(ItemID.MM_PREPOT_DEVICE);
	}

	public boolean hasSaturatedHeart()
	{
		return hasItemInInventory(ItemID.SATURATED_HEART);
	}

	private boolean hasItemInInventory(int itemId)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return false;
		}
		for (Item item : inventory.getItems())
		{
			if (item != null && item.getId() == itemId)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if the player has the required runes, accounting for combination runes.
	 * Substitution is driven by {@link CombinationRune}: each rune type is checked
	 * independently by summing the player's standard runes with any combination runes
	 * that cover that type. This matches OSRS behaviour, where the game validates each
	 * rune type independently — a single Steam rune counts toward both the Fire check
	 * and the Water check without being "spent" between the two.
	 *
	 * @param requiredRunes Map of rune ID to required quantity
	 * @param spellName Name of the spell for logging purposes
	 * @return true if the player has sufficient runes, false otherwise
	 */
	private boolean hasRequiredRunesWithSubstitution(Map<Integer, Integer> requiredRunes, String spellName)
	{
		Map<Integer, Integer> totalRunes = getTotalRuneCounts();

		for (Map.Entry<Integer, Integer> entry : requiredRunes.entrySet())
		{
			int runeId = entry.getKey();
			int required = entry.getValue();
			int available = totalRunes.getOrDefault(runeId, 0);

			for (CombinationRune combinationRune : CombinationRune.getSubstitutesForRune(runeId))
			{
				available += totalRunes.getOrDefault(combinationRune.getItemId(), 0);
			}

			log.debug("  Rune {} - need {}, have {}", runeId, required, available);

			if (available < required)
			{
				log.debug("  Missing rune {} - need {}, have {}", runeId, required, available);
				return false;
			}
		}

		log.debug("  All runes available for {}!", spellName);
		return true;
	}

	/**
	 * Check if the player has a Book of the Dead in their inventory or equipped.
	 * Book of the Dead is required to cast Thralls.
	 *
	 * @return true if the player has a Book of the Dead, false otherwise
	 */
	public boolean hasBookOfTheDead()
	{
		final int BOOK_OF_THE_DEAD_ID = 25818;
		final int EQUIPMENT_CONTAINER_ID = 94;

		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory != null)
		{
			for (Item item : inventory.getItems())
			{
				if (item != null && item.getId() == BOOK_OF_THE_DEAD_ID)
				{
					log.debug("Book of the Dead found in inventory");
					return true;
				}
			}
		}

		ItemContainer equipment = client.getItemContainer(EQUIPMENT_CONTAINER_ID);
		if (equipment != null)
		{
			for (Item item : equipment.getItems())
			{
				if (item != null && item.getId() == BOOK_OF_THE_DEAD_ID)
				{
					log.debug("Book of the Dead found in equipment");
					return true;
				}
			}
		}

		log.debug("Book of the Dead NOT found");
		return false;
	}

	/**
	 * Get total rune counts from inventory, equipment, and rune pouch.
	 * This method is called multiple times during validation, so results should be cached
	 * at the validation level to avoid redundant inventory scans.
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
			for (Item item : inventory.getItems())
			{
				if (item != null && item.getId() > 0)
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
	 * IMPORTANT: Only returns contents if the rune pouch is actually in the player's inventory.
	 * This prevents reading stale varbit data when the pouch has been deposited.
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

		for (Item item : inventory.getItems())
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

	private int getCurrentSpellbook()
	{
		return client.getVarbitValue(VarbitID.SPELLBOOK);
	}
}
