package com.osrs.accessdenied;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Varbits;
import net.runelite.api.gameval.InventoryID;
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
	private static final int SOUL_RUNE_ID = 566;
	private static final int BLOOD_RUNE_ID = 565;
	private static final int COSMIC_RUNE_ID = 564;
	private static final int DEATH_RUNE_ID = 560;
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
		when(client.getVarbitValue(Varbits.SPELLBOOK)).thenReturn(3); // Arceuus spellbook
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

	/**
	 * Property: Having exactly the required runes should always pass validation.
	 */
	@Property
	void exactRunesForResurrectGreaterGhostShouldPass(
		@ForAll @IntRange(min = 4, max = 100) int soulRunes,
		@ForAll @IntRange(min = 2, max = 100) int bloodRunes,
		@ForAll @IntRange(min = 1, max = 100) int cosmicRunes
	)
	{
		// Setup inventory with exact or more runes
		Item[] items = new Item[]{
			createItem(SOUL_RUNE_ID, soulRunes),
			createItem(BLOOD_RUNE_ID, bloodRunes),
			createItem(COSMIC_RUNE_ID, cosmicRunes),
			createItem(BOOK_OF_THE_DEAD_ID, 1)
		};
		PlayerStateValidator validator = createValidator(items);

		// Should pass validation
		boolean result = validator.hasResurrectGreaterGhostRunes();
		assertThat(result)
			.as("Should pass with Soul:%d, Blood:%d, Cosmic:%d", soulRunes, bloodRunes, cosmicRunes)
			.isTrue();
	}

	/**
	 * Property: Missing even one required rune should fail validation.
	 */
	@Property
	void missingOneRuneTypeShouldFail(
		@ForAll @IntRange(min = 0, max = 3) int soulRunes,
		@ForAll @IntRange(min = 0, max = 1) int bloodRunes,
		@ForAll @IntRange(min = 0, max = 0) int cosmicRunes
	)
	{
		// At least one rune type is below requirement
		Item[] items = new Item[]{
			createItem(SOUL_RUNE_ID, soulRunes),
			createItem(BLOOD_RUNE_ID, bloodRunes),
			createItem(COSMIC_RUNE_ID, cosmicRunes),
			createItem(BOOK_OF_THE_DEAD_ID, 1)
		};
		PlayerStateValidator validator = createValidator(items);

		// Should fail validation
		boolean result = validator.hasResurrectGreaterGhostRunes();
		assertThat(result)
			.as("Should fail with Soul:%d, Blood:%d, Cosmic:%d", soulRunes, bloodRunes, cosmicRunes)
			.isFalse();
	}

	/**
	 * Property: Aether runes can substitute for Soul runes.
	 */
	@Property
	void aetherRunesCanSubstituteForSoulRunes(
		@ForAll @IntRange(min = 0, max = 3) int soulRunes,
		@ForAll @IntRange(min = 1, max = 10) int aetherRunes
	)
	{
		// Calculate if we have enough total
		int totalSoulEquivalent = soulRunes + aetherRunes;
		boolean shouldPass = totalSoulEquivalent >= 4;

		Item[] items = new Item[]{
			createItem(SOUL_RUNE_ID, soulRunes),
			createItem(AETHER_RUNE_ID, aetherRunes),
			createItem(BLOOD_RUNE_ID, 2),
			createItem(COSMIC_RUNE_ID, 1),
			createItem(BOOK_OF_THE_DEAD_ID, 1)
		};
		PlayerStateValidator validator = createValidator(items);

		boolean result = validator.hasResurrectGreaterGhostRunes();
		assertThat(result)
			.as("With Soul:%d + Aether:%d (total:%d), expected:%s", soulRunes, aetherRunes, totalSoulEquivalent, shouldPass)
			.isEqualTo(shouldPass);
	}

	/**
	 * Property: Aether runes can substitute for Cosmic runes.
	 */
	@Property
	void aetherRunesCanSubstituteForCosmicRunes(
		@ForAll @IntRange(min = 0, max = 0) int cosmicRunes,
		@ForAll @IntRange(min = 1, max = 10) int aetherRunes
	)
	{
		// With 0 cosmic but aether available, should pass
		Item[] items = new Item[]{
			createItem(SOUL_RUNE_ID, 4),
			createItem(BLOOD_RUNE_ID, 2),
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
	 * Property: Aether runes can substitute for both Soul AND Cosmic simultaneously.
	 */
	@Property
	void aetherRunesCanSubstituteForBothSoulAndCosmic(
		@ForAll @IntRange(min = 0, max = 3) int soulRunes,
		@ForAll @IntRange(min = 0, max = 0) int cosmicRunes,
		@ForAll @IntRange(min = 5, max = 10) int aetherRunes
	)
	{
		// Need 4 soul + 1 cosmic = 5 total
		// With enough aether, should always pass
		Item[] items = new Item[]{
			createItem(SOUL_RUNE_ID, soulRunes),
			createItem(BLOOD_RUNE_ID, 2),
			createItem(COSMIC_RUNE_ID, cosmicRunes),
			createItem(AETHER_RUNE_ID, aetherRunes),
			createItem(BOOK_OF_THE_DEAD_ID, 1)
		};
		PlayerStateValidator validator = createValidator(items);

		boolean result = validator.hasResurrectGreaterGhostRunes();
		assertThat(result)
			.as("Should pass with Soul:%d, Cosmic:%d, Aether:%d", soulRunes, cosmicRunes, aetherRunes)
			.isTrue();
	}

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
		// Aether can substitute for Soul
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

	/**
	 * Property: Empty inventory should always fail validation.
	 */
	@Property
	void emptyInventoryShouldFail()
	{
		PlayerStateValidator validator = createValidator(new Item[0]);

		boolean thrallResult = validator.hasResurrectGreaterGhostRunes();
		boolean deathChargeResult = validator.hasDeathChargeRunes();

		assertThat(thrallResult).as("Empty inventory should fail thrall validation").isFalse();
		assertThat(deathChargeResult).as("Empty inventory should fail death charge validation").isFalse();
	}

	/**
	 * Property: Null inventory should not crash and should fail validation.
	 */
	@Property
	void nullInventoryShouldNotCrash()
	{
		PlayerStateValidator validator = createValidatorWithNullInventory();

		boolean thrallResult = validator.hasResurrectGreaterGhostRunes();
		boolean deathChargeResult = validator.hasDeathChargeRunes();
		boolean bookResult = validator.hasBookOfTheDead();

		assertThat(thrallResult).as("Null inventory should fail thrall validation").isFalse();
		assertThat(deathChargeResult).as("Null inventory should fail death charge validation").isFalse();
		assertThat(bookResult).as("Null inventory should fail book check").isFalse();
	}

	/**
	 * Property: Book of the Dead detection should work regardless of inventory position.
	 */
	@Property
	void bookOfTheDeadDetectionIsPositionIndependent(
		@ForAll @IntRange(min = 0, max = 27) int bookPosition
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

	// Helper methods

	private Item createItem(int id, int quantity)
	{
		Item item = new Item(id, quantity);
		return item;
	}
}
