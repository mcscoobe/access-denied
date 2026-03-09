# Access Denied

A RuneLite plugin that validates player requirements before entering boss areas, helping prevent accidental entries without proper spell setup.

## Features

- Validates spell requirements for major PvM content:
  - Nex
  - Theatre of Blood (ToB)
  - Tombs of Amascut (ToA)
  - Chambers of Xeric (CoX)

- Configurable requirements per location:
  - Resurrect Greater Ghost spell (thralls)
  - Death Charge spell
  - Arceuus spellbook validation
  - Rune pouch and inventory checking

- Smart validation:
  - Checks inventory and rune pouch contents
  - Supports Aether runes as substitutes for Soul/Cosmic runes
  - Real-time validation on item/equipment changes
  - Clear feedback messages when requirements aren't met
  - 30-second validation cache to reduce redundant checks

## Configuration

The plugin provides granular control over validation for each boss location. Access the configuration through RuneLite's plugin panel.

### Per-Location Settings

Each boss location (Nex, ToB, ToA, CoX) has three configuration options:

1. **Enable Validation** (Master Toggle)
   - Default: Enabled
   - Controls whether any validation occurs for this location
   - When disabled, you can enter freely without checks

2. **Require Resurrect Greater Ghost**
   - Default: Disabled
   - Validates you have the required runes and Book of the Dead
   - Required runes: 4 Soul, 2 Blood, 1 Cosmic (per cast)
   - Aether runes can substitute for Soul or Cosmic runes

3. **Require Death Charge**
   - Default: Disabled
   - Validates you have the required runes for Death Charge
   - Required runes: 1 Death, 1 Blood, 1 Soul (per cast)
   - Aether runes can substitute for Soul runes

### Configuration Examples

**Example 1: Nex with Thralls Only**
- Enable Validation: ✓
- Require Resurrect Greater Ghost: ✓
- Require Death Charge: ✗

**Example 2: ToB with Both Spells**
- Enable Validation: ✓
- Require Resurrect Greater Ghost: ✓
- Require Death Charge: ✓

**Example 3: Disable All Validation for CoX**
- Enable Validation: ✗
- (Other settings ignored when master toggle is off)

### Important Notes

- At least one requirement must be enabled for validation to occur
- The plugin checks both inventory and rune pouch for runes
- Validation updates automatically when you change equipment or inventory
- Spellbook must be set to Arceuus for all requirements

## How It Works

When you hover over a boss entrance with validation enabled, the plugin:
1. Checks if you're on the Arceuus spellbook
2. Verifies you have the required runes (including rune pouch)
3. Confirms Book of the Dead is in inventory (for thralls)
4. Prevents accidental entry by changing the default click action if requirements aren't met

## Contributing

Ideas and suggestions are welcome! If you have:
- Boss locations you'd like to see added
- Additional item or rune configurations
- Feature requests or improvements

Feel free to open an issue or submit a pull request.

## Development

Build: `gradlew.bat build`
Test: `gradlew.bat test`
Run test client: `gradlew.bat runTestClient`