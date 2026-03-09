package com.osrs.accessdenied;

import java.util.Arrays;
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
	 * Theatre of Blood raid location
	 * Region ID: 14642
	 */
	public static final BossLocation THEATRE_OF_BLOOD = new BossLocation(
		"tob",
		"Theatre of Blood",
		Collections.singleton(14642)
	);

	/**
	 * Tombs of Amascut raid location
	 * Region ID: 13454
	 */
	public static final BossLocation TOMBS_OF_AMASCUT = new BossLocation(
		"toa",
		"Tombs of Amascut",
		Collections.singleton(13454)
	);

	/**
	 * Chambers of Xeric raid location
	 * Region IDs: 13393, 13137 (varies by raid layout)
	 */
	public static final BossLocation CHAMBERS_OF_XERIC = new BossLocation(
		"cox",
		"Chambers of Xeric",
		new HashSet<>(Arrays.asList(13393, 13137))
	);

	/**
	 * Inferno challenge location
	 * Region IDs: 10063, 9807
	 */
	public static final BossLocation INFERNO = new BossLocation(
		"inferno",
		"Inferno",
		new HashSet<>(Arrays.asList(10063, 9807))
	);

	/**
	 * Game object ID that requires validation within the Nex region.
	 * Object ID 42967 = NEX_FIGHT_BARRIER (room available)
	 * Object ID 42968 = NEX_FIGHT_BARRIER_BUSY (room busy) - not validated
	 */
	public static final int NEX_OBJECT = 42967;

	/**
	 * Game object ID that requires validation within the Theatre of Blood region.
	 * Object ID 32653 = TOB_ENTRANCE
	 */
	public static final int TOB_OBJECT = 32653;

	/**
	 * Game object ID that requires validation within the Tombs of Amascut region.
	 * Object ID 46089 = TOA_ENTRANCE
	 */
	public static final int TOA_OBJECT = 46089;

	/**
	 * Game object ID that requires validation within the Chambers of Xeric region.
	 * Object ID 29789 = COX_ENTRANCE
	 */
	public static final int COX_OBJECT = 29789;

	/**
	 * Game object ID that requires validation within the Inferno region.
	 * Object ID 30352 = INFERNO_ENTRANCE
	 */
	public static final int INFERNO_OBJECT = 30352;

	/**
	 * All supported boss locations.
	 * Add new locations to this set as they are implemented.
	 */
	public static final Set<BossLocation> ALL_LOCATIONS;

	/**
	 * Map of location ID to the game object ID that requires validation in that location.
	 */
	public static final Map<String, Integer> LOCATION_OBJECTS;

	static
	{
		Set<BossLocation> locations = new HashSet<>();
		locations.add(NEX);
		locations.add(THEATRE_OF_BLOOD);
		locations.add(TOMBS_OF_AMASCUT);
		locations.add(CHAMBERS_OF_XERIC);
		locations.add(INFERNO);
		
		ALL_LOCATIONS = Collections.unmodifiableSet(locations);

		// Map locations to their game objects
		Map<String, Integer> objectMap = new HashMap<>();
		objectMap.put(NEX.getId(), NEX_OBJECT);
		objectMap.put(THEATRE_OF_BLOOD.getId(), TOB_OBJECT);
		objectMap.put(TOMBS_OF_AMASCUT.getId(), TOA_OBJECT);
		objectMap.put(CHAMBERS_OF_XERIC.getId(), COX_OBJECT);
		objectMap.put(INFERNO.getId(), INFERNO_OBJECT);
		
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
	 * Get the game object ID that requires validation for a location
	 */
	public static Integer getObjectForLocation(BossLocation location)
	{
		if (location == null)
		{
			return null;
		}
		return LOCATION_OBJECTS.get(location.getId());
	}

	/**
	 * Check if an object ID requires validation in the given location
	 */
	public static boolean isValidatedObject(BossLocation location, int objectId)
	{
		Integer validatedObjectId = getObjectForLocation(location);
		return validatedObjectId != null && validatedObjectId == objectId;
	}
}
