# TNT-Tag 10.0.1 modernization notes

This branch targets Paper 26.2+ and Java 25. Back up the TNT-Tag data directory before replacing an existing JAR.

## Build and runtime

- The Paper API dependency uses `[26.2.build,)` with `provided` scope.
- LAMP and RyseInventory have been removed.
- Parties and PartyAndFriends support has been removed because those integrations are not used by BadWolfMC.
- BoostedYAML and Gson remain shaded into the plugin.
- PlaceholderAPI and TAB remain optional `provided` integrations.
- `plugin.yml` remains the plugin descriptor; no `paper-plugin.yml` migration is required.

## Commands

Commands are registered as a native Paper Brigadier tree during `LifecycleEvents.COMMANDS`. The `/tnttag` root and `/tt` alias remain available.

Existing permissions are retained:

- `tnttag.help`
- `tnttag.join`
- `tnttag.gui.join`
- `tnttag.list`
- `tnttag.info`
- `tnttag.stats`
- `tnttag.top`
- `tnttag.create`
- `tnttag.delete`
- `tnttag.editor`
- `tnttag.reload`
- `tnttag.setlobby`
- `tnttag.start`
- `tnttag.forcejoin`
- `tnttag.forceleave`
- `tnttag.dump.all`
- `tnttag.dump.log`
- `tnttag.bypass-forcejoin`
- `tnttag.bypass-forceleave`
- `tnttag.createsigns`
- `tnttag.breaksigns`
- `tnttag.update`

Permissions now also control whether Brigadier exposes each command branch to the sender. Arena arguments provide live arena-name suggestions, quoted arena names are supported, and the `forced` start argument is a native boolean.

A normal administrative start requires the arena's configured minimum player count. A forced start may bypass that configured minimum but still requires at least two active players, because a TNT-Tag round cannot operate correctly with fewer participants.

## Menus and inventory safety

The arena selector, editor, statistics, and leaderboard screens now use a small native menu layer built on `InventoryHolder` and Paper/Bukkit inventory events.

- Menu clicks and drags are cancelled as native inventory transactions before a menu callback runs.
- Shift-clicking, number-key swaps, double-click collection, and lower-inventory transfers cannot move menu items.
- During an active TNT-Tag lobby/game session, player inventory clicks and drags are also protected.
- Configured TNT-Tag items are tagged with a plugin `PersistentDataContainer` key. Cleanup removes only those tagged items instead of guessing by material or display name.
- Join, leave, radar, and TNT items remain usable through normal interaction even though they cannot be moved.
- Failed or invalid arena joins are rejected before TNT-Tag snapshots or clears the player inventory.

Menu behavior is separated from named `MenuLayout` slots. The layouts are currently code defaults, but this boundary is intended to allow a later configuration-backed layout system without replacing the menu implementation again.

## Player-state restoration

TNT-Tag now keeps one complete snapshot when it first takes control of a player and restores that snapshot exactly once when the session ends. The snapshot includes:

- Storage, armor, offhand, and cursor items
- Experience and levels
- Game mode, food, and saturation
- Potion effects
- Flight, collision, and invisibility state
- Adventure display/list names
- Scoreboard and native Adventure display/list names
- Original location, unless `skip-location-restoral` is enabled

Normal leave, quit, plugin disable, arena end, and administrative removal paths all converge on this session restoration. TNT-Tag removes its temporary TAB API prefix override and unhides the nametag so TAB can resume its configured dynamic formatting. Marked leftovers from an interrupted older session are cleaned on startup/join without deleting unrelated player items.

## Arena lifecycle

- Arena capacity must use `minPlayers >= 2` and `maxPlayers > minPlayers`.
- All join paths use one centralized capacity check.
- Arenas with active players or game state cannot be deleted, reloaded, or changed through a stale editor window.
- Scoreboard, countdown, round, ending, and delayed command tasks are cancelled when an arena is unloaded.
- The arena selector is paginated and no longer has a 36-arena display limit.

## Data and diagnostics

- Player-stat mutations are coalesced and saved periodically instead of rewriting `playerdata.yml` on every statistic change. Dirty data is flushed during plugin shutdown.
- Diagnostic file reads and HTTP uploads run asynchronously; Bukkit-facing data is captured on the primary thread and result messages return to it.
- Bungee-mode auto-join waits for delayed arena/lobby initialization, and leave routing restores state correctly whether the global lobby is enabled or skipped.
- The PlaceholderAPI expansion safely handles malformed rankings, supports `top_winstreak_<position>`, resolves arena names containing underscores, and provides offline statistics.

## Configuration compatibility

Fresh defaults use MiniMessage. Existing ampersand-colored messages continue to parse. Existing BoostedYAML files are retained; a migration to Paper/Bukkit `YamlConfiguration` is intentionally deferred to a later phase.
