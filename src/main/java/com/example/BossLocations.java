package com.example;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Registry of all supported boss locations and their associated game objects.
 * Add new locations here as the plugin is extended.
 */
public class BossLocations
{
	/**
	 * Nex boss location in the God Wars Dungeon
	 * Region ID: 11601
	 */
	public static final BossLocation NEX = new BossLocation(
		"nex",
		"Nex",
		Collections.singleton(11601)
	);

	/**
	 * Game object IDs that require validation within the Nex region.
	 * Object ID 42967 = NEX_FIGHT_BARRIER (room available)
	 * Object ID 42968 = NEX_FIGHT_BARRIER_BUSY (room busy) - not validated
	 */
	public static final Set<Integer> NEX_OBJECTS = Collections.singleton(42967);

	/**
	 * All supported boss locations.
	 * Add new locations to this set as they are implemented.
	 */
	public static final Set<BossLocation> ALL_LOCATIONS;

	/**
	 * Map of location ID to the set of game object IDs that require validation in that location.
	 */
	public static final Map<String, Set<Integer>> LOCATION_OBJECTS;

	static
	{
		Set<BossLocation> locations = new HashSet<>();
		locations.add(NEX);
		// Add future locations here:
		// locations.add(COLOSSEUM);
		// locations.add(INFERNO);
		// etc.
		
		ALL_LOCATIONS = Collections.unmodifiableSet(locations);

		// Map locations to their game objects
		Map<String, Set<Integer>> objectMap = new HashMap<>();
		objectMap.put(NEX.getId(), NEX_OBJECTS);
		// Add future location objects here:
		// objectMap.put(COLOSSEUM.getId(), COLOSSEUM_OBJECTS);
		
		LOCATION_OBJECTS = Collections.unmodifiableMap(objectMap);
	}

	/**
	 * Find a boss location by region ID
	 */
	public static BossLocation findByRegion(int regionId)
	{
		for (BossLocation location : ALL_LOCATIONS)
		{
			if (location.isInRegion(regionId))
			{
				return location;
			}
		}
		return null;
	}

	/**
	 * Find a boss location by any of the given regions
	 */
	public static BossLocation findByAnyRegion(int[] regions)
	{
		if (regions == null)
		{
			return null;
		}

		for (BossLocation location : ALL_LOCATIONS)
		{
			if (location.isInAnyRegion(regions))
			{
				return location;
			}
		}
		return null;
	}

	/**
	 * Get the set of game object IDs that require validation for a location
	 */
	public static Set<Integer> getObjectsForLocation(BossLocation location)
	{
		if (location == null)
		{
			return Collections.emptySet();
		}
		return LOCATION_OBJECTS.getOrDefault(location.getId(), Collections.emptySet());
	}

	/**
	 * Check if an object ID requires validation in the given location
	 */
	public static boolean isValidatedObject(BossLocation location, int objectId)
	{
		Set<Integer> objects = getObjectsForLocation(location);
		return objects.contains(objectId);
	}
}
