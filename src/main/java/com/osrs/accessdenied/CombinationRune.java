package com.osrs.accessdenied;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

/**
 * Represents combination runes that can substitute for one or more standard rune types.
 * Each constant defines the item ID of the combination rune and the set of rune IDs it
 * can stand in for when checking spell requirements.
 */
@Getter
public enum CombinationRune
{
	/**
	 * Aether rune (ID 30843).
	 * Substitutes for Soul runes (566) and Cosmic runes (564).
	 */
	AETHER(30843, 566, 564),

	/**
	 * Lava rune (ID 4699).
	 * Substitutes for Fire runes (554) and Earth runes (557).
	 */
	LAVA(4699, 554, 557),

	/**
	 * Mist rune (ID 4695).
	 * Substitutes for Air runes (556) and Water runes (555).
	 */
	MIST(4695, 556, 555),

	/**
	 * Mud rune (ID 4698).
	 * Substitutes for Earth runes (557) and Water runes (555).
	 */
	MUD(4698, 557, 555),

	/**
	 * Smoke rune (ID 4697).
	 * Substitutes for Air runes (556) and Fire runes (554).
	 */
	SMOKE(4697, 556, 554),

	/**
	 * Steam rune (ID 4694).
	 * Substitutes for Fire runes (554) and Water runes (555).
	 */
	STEAM(4694, 554, 555);

	/**
	 * -- GETTER --
	 *  Returns the item ID of this combination rune.
	 *
	 */
	private final int itemId;
	/**
	 * -- GETTER --
	 *  Returns the set of rune item IDs that this combination rune can substitute for.
	 *
	 */
	private final Set<Integer> substitutesFor;

	CombinationRune(int itemId, int... runeIds)
	{
		this.itemId = itemId;
		Set<Integer> set = new HashSet<>();
		for (int id : runeIds)
		{
			set.add(id);
		}
		this.substitutesFor = Collections.unmodifiableSet(set);
	}

	/**
	 * Returns whether this combination rune can substitute for the given rune ID.
	 *
	 * @param runeId the rune item ID to check
	 * @return true if this combination rune substitutes for the given rune
	 */
	public boolean canSubstituteFor(int runeId)
	{
		return substitutesFor.contains(runeId);
	}

	/**
	 * Returns all combination runes that can substitute for the given rune ID.
	 *
	 * @param runeId the rune item ID to check
	 * @return array of combination runes that substitute for the given rune
	 */
	public static CombinationRune[] getSubstitutesForRune(int runeId)
	{
		return Arrays.stream(values())
			.filter(r -> r.canSubstituteFor(runeId))
			.toArray(CombinationRune[]::new);
	}
}
