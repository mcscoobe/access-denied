package com.osrs.accessdenied;

import lombok.Value;

import java.util.Map;
import java.util.Set;

/**
 * Represents the result of validation checks for area entry requirements.
 * This immutable data model indicates whether a player meets the requirements
 * and provides feedback about what's missing.
 * 
 * Validates Requirements: 3.1, 3.3
 */
/**
 * Represents the result of validation checks for area entry requirements.
 * This immutable data model indicates whether a player meets the requirements
 * and provides feedback about what's missing.
 *
 * Validates Requirements: 3.1, 3.3
 */
@Value
public class ValidationResult {
    /**
     * Whether the player meets all requirements for entry.
     */
    boolean valid;

    /**
     * Set of missing requirements (e.g., "Blood rune x5", "Death rune x10").
     * Empty when valid is true.
     */
    Set<String> missingRequirements;

    /**
     * Human-readable feedback message describing the validation result.
     * Contains details about missing requirements when valid is false.
     */
    String feedbackMessage;

    /**
     * Map of missing rune IDs to required quantities.
     * Empty when valid is true.
     * Used by FeedbackManager to display detailed missing runes messages.
     */
    Map<Integer, Integer> missingRunesMap;
}

