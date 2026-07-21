# TNT-Tag

TNT-Tag is a Paper minigame in which players must avoid the tagger, pass the TNT before it explodes, and survive the final round.

## Requirements

- Paper 26.2 or newer
- Java 25
- Maven 3.9+ to build from source

Optional integrations are available for PlaceholderAPI and TAB.

## Building

```bash
mvn clean verify
```

The shaded plugin JAR is written to `target/TNT-Tag.jar`. The repository workflows also verify pushes and pull requests with Java 25; deployment to a server remains manual.

The Paper API uses the recommended Maven version range beginning at 26.2, so each intentional clean build resolves the newest Paper API matching that range.

## Architecture

- Commands use Paper's native lifecycle-aware Brigadier command API.
- Menus use Paper/Bukkit inventories with a plugin-owned `InventoryHolder` and native inventory events.
- BoostedYAML remains the configuration backend for this phase.
- Gson is shaded for diagnostic-service JSON responses.
- PlaceholderAPI and TAB are optional, provided integrations and are not bundled.

## Configuration formatting

Fresh configuration files use [MiniMessage](https://docs.advntr.dev/minimessage/format.html) formatting. Existing configurations that use legacy ampersand color codes remain supported for backward compatibility.

## Installation

1. Install Paper 26.2 or newer with Java 25.
2. Copy `TNT-Tag.jar` into the server's `plugins` directory.
3. Start the server and configure the lobby and arenas.

See [MIGRATION.md](MIGRATION.md) before upgrading an existing installation.

## License

This project retains its existing Attribution-NonCommercial-NoDerivatives 4.0 International license. Review [LICENSE.md](LICENSE.md) before using or distributing the source.
