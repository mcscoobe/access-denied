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

## Configuration

Each boss location has independent configuration options:
- Enable/disable Resurrect Greater Ghost requirement
- Enable/disable Death Charge requirement

## How It Works

When you hover over a boss entrance with validation enabled, the plugin:
1. Checks if you're on the Arceuus spellbook
2. Verifies you have the required runes (including rune pouch)
3. Confirms Book of the Dead is in inventory (for thralls)
4. Prevents accidental entry by changing the default click action if requirements aren't met

## Development

Build: `gradlew.bat build`
Test: `gradlew.bat test`
Run test client: `gradlew.bat runTestClient`