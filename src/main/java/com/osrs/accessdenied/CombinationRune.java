package com.osrs.accessdenied;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

/**
 * Combination runes and the standard rune types each one stands in for when checking
 * spell requirements. Constructor arguments are the combination rune's item ID followed
 * by the rune IDs it covers.
 */
@Getter
public enum CombinationRune
{
	AETHER(30843, 566, 564),
	LAVA(4699, 554, 557),
	MIST(4695, 556, 555),
	MUD(4698, 557, 555),
	SMOKE(4697, 556, 554),
	STEAM(4694, 554, 555);

	private final int itemId;
	private final Set<Integer> substitutesFor;

	/**
	 * Reverse index, precomputed because the lookup runs per rune type on every validation.
	 */
	private static final Map<Integer, List<CombinationRune>> BY_SUBSTITUTED_RUNE = buildIndex();

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

	private static Map<Integer, List<CombinationRune>> buildIndex()
	{
		Map<Integer, List<CombinationRune>> index = new HashMap<>();
		for (CombinationRune rune : values())
		{
			for (int runeId : rune.substitutesFor)
			{
				index.computeIfAbsent(runeId, id -> new ArrayList<>()).add(rune);
			}
		}
		index.replaceAll((runeId, runes) -> List.copyOf(runes));
		return index;
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
	 * @return the combination runes that substitute for the given rune, empty if none do
	 */
	public static List<CombinationRune> getSubstitutesForRune(int runeId)
	{
		return BY_SUBSTITUTED_RUNE.getOrDefault(runeId, List.of());
	}
}
