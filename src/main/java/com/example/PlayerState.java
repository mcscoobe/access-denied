package com.example;

import lombok.Value;

import java.util.Map;

/**
 * Immutable data model representing the current player state for validation purposes.
 * This class captures the player's rune pouch contents and is designed to be extensible
 * for future validation types such as spellbook and inventory checks.
 * 
 * Validates Requirements: 4.1, 4.2
 */
@Value
public class PlayerState {
    /**
     * Map of rune pouch contents where key is the rune item ID and value is the quantity.
     * Used to validate against area requirements to ensure players have sufficient runes.
     */
    Map<Integer, Integer> runePouchContents;
    
    // Future extensibility: Spellbook validation
    // When implementing spellbook validation, uncomment and implement:
    // /**
    //  * The player's currently active spellbook.
    //  * Used to validate against area requirements for spellbook-specific content.
    //  */
    // Spellbook currentSpellbook;
    
    // Future extensibility: Inventory validation
    // When implementing inventory validation, uncomment and implement:
    // /**
    //  * Set of item IDs currently in the player's inventory.
    //  * Used to validate against area requirements for required items.
    //  */
    // Set<Integer> inventoryItems;
}
