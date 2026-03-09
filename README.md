# Access Denied

A RuneLite plugin that prevents accidental boss entries by validating spell and gear requirements before you enter.

## What It Does

Checks if you have the required runes, spellbook, and gear before entering boss areas, based off your plugin configuration. If requirements aren't met, it changes the default click action to "Walk here" and shows you what's missing.

## Supported Locations

- **Nex** - Arceuus spellbook, thralls, death charge
- **Theatre of Blood** - Arceuus spellbook, thralls, death charge
- **Tombs of Amascut** - Arceuus spellbook, thralls, death charge
- **Chambers of Xeric** - Arceuus spellbook, thralls, death charge
- **Inferno** - Ancient spellbook, Ice Barrage, Blood Barrage

## Quick Start

1. Enable the plugin in RuneLite
2. Open plugin configuration
3. Choose a location (e.g., "Nex")
4. Enable "Enable Validation"
5. Enable at least one requirement (e.g., "Require Resurrect Greater Ghost")

That's it! The plugin will now check your requirements when you try to enter.

## Key Features

- **Smart rune detection** - Checks inventory and rune pouch
- **Aether rune support** - Automatically substitutes for Soul/Cosmic runes
- **Kodai wand support** - Infinite water runes for Ice Barrage
- **Real-time updates** - Validates when you change items or equipment
- **Clear feedback** - Shows exactly what you're missing

## Configuration

Each location has:
- **Enable Validation** - Master toggle
- **Spell requirements** - Choose which spells to check

At least one spell requirement must be enabled for validation to work.

### Example: Nex with Thralls

1. Go to "Nex" section in config
2. Enable "Enable Validation" ✓
3. Enable "Require Resurrect Greater Ghost" ✓
4. Done! Plugin will check for thrall runes and Book of the Dead

## Spell Requirements

### Arceuus Spellbook (Nex, ToB, ToA, CoX)

**Resurrect Greater Ghost:**
- 4 Soul, 2 Blood, 1 Cosmic runes
- Book of the Dead
- Aether runes work for Soul/Cosmic

**Death Charge:**
- 1 Death, 1 Blood, 1 Soul runes
- Aether runes work for Soul

### Ancient Spellbook (Inferno)

**Ice Barrage:**
- 6 Water, 2 Death, 4 Blood runes
- Kodai wand provides infinite water runes

**Blood Barrage:**
- 4 Blood, 1 Soul, 1 Death runes
- Aether runes work for Soul

## Development

```bash
gradlew.bat build           # Build plugin
gradlew.bat test            # Run tests
gradlew.bat runTestClient   # Test in-game
```

## Contributing

Suggestions welcome! Open an issue or PR for:
- New boss locations
- Additional spell requirements
- Feature improvements

## Documentation

Detailed documentation available in the repository:
- `INFERNO_IMPLEMENTATION.md` - Inferno validation details
- `RUNE_REFERENCE.md` - Complete rune ID reference
- `KODAI_WAND_SUPPORT.md` - Kodai wand implementation
- `TESTING_STRATEGY.md` - Testing guide
