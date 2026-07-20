# TNT-Tag

TNT-Tag is a Paper minigame in which players must avoid the tagger, pass the TNT before it explodes, and survive the final round.

## Requirements

- Paper 26.2
- Java 25
- Maven 3.9+ to build from source

Optional integrations are available for PlaceholderAPI, TAB, Parties, and Party and Friends.

## Building

```bash
mvn clean verify
```

The shaded plugin JAR is written to `target/TNT-Tag.jar`. GitHub Actions also builds and uploads this artifact on pushes and pull requests to `main`.

## Configuration formatting

Fresh configuration files use [MiniMessage](https://docs.advntr.dev/minimessage/format.html) formatting. Existing configurations that use legacy ampersand color codes remain supported for backward compatibility.

## Installation

1. Install Paper 26.2 with Java 25.
2. Copy `TNT-Tag.jar` into the server's `plugins` directory.
3. Start the server and configure the lobby and arenas.

See [MIGRATION.md](MIGRATION.md) before upgrading an existing installation.

## License

This project retains its existing Attribution-NonCommercial-NoDerivatives 4.0 International license. Review [LICENSE.md](LICENSE.md) before using or distributing the source.
