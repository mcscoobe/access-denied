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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Service class responsible for validating player state against area requirements.
 * Checks actual rune counts from inventory, equipment, and rune pouch.
 */
@Slf4j
@Singleton
public class PlayerStateValidator
{
	/**
	 * Items that provide an unlimited supply of one or more standard runes.
	 * Maps item ID to the rune ID(s) the item covers. Only the charged tome
	 * variants are listed; uncharged tomes have distinct item IDs and provide nothing.
	 */
	private static final Map<Integer, int[]> INFINITE_RUNE_SOURCES = buildInfiniteRuneSources();

	/**
	 * Regular, divine (current and old), and locked divine rune pouches.
	 */
	private static final Set<Integer> RUNE_POUCH_IDS = Set.of(12791, 27281, 27086, 27509);

	private final Client client;

	@Inject
	public PlayerStateValidator(Client client)
	{
		this.client = client;
	}

	private static Map<Integer, int[]> buildInfiniteRuneSources()
	{
		final int[] air = {ItemID.AIRRUNE};
		final int[] water = {ItemID.WATERRUNE};
		final int[] earth = {ItemID.EARTHRUNE};
		final int[] fire = {ItemID.FIRERUNE};
		final int[] lava = {ItemID.FIRERUNE, ItemID.EARTHRUNE};
		final int[] mud = {ItemID.WATERRUNE, ItemID.EARTHRUNE};
		final int[] steam = {ItemID.WATERRUNE, ItemID.FIRERUNE};
		final int[] smoke = {ItemID.AIRRUNE, ItemID.FIRERUNE};
		final int[] mist = {ItemID.AIRRUNE, ItemID.WATERRUNE};
		final int[] dust = {ItemID.AIRRUNE, ItemID.EARTHRUNE};

		Map<Integer, int[]> sources = new HashMap<>();
		sources.put(ItemID.KODAI_WAND, water);
		sources.put(ItemID.TOME_OF_FIRE, fire);
		sources.put(ItemID.TOME_OF_WATER, water);

		sources.put(ItemID.STAFF_OF_AIR, air);
		sources.put(ItemID.AIR_BATTLESTAFF, air);
		sources.put(ItemID.MYSTIC_AIR_STAFF, air);
		sources.put(ItemID.STAFF_OF_WATER, water);
		sources.put(ItemID.WATER_BATTLESTAFF, water);
		sources.put(ItemID.MYSTIC_WATER_STAFF, water);
		sources.put(ItemID.STAFF_OF_EARTH, earth);
		sources.put(ItemID.EARTH_BATTLESTAFF, earth);
		sources.put(ItemID.MYSTIC_EARTH_STAFF, earth);
		sources.put(ItemID.STAFF_OF_FIRE, fire);
		sources.put(ItemID.FIRE_BATTLESTAFF, fire);
		sources.put(ItemID.MYSTIC_FIRE_STAFF, fire);
		sources.put(ItemID.LAVA_BATTLESTAFF, lava);
		sources.put(ItemID.MYSTIC_LAVA_STAFF, lava);
		sources.put(ItemID.MUD_BATTLESTAFF, mud);
		sources.put(ItemID.MYSTIC_MUD_STAFF, mud);
		sources.put(ItemID.STEAM_BATTLESTAFF, steam);
		sources.put(ItemID.MYSTIC_STEAM_BATTLESTAFF, steam);
		sources.put(ItemID.SMOKE_BATTLESTAFF, smoke);
		sources.put(ItemID.MYSTIC_SMOKE_BATTLESTAFF, smoke);
		sources.put(ItemID.MIST_BATTLESTAFF, mist);
		sources.put(ItemID.MYSTIC_MIST_BATTLESTAFF, mist);
		sources.put(ItemID.DUST_BATTLESTAFF, dust);
		sources.put(ItemID.MYSTIC_DUST_BATTLESTAFF, dust);
		return sources;
	}

	/**
	 * Spellbook varbit values: 0 = Standard, 1 = Ancient, 2 = Lunar, 3 = Arceuus.
	 */
	private static final int ANCIENT_SPELLBOOK = 1;
	private static final int LUNAR_SPELLBOOK = 2;
	private static final int ARCEUUS_SPELLBOOK = 3;

