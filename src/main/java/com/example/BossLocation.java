package com.example;

import lombok.Value;
import java.util.Set;

/**
 * Represents a boss location/region with validation requirements.
 * The region defines the location, and we validate when players interact with
 * specific objects within that region.
 */
@Value
public class BossLocation
{
	/**
	 * Unique identifier for this location (e.g., "nex", "colosseum")
	 */
	String id;

	/**
	 * Display name for this location
	 */
	String displayName;

	/**
	 * Set of region IDs that define this location
	 */
	Set<Integer> regionIds;

	/**
	 * Check if a region ID belongs to this location
	 */
	public boolean isInRegion(int regionId)
	{
		return regionIds.contains(regionId);
	}

	/**
	 * Check if any of the given regions match this location
	 */
	public boolean isInAnyRegion(int[] regions)
	{
		if (regions == null)
		{
			return false;
		}
		
		for (int region : regions)
		{
			if (isInRegion(region))
			{
				return true;
			}
		}
		return false;
	}
}
