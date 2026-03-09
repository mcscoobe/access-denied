package com.osrs.accessdenied;

import org.junit.Test;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

public class ValidationResultTest
{
	@Test
	public void testValidResult()
	{
		ValidationResult result = new ValidationResult(
			true,
			Collections.emptySet(),
			"All requirements met"
		);

		assertTrue(result.isValid());
		assertTrue(result.getMissingRequirements().isEmpty());
		assertEquals("All requirements met", result.getFeedbackMessage());
	}

	@Test
	public void testInvalidResultWithMissingRequirements()
	{
		Set<String> missing = new HashSet<>();
		missing.add("Soul rune x4");
		missing.add("Blood rune x2");

		ValidationResult result = new ValidationResult(
			false,
			missing,
			"Missing: Soul rune x4, Blood rune x2"
		);

		assertFalse(result.isValid());
		assertEquals(2, result.getMissingRequirements().size());
		assertTrue(result.getMissingRequirements().contains("Soul rune x4"));
		assertTrue(result.getMissingRequirements().contains("Blood rune x2"));
		assertEquals("Missing: Soul rune x4, Blood rune x2", result.getFeedbackMessage());
	}

	@Test
	public void testInvalidResultWithSingleMissingRequirement()
	{
		ValidationResult result = new ValidationResult(
			false,
			Collections.singleton("Arceuus spellbook"),
			"Missing: Arceuus spellbook"
		);

		assertFalse(result.isValid());
		assertEquals(1, result.getMissingRequirements().size());
		assertTrue(result.getMissingRequirements().contains("Arceuus spellbook"));
		assertEquals("Missing: Arceuus spellbook", result.getFeedbackMessage());
	}
}
