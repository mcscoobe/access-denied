package com.osrs.accessdenied;

import lombok.Value;

import java.util.Set;

/**
 * Represents the result of validation checks for area entry requirements.
 * This immutable data model indicates whether a player meets the requirements
 * and provides feedback about what's missing.
 */
@Value
public class ValidationResult
{
	/**
	 * Whether the player meets all requirements for entry.
	 */
	boolean valid;

	/**
	 * Set of missing requirements (e.g., "runes for Resurrect Greater Ghost", "Book of the Dead").
	 * Empty when valid is true.
	 */
	Set<String> missingRequirements;

	/**
	 * Human-readable feedback message describing the validation result.
	 * Contains details about missing requirements when valid is false.
	 */
	String feedbackMessage;
}

