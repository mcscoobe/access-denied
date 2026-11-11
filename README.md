# Access Denied

A RuneLite plugin that prevents accidental boss entries by validating your spellbook and rune requirements before you enter.

## Supported Locations

- **Nex** — Arceuus spellbook, thralls, death charge
- **The Fractured Archive** — Arceuus spellbook, thralls, death charge
- **Theatre of Blood** — Arceuus spellbook, thralls, death charge
- **Tombs of Amascut** — Arceuus spellbook, thralls, death charge
- **Chambers of Xeric** — Arceuus spellbook (thralls, death charge) or Lunar spellbook (humidify, vengeance)
- **Inferno** — Ancient spellbook, ice barrage, blood barrage

## Setup

1. Enable the plugin in RuneLite
2. Open plugin configuration and select a location
3. Enable at least one requirement (**Enable Validation** is on by default, but validates nothing until a requirement is set)

If requirements aren't met on entry, the default click becomes **Walk Here** and a chat message shows what's missing.

## CoX Reload Lock

While scouting Chambers of Xeric, the plugin can stop you from reloading a raid you want to keep.
Turn on **Enable Scouting Protection** and set at least one of the required rooms, disallowed
rooms or allowed layouts filters.

- When a scouted raid passes every filter you set, the lock arms and the reload object's
  options are removed.
- Right-click the reload object and choose **Release raid lock** to unlock it. The lock stays
  released for that layout until the raid resets.
- The lock relies on RuneLite's built-in **Chambers of Xeric** plugin to scout the raid, so that
  plugin must be enabled. The lock doesn't arm in challenge mode.

## Side Panel

The plugin adds a panel to the RuneLite sidebar for the settings you change mid-session.

- **NPCs visible / NPCs hidden** — a toggle that hides every NPC, along with its health bar,
  overhead prayers, hitsplats and name. Useful for seeing through a crowded room in Chambers
  of Xeric. The button reads out its current state, and the setting persists across restarts
  and is stored per profile.

Hiding works the same way the core Entity Hider plugin's does, so the two can run together.
An NPC stays hidden if either plugin hides it.
