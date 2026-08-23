package com.osrs.accessdenied;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for PlayerStateValidator using jqwik.
 * Tests rune counting logic with random inputs to verify edge cases and invariants.
 */
class PlayerStateValidatorPropertyTest
{

	// Rune IDs
	private static final int FIRE_RUNE_ID = 554;
	private static final int WATER_RUNE_ID = 555;
	private static final int EARTH_RUNE_ID = 557;
	private static final int DEATH_RUNE_ID = 560;
	private static final int COSMIC_RUNE_ID = 564;
	private static final int BLOOD_RUNE_ID = 565;
	private static final int SOUL_RUNE_ID = 566;
	private static final int ASTRAL_RUNE_ID = 9075;
	private static final int AETHER_RUNE_ID = 30843;
	private static final int BOOK_OF_THE_DEAD_ID = 25818;
	private static final int RUNE_POUCH_ID = 12791;

	/**
	 * Helper to create a validator with mocked client and inventory.
	 */
	private PlayerStateValidator createValidator(Item[] inventoryItems)
	{
		Client client = mock(Client.class);
		ItemContainer inventory = mock(ItemContainer.class);
		EnumComposition runepouchEnum = mock(EnumComposition.class);

		when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
		when(client.getEnum(EnumID.RUNEPOUCH_RUNE)).thenReturn(runepouchEnum);
		when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(3); // Arceuus spellbook
		when(inventory.getItems()).thenReturn(inventoryItems);

		// Setup empty rune pouch by default
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_TYPE_1)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_TYPE_2)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_TYPE_3)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_TYPE_4)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_QUANTITY_1)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_QUANTITY_2)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_QUANTITY_3)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_QUANTITY_4)).thenReturn(0);

		return new PlayerStateValidator(client);
	}

	/**
	 * Helper to create a validator with null inventory.
	 */
	private PlayerStateValidator createValidatorWithNullInventory()
	{
		Client client = mock(Client.class);
		when(client.getItemContainer(InventoryID.INV)).thenReturn(null);
		return new PlayerStateValidator(client);
	}

	// -----------------------------------------------------------------------
	// Resurrect Greater Ghost (Thralls) — Fire x10, Blood x5, Cosmic x1
	// -----------------------------------------------------------------------

	/**
	 * Property: Having at least the required runes should always pass Thralls validation.
	 */
	@Property
	void exactRunesForResurrectGreaterGhostShouldPass(
		@ForAll @IntRange(min = 10, max = 100) int fireRunes,
		@ForAll @IntRange(min = 5, max = 100) int bloodRunes,
		@ForAll @IntRange(min = 1, max = 100) int cosmicRunes
	)
	{
		Item[] items = new Item[]{
			createItem(FIRE_RUNE_ID, fireRunes),
			createItem(BLOOD_RUNE_ID, bloodRunes),
			createItem(COSMIC_RUNE_ID, cosmicRunes),
			createItem(BOOK_OF_THE_DEAD_ID, 1)
		};
		PlayerStateValidator validator = createValidator(items);

		boolean result = validator.hasResurrectGreaterGhostRunes();
		assertThat(result)
			.as("Should pass with Fire:%d, Blood:%d, Cosmic:%d", fireRunes, bloodRunes, cosmicRunes)
			.isTrue();
	}

	/**
	 * Property: Being short on any rune type should fail Thralls validation.
	 */
	@Property
	void missingOneRuneTypeShouldFail(
		@ForAll @IntRange(max = 9) int fireRunes,
		@ForAll @IntRange(max = 4) int bloodRunes,
		@ForAll @IntRange(max = 0) int cosmicRunes
	)
	{
		Item[] items = new Item[]{
			createItem(FIRE_RUNE_ID, fireRunes),
			createItem(BLOOD_RUNE_ID, bloodRunes),
			createItem(COSMIC_RUNE_ID, cosmicRunes),
			createItem(BOOK_OF_THE_DEAD_ID, 1)
		};
		PlayerStateValidator validator = createValidator(items);

		boolean result = validator.hasResurrectGreaterGhostRunes();
		assertThat(result)
			.as("Should fail with Fire:%d, Blood:%d, Cosmic:%d", fireRunes, bloodRunes, cosmicRunes)
			.isFalse();
	}

	/**
	 * Property: Aether runes can substitute for Cosmic runes in Thralls.
	 */
	@Property
	void aetherRunesCanSubstituteForCosmicInThralls(
		@ForAll @IntRange(max = 0) int cosmicRunes,
		@ForAll @IntRange(min = 1, max = 10) int aetherRunes
	)
	{
		Item[] items = new Item[]{
			createItem(FIRE_RUNE_ID, 10),
			createItem(BLOOD_RUNE_ID, 5),
			createItem(COSMIC_RUNE_ID, cosmicRunes),
			createItem(AETHER_RUNE_ID, aetherRunes),
			createItem(BOOK_OF_THE_DEAD_ID, 1)
		};
		PlayerStateValidator validator = createValidator(items);

		boolean result = validator.hasResurrectGreaterGhostRunes();
		assertThat(result)
			.as("Should pass with Cosmic:%d + Aether:%d", cosmicRunes, aetherRunes)
			.isTrue();
	}

	/**
	 * Property: Lava runes can substitute for Fire runes in Thralls.
	 */
	@Property
	void lavaRunesCanSubstituteForFireInThralls(
		@ForAll @IntRange(max = 9) int fireRunes,
		@ForAll @IntRange(min = 1, max = 20) int lavaRunes
	)
	{
		int totalFireEquivalent = fireRunes + lavaRunes;
		boolean shouldPass = totalFireEquivalent >= 10;

		Item[] items = new Item[]{
			createItem(FIRE_RUNE_ID, fireRunes),
			createItem(CombinationRune.LAVA.getItemId(), lavaRunes),
			createItem(BLOOD_RUNE_ID, 5),
			createItem(COSMIC_RUNE_ID, 1),
			createItem(BOOK_OF_THE_DEAD_ID, 1)
		};
		PlayerStateValidator validator = createValidator(items);

		boolean result = validator.hasResurrectGreaterGhostRunes();
		assertThat(result)
			.as("With Fire:%d + Lava:%d (total:%d), expected:%s", fireRunes, lavaRunes, totalFireEquivalent, shouldPass)
			.isEqualTo(shouldPass);
	}

	// -----------------------------------------------------------------------
	// Death Charge — Death x1, Blood x1, Soul x1
	// -----------------------------------------------------------------------

	/**
	 * Property: Death Charge requires exactly 1 of each rune type.
	 */
	@Property
	void deathChargeWithExactRunesShouldPass(
		@ForAll @IntRange(min = 1, max = 100) int deathRunes,
		@ForAll @IntRange(min = 1, max = 100) int bloodRunes,
		@ForAll @IntRange(min = 1, max = 100) int soulRunes
	)
	{
		Item[] items = new Item[]{
			createItem(DEATH_RUNE_ID, deathRunes),
			createItem(BLOOD_RUNE_ID, bloodRunes),
			createItem(SOUL_RUNE_ID, soulRunes)
		};
		PlayerStateValidator validator = createValidator(items);

		boolean result = validator.hasDeathChargeRunes();
		assertThat(result)
			.as("Should pass with Death:%d, Blood:%d, Soul:%d", deathRunes, bloodRunes, soulRunes)
			.isTrue();
	}

	/**
	 * Property: Aether runes can substitute for Soul runes in Death Charge.
	 */
	@Property
	void deathChargeWithAetherForSoulShouldPass(
		@ForAll @IntRange(min = 1, max = 100) int deathRunes,
		@ForAll @IntRange(min = 1, max = 100) int bloodRunes,
		@ForAll @IntRange(min = 1, max = 100) int aetherRunes
	)
	{
		Item[] items = new Item[]{
			createItem(DEATH_RUNE_ID, deathRunes),
			createItem(BLOOD_RUNE_ID, bloodRunes),
			createItem(AETHER_RUNE_ID, aetherRunes)
		};
		PlayerStateValidator validator = createValidator(items);

		boolean result = validator.hasDeathChargeRunes();
		assertThat(result)
			.as("Should pass with Death:%d, Blood:%d, Aether:%d", deathRunes, bloodRunes, aetherRunes)
			.isTrue();
	}

	// -----------------------------------------------------------------------
	// Humidify — Astral x1, Fire x1, Water x1
	// -----------------------------------------------------------------------

	/**
	 * Property: Having at least the required runes should always pass Humidify validation.
	 */
	@Property
	void exactRunesForHumidifyShouldPass(
		@ForAll @IntRange(min = 1, max = 100) int astralRunes,
		@ForAll @IntRange(min = 1, max = 100) int fireRunes,
		@ForAll @IntRange(min = 1, max = 100) int waterRunes
	)
	{
		Item[] items = new Item[]{
			createItem(ASTRAL_RUNE_ID, astralRunes),
			createItem(FIRE_RUNE_ID, fireRunes),
			createItem(WATER_RUNE_ID, waterRunes)
		};
		PlayerStateValidator validator = createValidator(items);

		boolean result = validator.hasHumidifyRunes();
		assertThat(result)
			.as("Should pass with Astral:%d, Fire:%d, Water:%d", astralRunes, fireRunes, waterRunes)
			.isTrue();
	}

	/**
	 * Property: Missing any rune should fail Humidify validation.
	 */
	@Property
	void missingRunesForHumidifyShouldFail()
	{
		PlayerStateValidator validator = createValidator(new Item[]{
			createItem(ASTRAL_RUNE_ID, 0),
			createItem(FIRE_RUNE_ID, 1),
			createItem(WATER_RUNE_ID, 1)
		});
		assertThat(validator.hasHumidifyRunes()).isFalse();

		validator = createValidator(new Item[]{
			createItem(ASTRAL_RUNE_ID, 1),
			createItem(FIRE_RUNE_ID, 0),
			createItem(WATER_RUNE_ID, 1)
		});
		assertThat(validator.hasHumidifyRunes()).isFalse();

		validator = createValidator(new Item[]{
			createItem(ASTRAL_RUNE_ID, 1),
			createItem(FIRE_RUNE_ID, 1),
			createItem(WATER_RUNE_ID, 0)
		});
		assertThat(validator.hasHumidifyRunes()).isFalse();
	}

	/**
	 * Property: Steam runes can substitute for both Fire and Water in Humidify.
	 */
	@Property
	void steamRunesCanSubstituteForFireAndWaterInHumidify(
		@ForAll @IntRange(min = 1, max = 10) int steamRunes
	)
	{
		Item[] items = new Item[]{
			createItem(ASTRAL_RUNE_ID, 1),
			createItem(CombinationRune.STEAM.getItemId(), steamRunes)
		};
		PlayerStateValidator validator = createValidator(items);

		boolean result = validator.hasHumidifyRunes();
		assertThat(result)
			.as("Steam rune should cover Fire and Water requirements for Humidify")
			.isTrue();
	}

	/**
	 * Property: Mist runes can substitute for Water in Humidify.
	 */
	@Property
	void mistRunesCanSubstituteForWaterInHumidify(
		@ForAll @IntRange(min = 1, max = 10) int mistRunes
	)
	{
		Item[] items = new Item[]{
			createItem(ASTRAL_RUNE_ID, 1),
			createItem(FIRE_RUNE_ID, 1),
			createItem(CombinationRune.MIST.getItemId(), mistRunes)
		};
		PlayerStateValidator validator = createValidator(items);

		boolean result = validator.hasHumidifyRunes();
		assertThat(result)
			.as("Mist rune should substitute for Water in Humidify")
			.isTrue();
	}

	// -----------------------------------------------------------------------
	// Vengeance — Earth x10, Astral x4, Death x2
	// -----------------------------------------------------------------------

	/**
	 * Property: Having at least the required runes should always pass Vengeance validation.
	 */
	@Property
	void exactRunesForVengeanceShouldPass(
		@ForAll @IntRange(min = 10, max = 100) int earthRunes,
		@ForAll @IntRange(min = 4, max = 100) int astralRunes,
		@ForAll @IntRange(min = 2, max = 100) int deathRunes
	)
	{
		Item[] items = new Item[]{
			createItem(EARTH_RUNE_ID, earthRunes),
			createItem(ASTRAL_RUNE_ID, astralRunes),
			createItem(DEATH_RUNE_ID, deathRunes)
		};
		PlayerStateValidator validator = createValidator(items);

		boolean result = validator.hasVengeanceRunes();
		assertThat(result)
			.as("Should pass with Earth:%d, Astral:%d, Death:%d", earthRunes, astralRunes, deathRunes)
			.isTrue();
	}

	/**
	 * Property: Being short on any rune type should fail Vengeance validation.
	 */
	@Property
	void missingRunesForVengeanceShouldFail(
		@ForAll @IntRange(max = 9) int earthRunes,
		@ForAll @IntRange(max = 3) int astralRunes,
		@ForAll @IntRange(max = 1) int deathRunes
	)
	{
		Item[] items = new Item[]{
			createItem(EARTH_RUNE_ID, earthRunes),
			createItem(ASTRAL_RUNE_ID, astralRunes),
			createItem(DEATH_RUNE_ID, deathRunes)
		};
		PlayerStateValidator validator = createValidator(items);

		boolean result = validator.hasVengeanceRunes();
		assertThat(result)
			.as("Should fail with Earth:%d, Astral:%d, Death:%d", earthRunes, astralRunes, deathRunes)
			.isFalse();
	}

	/**
	 * Property: Mud runes can substitute for Earth in Vengeance.
	 */
	@Property
	void mudRunesCanSubstituteForEarthInVengeance(
		@ForAll @IntRange(max = 9) int earthRunes,
		@ForAll @IntRange(min = 1, max = 20) int mudRunes
	)
	{
		int totalEarthEquivalent = earthRunes + mudRunes;
		boolean shouldPass = totalEarthEquivalent >= 10;

		Item[] items = new Item[]{
			createItem(EARTH_RUNE_ID, earthRunes),
			createItem(CombinationRune.MUD.getItemId(), mudRunes),
			createItem(ASTRAL_RUNE_ID, 4),
			createItem(DEATH_RUNE_ID, 2)
		};
		PlayerStateValidator validator = createValidator(items);

		boolean result = validator.hasVengeanceRunes();
		assertThat(result)
			.as("With Earth:%d + Mud:%d (total:%d), expected:%s", earthRunes, mudRunes, totalEarthEquivalent, shouldPass)
			.isEqualTo(shouldPass);
	}

	// -----------------------------------------------------------------------
	// Infinite rune sources — staves, charged tomes, Kodai wand
	// -----------------------------------------------------------------------

	/**
	 * A fire staff in the inventory satisfies the Thralls fire-rune requirement
	 * even with zero Fire runes carried.
	 */
	@Example
	void fireStaffSatisfiesThrallsFireRequirement()
	{
		Item[] items = new Item[]{
			createItem(ItemID.FIRE_BATTLESTAFF, 1),
			createItem(BLOOD_RUNE_ID, 5),
			createItem(COSMIC_RUNE_ID, 1),
			createItem(BOOK_OF_THE_DEAD_ID, 1)
		};
		PlayerStateValidator validator = createValidator(items);

		assertThat(validator.hasResurrectGreaterGhostRunes())
			.as("Fire battlestaff should cover the 10 Fire rune requirement for Thralls")
			.isTrue();
	}

	/**
	 * A charged Tome of Water satisfies the Ice Barrage water-rune requirement;
	 * the uncharged tome does not.
	 */
	@Example
	void tomeOfWaterSatisfiesIceBarrageWaterRequirement()
	{
		Item[] withChargedTome = new Item[]{
			createItem(ItemID.TOME_OF_WATER, 1),
			createItem(DEATH_RUNE_ID, 2),
			createItem(BLOOD_RUNE_ID, 4)
		};
		assertThat(createValidator(withChargedTome).hasIceBarrageRunes())
			.as("Charged Tome of Water should cover the Water rune requirement")
			.isTrue();

		Item[] withUnchargedTome = new Item[]{
			createItem(ItemID.TOME_OF_WATER_UNCHARGED, 1),
			createItem(DEATH_RUNE_ID, 2),
			createItem(BLOOD_RUNE_ID, 4)
		};
		assertThat(createValidator(withUnchargedTome).hasIceBarrageRunes())
			.as("Uncharged Tome of Water should NOT cover the Water rune requirement")
			.isFalse();
	}

	/**
	 * Regression: the Kodai wand still provides infinite water runes for Ice Barrage
	 * after the special case was folded into the generic infinite-rune handling.
	 */
	@Example
	void kodaiWandStillSatisfiesIceBarrageWaterRequirement()
	{
		Item[] items = new Item[]{
			createItem(ItemID.KODAI_WAND, 1),
			createItem(DEATH_RUNE_ID, 2),
			createItem(BLOOD_RUNE_ID, 4)
		};
		PlayerStateValidator validator = createValidator(items);

		assertThat(validator.hasIceBarrageRunes())
			.as("Kodai wand should cover the Water rune requirement for Ice Barrage")
			.isTrue();
	}

	/**
	 * A combination staff covers both of its elements: Steam battlestaff
	 * satisfies the Fire and Water requirements of Humidify.
	 */
	@Example
	void steamStaffCoversFireAndWaterForHumidify()
	{
		Item[] items = new Item[]{
			createItem(ItemID.STEAM_BATTLESTAFF, 1),
			createItem(ASTRAL_RUNE_ID, 1)
		};
		PlayerStateValidator validator = createValidator(items);

		assertThat(validator.hasHumidifyRunes())
			.as("Steam battlestaff should cover Fire and Water requirements for Humidify")
			.isTrue();
	}

	/**
	 * An infinite rune source only covers its own runes — a water staff does not
	 * excuse the missing Death and Blood runes for Ice Barrage.
	 */
	@Example
	void waterStaffDoesNotCoverNonElementalRunes()
	{
		Item[] items = new Item[]{
			createItem(ItemID.WATER_BATTLESTAFF, 1)
		};
		PlayerStateValidator validator = createValidator(items);

		assertThat(validator.hasIceBarrageRunes())
			.as("Water battlestaff alone should not satisfy Death/Blood requirements")
			.isFalse();
	}

	/**
	 * An equipped (worn) staff counts as an infinite rune source, matching the
	 * previous Kodai wand behaviour.
	 */
	@Example
	void equippedStaffCountsAsInfiniteRuneSource()
	{
		Client client = mock(Client.class);
		ItemContainer inventory = mock(ItemContainer.class);
		ItemContainer equipment = mock(ItemContainer.class);

		Item[] inventoryItems = new Item[]{
			createItem(BLOOD_RUNE_ID, 5),
			createItem(COSMIC_RUNE_ID, 1),
			createItem(BOOK_OF_THE_DEAD_ID, 1)
		};
		Item[] equipmentItems = new Item[]{
			createItem(ItemID.MYSTIC_FIRE_STAFF, 1)
		};

		when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(equipment);
		when(inventory.getItems()).thenReturn(inventoryItems);
		when(equipment.getItems()).thenReturn(equipmentItems);

		PlayerStateValidator validator = new PlayerStateValidator(client);

		assertThat(validator.hasResurrectGreaterGhostRunes())
			.as("Equipped mystic fire staff should cover the Fire rune requirement for Thralls")
			.isTrue();
	}

	/**
	 * Symmetry with the Tome of Water case: an uncharged Tome of Fire provides nothing
	 * and must not satisfy the Thralls fire requirement.
	 */
	@Example
	void unchargedTomeOfFireDoesNotSatisfyThrallsFireRequirement()
	{
		Item[] items = new Item[]{
			createItem(ItemID.TOME_OF_FIRE_UNCHARGED, 1),
			createItem(BLOOD_RUNE_ID, 5),
			createItem(COSMIC_RUNE_ID, 1),
			createItem(BOOK_OF_THE_DEAD_ID, 1)
		};
		assertThat(createValidator(items).hasResurrectGreaterGhostRunes())
			.as("Uncharged Tome of Fire should NOT cover the Fire rune requirement")
			.isFalse();
	}

	// -----------------------------------------------------------------------
	// Blood Barrage — Blood x4, Soul x1, Death x4
	// -----------------------------------------------------------------------

	/**
	 * Property: Having at least the required runes should pass Blood Barrage validation.
	 */
	@Property
	void bloodBarrageWithExactRunesShouldPass(
		@ForAll @IntRange(min = 4, max = 100) int bloodRunes,
		@ForAll @IntRange(min = 1, max = 100) int soulRunes,
		@ForAll @IntRange(min = 4, max = 100) int deathRunes
	)
	{
		Item[] items = new Item[]{
			createItem(BLOOD_RUNE_ID, bloodRunes),
			createItem(SOUL_RUNE_ID, soulRunes),
			createItem(DEATH_RUNE_ID, deathRunes)
		};
		PlayerStateValidator validator = createValidator(items);

		assertThat(validator.hasBloodBarrageRunes())
			.as("Should pass with Blood:%d, Soul:%d, Death:%d", bloodRunes, soulRunes, deathRunes)
			.isTrue();
	}

	/**
	 * Regression guard for review fix #5: Blood Barrage requires 4 Death runes
	 * (previously documented as 1), so three Death runes must fail.
	 */
	@Example
	void bloodBarrageWithThreeDeathRunesShouldFail()
	{
		Item[] items = new Item[]{
			createItem(BLOOD_RUNE_ID, 4),
			createItem(SOUL_RUNE_ID, 1),
			createItem(DEATH_RUNE_ID, 3)
		};
		assertThat(createValidator(items).hasBloodBarrageRunes())
			.as("Blood Barrage should require 4 Death runes, so 3 must fail")
			.isFalse();
	}

	/**
	 * Property: Aether runes can substitute for the Soul rune in Blood Barrage.
	 */
	@Property
	void bloodBarrageWithAetherForSoulShouldPass(
		@ForAll @IntRange(min = 1, max = 100) int aetherRunes
	)
	{
		Item[] items = new Item[]{
			createItem(BLOOD_RUNE_ID, 4),
			createItem(DEATH_RUNE_ID, 4),
			createItem(AETHER_RUNE_ID, aetherRunes)
		};
		PlayerStateValidator validator = createValidator(items);

		assertThat(validator.hasBloodBarrageRunes())
			.as("Aether should substitute for Soul in Blood Barrage (Aether:%d)", aetherRunes)
			.isTrue();
	}

	// -----------------------------------------------------------------------
	// Rune pouch — counting and stale-varbit guard
	// -----------------------------------------------------------------------

	/**
	 * Runes stored in the rune pouch contribute to requirement checks when the
	 * pouch is physically present in the inventory.
	 */
	@Example
	void runePouchRunesCountTowardRequirements()
	{
		Item[] inventory = new Item[]{
			createItem(RUNE_POUCH_ID, 1),
			createItem(BOOK_OF_THE_DEAD_ID, 1)
		};
		PlayerStateValidator validator = createValidatorWithRunePouch(
			inventory,
			new int[]{FIRE_RUNE_ID, BLOOD_RUNE_ID, COSMIC_RUNE_ID},
			new int[]{10, 5, 1});

		assertThat(validator.hasResurrectGreaterGhostRunes())
			.as("Runes stored in the rune pouch should count toward Thralls")
			.isTrue();
	}

	/**
	 * Stale-varbit guard: pouch varbits must be ignored when no pouch item is in the
	 * inventory (the varbits persist after the pouch is deposited).
	 */
	@Example
	void runePouchIgnoredWhenPouchNotInInventory()
	{
		Item[] inventory = new Item[]{
			createItem(BOOK_OF_THE_DEAD_ID, 1) // pouch item intentionally absent
		};
		PlayerStateValidator validator = createValidatorWithRunePouch(
			inventory,
			new int[]{FIRE_RUNE_ID, BLOOD_RUNE_ID, COSMIC_RUNE_ID},
			new int[]{10, 5, 1});

		assertThat(validator.hasResurrectGreaterGhostRunes())
			.as("Rune pouch varbits must be ignored when no pouch is in the inventory")
			.isFalse();
	}

	/**
	 * All rune-pouch variants (regular and the divine variants) enable pouch reading.
	 */
	@Property
	void divineRunePouchVariantsAreRecognized(
		@ForAll("runePouchIds") int pouchId
	)
	{
		Item[] inventory = new Item[]{
			createItem(pouchId, 1),
			createItem(BOOK_OF_THE_DEAD_ID, 1)
		};
		PlayerStateValidator validator = createValidatorWithRunePouch(
			inventory,
			new int[]{FIRE_RUNE_ID, BLOOD_RUNE_ID, COSMIC_RUNE_ID},
			new int[]{10, 5, 1});

		assertThat(validator.hasResurrectGreaterGhostRunes())
			.as("Pouch variant %d should enable rune-pouch reading", pouchId)
			.isTrue();
	}

	@Provide
	Arbitrary<Integer> runePouchIds()
	{
		// Regular, Divine, Divine (old), Divine (locked)
		return Arbitraries.of(12791, 27281, 27086, 27509);
	}

	// -----------------------------------------------------------------------
	// Spellbook discrimination
	// -----------------------------------------------------------------------

	/**
	 * Exactly one spellbook predicate is true for each SPELLBOOK varbit value
	 * (1 Ancient, 2 Lunar, 3 Arceuus), and none are true for the standard book (0).
	 */
	@Property
	void spellbookPredicatesMatchVarbit(
		@ForAll @IntRange(min = 0, max = 3) int spellbook
	)
	{
		Client client = mock(Client.class);
		when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(spellbook);
		PlayerStateValidator validator = new PlayerStateValidator(client);

		assertThat(validator.isOnAncientSpellbook()).as("Ancient").isEqualTo(spellbook == 1);
		assertThat(validator.isOnLunarSpellbook()).as("Lunar").isEqualTo(spellbook == 2);
		assertThat(validator.isOnArceuusSpellbook()).as("Arceuus").isEqualTo(spellbook == 3);
	}

	// -----------------------------------------------------------------------
	// Item presence checks
	// -----------------------------------------------------------------------

	@Example
	void chugJugDetectedWhenPresentAndAbsent()
	{
		assertThat(createValidator(new Item[]{createItem(ItemID.MM_PREPOT_DEVICE, 1)}).hasChugJug())
			.as("Chug Jug present").isTrue();
		assertThat(createValidator(new Item[]{createItem(FIRE_RUNE_ID, 1)}).hasChugJug())
			.as("Chug Jug absent").isFalse();
	}

	@Example
	void saturatedHeartDetectedWhenPresentAndAbsent()
	{
		assertThat(createValidator(new Item[]{createItem(ItemID.SATURATED_HEART, 1)}).hasSaturatedHeart())
			.as("Saturated Heart present").isTrue();
		assertThat(createValidator(new Item[]{createItem(FIRE_RUNE_ID, 1)}).hasSaturatedHeart())
			.as("Saturated Heart absent").isFalse();
	}

	// -----------------------------------------------------------------------
	// General invariants
	// -----------------------------------------------------------------------

	/**
	 * Property: Empty inventory should always fail validation.
	 */
	@Property
	void emptyInventoryShouldFail()
	{
		PlayerStateValidator validator = createValidator(new Item[0]);

		assertThat(validator.hasResurrectGreaterGhostRunes()).as("Thralls").isFalse();
		assertThat(validator.hasDeathChargeRunes()).as("Death Charge").isFalse();
		assertThat(validator.hasHumidifyRunes()).as("Humidify").isFalse();
		assertThat(validator.hasVengeanceRunes()).as("Vengeance").isFalse();
		assertThat(validator.hasIceBarrageRunes()).as("Ice Barrage").isFalse();
		assertThat(validator.hasBloodBarrageRunes()).as("Blood Barrage").isFalse();
	}

	/**
	 * Property: Null inventory should not crash and should fail validation.
	 */
	@Property
	void nullInventoryShouldNotCrash()
	{
		PlayerStateValidator validator = createValidatorWithNullInventory();

		assertThat(validator.hasResurrectGreaterGhostRunes()).as("Thralls").isFalse();
		assertThat(validator.hasDeathChargeRunes()).as("Death Charge").isFalse();
		assertThat(validator.hasHumidifyRunes()).as("Humidify").isFalse();
		assertThat(validator.hasVengeanceRunes()).as("Vengeance").isFalse();
		assertThat(validator.hasBookOfTheDead()).as("Book of the Dead").isFalse();
	}

	/**
	 * Property: Book of the Dead detection should work regardless of inventory position.
	 */
	@Property
	void bookOfTheDeadDetectionIsPositionIndependent(
		@ForAll @IntRange(max = 27) int bookPosition
	)
	{
		Item[] items = new Item[28];
		for (int i = 0; i < 28; i++)
		{
			if (i == bookPosition)
			{
				items[i] = createItem(BOOK_OF_THE_DEAD_ID, 1);
			}
			else
			{
				items[i] = createItem(-1, 0); // Empty slot
			}
		}
		PlayerStateValidator validator = createValidator(items);

		boolean result = validator.hasBookOfTheDead();
		assertThat(result)
			.as("Should find book at position %d", bookPosition)
			.isTrue();
	}

	/**
	 * Book of the Dead equipped (not in inventory) should still pass validation.
	 */
	@Property
	void bookOfTheDeadDetectionWorksWhenEquipped(
		@ForAll @IntRange(max = 12) int equipmentSlot
	)
	{
		final int EQUIPMENT_CONTAINER_ID = 94;

		Client client = mock(Client.class);
		ItemContainer inventory = mock(ItemContainer.class);
		ItemContainer equipment = mock(ItemContainer.class);
		EnumComposition runepouchEnum = mock(EnumComposition.class);

		// Empty inventory
		Item[] inventoryItems = new Item[28];
		for (int i = 0; i < 28; i++)
		{
			inventoryItems[i] = createItem(-1, 0);
		}

		// Book in equipment at a random slot
		Item[] equipmentItems = new Item[13];
		for (int i = 0; i < 13; i++)
		{
			equipmentItems[i] = (i == equipmentSlot)
				? createItem(BOOK_OF_THE_DEAD_ID, 1)
				: createItem(-1, 0);
		}

		when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
		when(client.getItemContainer(EQUIPMENT_CONTAINER_ID)).thenReturn(equipment);
		when(client.getEnum(EnumID.RUNEPOUCH_RUNE)).thenReturn(runepouchEnum);
		when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(3);
		when(inventory.getItems()).thenReturn(inventoryItems);
		when(equipment.getItems()).thenReturn(equipmentItems);

		when(client.getVarbitValue(VarbitID.RUNE_POUCH_TYPE_1)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_TYPE_2)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_TYPE_3)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_TYPE_4)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_QUANTITY_1)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_QUANTITY_2)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_QUANTITY_3)).thenReturn(0);
		when(client.getVarbitValue(VarbitID.RUNE_POUCH_QUANTITY_4)).thenReturn(0);

		PlayerStateValidator validator = new PlayerStateValidator(client);

		assertThat(validator.hasBookOfTheDead())
			.as("Should find Book of the Dead in equipment slot %d", equipmentSlot)
			.isTrue();
	}

	// Helper methods

	private Item createItem(int id, int quantity)
	{
		return new Item(id, quantity);
	}

	/**
	 * Build a validator whose rune pouch holds the given runes. Slot i stores
	 * {@code runeItemIds[i]} (mapped through the rune-pouch enum) with quantity
	 * {@code amounts[i]}; remaining slots are empty. The caller controls whether a
	 * pouch item is actually in {@code inventoryItems} to exercise the stale-varbit guard.
	 */
	private PlayerStateValidator createValidatorWithRunePouch(
		Item[] inventoryItems, int[] runeItemIds, int[] amounts)
	{
		Client client = mock(Client.class);
		ItemContainer inventory = mock(ItemContainer.class);
		EnumComposition runepouchEnum = mock(EnumComposition.class);

		when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
		when(client.getEnum(EnumID.RUNEPOUCH_RUNE)).thenReturn(runepouchEnum);
		when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(3); // Arceuus
		when(inventory.getItems()).thenReturn(inventoryItems);

		int[] typeVarbits = {
			VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2,
			VarbitID.RUNE_POUCH_TYPE_3, VarbitID.RUNE_POUCH_TYPE_4
		};
		int[] amountVarbits = {
			VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2,
			VarbitID.RUNE_POUCH_QUANTITY_3, VarbitID.RUNE_POUCH_QUANTITY_4
		};

		for (int i = 0; i < 4; i++)
		{
			if (i < runeItemIds.length)
			{
				int enumKey = i + 1; // arbitrary non-zero enum key per slot
				when(client.getVarbitValue(typeVarbits[i])).thenReturn(enumKey);
				when(client.getVarbitValue(amountVarbits[i])).thenReturn(amounts[i]);
				when(runepouchEnum.getIntValue(enumKey)).thenReturn(runeItemIds[i]);
			}
			else
			{
				when(client.getVarbitValue(typeVarbits[i])).thenReturn(0);
				when(client.getVarbitValue(amountVarbits[i])).thenReturn(0);
			}
		}

		return new PlayerStateValidator(client);
	}
}
