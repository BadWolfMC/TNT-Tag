# TNT-Tag 10 migration notes

## Platform requirements

TNT-Tag 10 targets **Paper 26.2** and **Java 25**. It is no longer intended to load on Spigot 1.17 or older Minecraft server versions.

## Before upgrading

1. Stop the server.
2. Back up the entire `plugins/TNT-Tag` directory.
3. Replace the old plugin JAR.
4. Start the server and review the console for invalid arena or registry-value warnings.

## Configuration compatibility

Fresh defaults now use MiniMessage formatting such as `<red>` and `<bold>`. Existing `&c`/`&l` formatting is still accepted, so current customization files do not need to be regenerated merely for this upgrade.

Materials, sounds, and potion effects are resolved through Paper's registries. Existing enum-style values such as `DIAMOND_AXE`, `ENTITY_EXPERIENCE_ORB_PICKUP`, and `SPEED` remain accepted. Namespaced values such as `minecraft:diamond_axe` are also supported.

## Arena capacity validation

Arenas now require:

- `minPlayers` of at least 2
- `maxPlayers` greater than `minPlayers`

Invalid saved arenas are skipped with a console error instead of being loaded into a broken state. The setup and editor flows prevent creating the same invalid combinations.

All entry paths—direct join, random join, forced join, Party and Friends, and Parties—now share the same capacity calculation. The join counter reports active arena participants rather than unrelated lobby occupancy.

## API and display changes

Messages, titles, action bars, signs, item names/lore, player display names, and scoreboard titles use Adventure components. TAB remains a String-based bridge because its API expects formatted strings.

The former XSeries material parser was removed; the Paper registries are now the source of truth.