	/**
	 * Arceuus is required to cast Thralls and Death Charge.
	 */
	public boolean isOnArceuusSpellbook()
	{
		return getCurrentSpellbook() == ARCEUUS_SPELLBOOK;
	}

	/**
	 * Lunar is required to cast Humidify and Vengeance.
	 */
	public boolean isOnLunarSpellbook()
	{
		return getCurrentSpellbook() == LUNAR_SPELLBOOK;
	}

	/**
	 * Ancient is required to cast Ice Barrage and Blood Barrage.
	 */
	public boolean isOnAncientSpellbook()
	{
		return getCurrentSpellbook() == ANCIENT_SPELLBOOK;
	}

	/**
	 * Thralls, on Arceuus. Aether runes substitute for the Cosmic rune.
	 */
	public boolean hasResurrectGreaterGhostRunes()
	{
		return hasRequiredRunes(Map.of(ItemID.FIRERUNE, 10, ItemID.BLOODRUNE, 5, ItemID.COSMICRUNE, 1));
	}

	/**
	 * Death Charge, on Arceuus. Aether runes substitute for the Soul rune.
	 */
	public boolean hasDeathChargeRunes()
	{
		return hasRequiredRunes(Map.of(ItemID.DEATHRUNE, 1, ItemID.BLOODRUNE, 1, ItemID.SOULRUNE, 1));
	}

	/**
	 * Humidify, on Lunar.
	 */
	public boolean hasHumidifyRunes()
	{
		return hasRequiredRunes(Map.of(ItemID.ASTRALRUNE, 1, ItemID.FIRERUNE, 1, ItemID.WATERRUNE, 1));
	}

	/**
	 * Vengeance, on Lunar.
	 */
	public boolean hasVengeanceRunes()
	{
		return hasRequiredRunes(Map.of(ItemID.EARTHRUNE, 10, ItemID.ASTRALRUNE, 4, ItemID.DEATHRUNE, 2));
	}

	/**
	 * Ice Barrage, on Ancient. Uses no Soul or Cosmic runes, so Aether runes don't help.
	 */
	public boolean hasIceBarrageRunes()
	{
		return hasRequiredRunes(Map.of(ItemID.WATERRUNE, 6, ItemID.DEATHRUNE, 2, ItemID.BLOODRUNE, 4));
	}

	/**
	 * Blood Barrage, on Ancient. Aether runes substitute for the Soul rune.
	 */
	public boolean hasBloodBarrageRunes()
	{
		return hasRequiredRunes(Map.of(ItemID.BLOODRUNE, 4, ItemID.SOULRUNE, 1, ItemID.DEATHRUNE, 4));
	}

	/**
	 * Scan inventory and worn equipment for items that grant unlimited runes
	 * (Kodai wand, charged tomes, elemental and combination staves).
	 * This is a capability check matching the previous Kodai behaviour: carrying the
	 * item is enough, it does not need to be wielded. As a result it can over-suppress
	 * when a spell needs two elements covered only by two staves that cannot be wielded
	 * at once (e.g. Humidify with separate fire and water staves); this only affects
	 * trivial single-rune requirements and is accepted.
	 *
	 * @return set of standard rune IDs the player has an unlimited supply of
	 */
	private Set<Integer> getInfiniteRuneSources()
	{
		Set<Integer> covered = new HashSet<>();

		anyCarriedOrWornItemMatches(item ->
		{
			int[] runes = INFINITE_RUNE_SOURCES.get(item.getId());
			if (runes != null)
			{
				for (int runeId : runes)
				{
					covered.add(runeId);
				}
			}
			return false;
		});

		return covered;
	}

	/**
	 * Scans the player's inventory then worn equipment, stopping as soon as {@code matcher}
	 * matches an item. Skips either container if it isn't present (e.g. not yet loaded).
	 *
	 * @return true if some item matched, false if none did
	 */
	private boolean anyCarriedOrWornItemMatches(Predicate<Item> matcher)
	{
		int[] containerIds = {InventoryID.INV, InventoryID.WORN};

		for (int containerId : containerIds)
		{
			ItemContainer container = client.getItemContainer(containerId);
			if (container == null)
			{
				continue;
			}

			for (Item item : container.getItems())
			{
				if (item != null && matcher.test(item))
				{
					return true;
				}
			}
		}

		return false;
	}

