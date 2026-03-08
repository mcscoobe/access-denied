package com.example;

import lombok.Value;

import java.util.Map;
import java.util.Set;

/**
 * Immutable data model representing requirements for entering a specific area.
 * This class stores configuration for area-specific validation including required runes
 * and game object IDs. The architecture is designed to be extensible for future
 * validation types such as spellbook and inventory checks.
 * 
 * Validates Requirements: 4.1, 4.2
 */
@Value
public class AreaRequirements {
    /**
     * Map of required runes where key is the rune item ID and value is the minimum quantity.
     * Used for rune pouch validation to ensure players have sufficient runes before entry.
     */
    Map<Integer, Integer> requiredRunes;
    
    /**
     * Set of game object IDs that represent entrances to this area.
     * Used to identify which game objects should trigger validation checks.
     */
    Set<Integer> gameObjectIds;
    
    // Future extensibility: Spellbook validation
    // When implementing spellbook validation, uncomment and implement:
    // /**
    //  * Required spellbook for entering this area.
    //  * Null if no specific spellbook is required.
    //  */
    // Spellbook requiredSpellbook;
    
    // Future extensibility: Inventory validation
    // When implementing inventory validation, uncomment and implement:
    // /**
    //  * Set of required inventory item IDs that must be present.
    //  * Used to validate player has necessary items before entry.
    //  */
    // Set<Integer> requiredItems;
}