	public boolean hasChugJug()
	{
		return anyInventoryItemMatches(item -> item.getId() == ItemID.MM_PREPOT_DEVICE);
	}

	public boolean hasSaturatedHeart()
	{
		return anyInventoryItemMatches(item -> item.getId() == ItemID.SATURATED_HEART);
	}

	/**
	 * As {@link #anyCarriedOrWornItemMatches}, but the inventory only — worn equipment
	 * doesn't count for items that are banned rather than required.
	 */
	private boolean anyInventoryItemMatches(Predicate<Item> matcher)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return false;
		}

		for (Item item : inventory.getItems())
		{
			if (item != null && matcher.test(item))
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
	 * Runes covered by an infinite rune source (staff, charged tome, Kodai wand) are
	 * treated as satisfied regardless of quantity.
	 *
	 * @param requiredRunes Map of rune ID to required quantity
	 * @return true if the player has sufficient runes, false otherwise
	 */
	private boolean hasRequiredRunes(Map<Integer, Integer> requiredRunes)
	{
		Map<Integer, Integer> totalRunes = getTotalRuneCounts();
		Set<Integer> infiniteRunes = getInfiniteRuneSources();

		for (Map.Entry<Integer, Integer> entry : requiredRunes.entrySet())
		{
			int runeId = entry.getKey();

			if (infiniteRunes.contains(runeId))
			{
				continue;
			}

			int available = totalRunes.getOrDefault(runeId, 0);

			for (CombinationRune combinationRune : CombinationRune.getSubstitutesForRune(runeId))
			{
				available += totalRunes.getOrDefault(combinationRune.getItemId(), 0);
			}

			if (available < entry.getValue())
			{
				log.debug("Missing rune {} - need {}, have {}", runeId, entry.getValue(), available);
				return false;
			}
		}

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
		return anyCarriedOrWornItemMatches(item -> item.getId() == ItemID.BOOK_OF_THE_DEAD);
	}

	/**
	 * @return Map of rune ID to total quantity across the inventory and the rune pouch
	 */
	private Map<Integer, Integer> getTotalRuneCounts()
	{
		Map<Integer, Integer> runeCounts = new HashMap<>();

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

		Map<Integer, Integer> runePouchRunes = getRunePouchContents();
		for (Map.Entry<Integer, Integer> entry : runePouchRunes.entrySet())
		{
			runeCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
		}

		return runeCounts;
	}

	/**
	 * Reads the rune pouch's four (type varbit, quantity varbit) slot pairs, mapping each
	 * type key through the RUNEPOUCH_RUNE enum to an item ID. A type key of 0 is an empty
	 * slot. The varbits persist after the pouch is deposited, so the pouch must be
	 * confirmed present first or stale contents would be counted.
	 *
	 * @return Map of rune ID to quantity in the rune pouch
	 */
	private Map<Integer, Integer> getRunePouchContents()
	{
		Map<Integer, Integer> contents = new HashMap<>();

		if (!hasRunePouchInInventory())
		{
			return contents;
		}

		EnumComposition runepouchEnum = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		if (runepouchEnum == null)
		{
			log.debug("Rune pouch enum not available");
			return contents;
		}

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

		for (int i = 0; i < runeVarbits.length; i++)
		{
			int runeEnumKey = client.getVarbitValue(runeVarbits[i]);
			int amount = client.getVarbitValue(amountVarbits[i]);

			if (runeEnumKey <= 0 || amount <= 0)
			{
				continue;
			}

			contents.put(runepouchEnum.getIntValue(runeEnumKey), amount);
		}

		return contents;
	}

	private boolean hasRunePouchInInventory()
	{
		return anyInventoryItemMatches(item -> RUNE_POUCH_IDS.contains(item.getId()));
	}

	private int getCurrentSpellbook()
	{
		return client.getVarbitValue(VarbitID.SPELLBOOK);
	}
}
