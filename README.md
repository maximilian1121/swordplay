# Sword Fight

A Minecraft PvP minigame server built on [Minestom](https://github.com/minestom/minestom). Players are split into teams and randomly paired for 1v1 duels on a floating platform. Knock your opponent off the edge to score — first to 3 wins!

## Features

- Team-based PvP (Red vs Blue)
- Random 1v1 matchmaking with best-of-five rounds
- Shield and charged attack mechanics
- Spectator mode with automatic player rotation
- Custom resource pack
- Boss bars, fireworks, and podium celebrations
- Authentication: Online, Offline, Velocity, and BungeeCord support

## Prerequisites

- Java 17 or newer
- Gradle 9.3+ (included via wrapper)

## Getting the World

This server was specifically built around the **Wuhu Island** map by [danicraft203](https://www.planetminecraft.com/member/danicraft203/). While any world could technically work, Wuhu Island is strongly recommended as it is the map this server was designed and tested on.

1. Download the map from [PlanetMinecraft](https://www.planetminecraft.com/member/danicraft203/)
2. Open the map in **Minecraft 26.2** and run it once to update the world format
3. Place the resulting `wuhu-island` folder in the server's working directory (the `run/` folder)

> **Note:** The server will refuse to start if `wuhu-island` is not found in the working directory.

## Building

```bash
./gradlew build
```

This produces a fat JAR at `build/libs/sword-fight-1.0-SNAPSHOT.jar`.

## Running

```bash
java -jar build/libs/sword-fight-1.0-SNAPSHOT.jar [OPTIONS]
```

### Options

| Flag | Description | Default |
|------|-------------|---------|
| `-p`, `--port` | Port to bind to | `25565` |
| `-i`, `--ip` | IP address to bind to | `0.0.0.0` |
| `-a`, `--auth` | Auth mode: `ONLINE`, `OFFLINE`, `VELOCITY`, `BUNGEE` | `ONLINE` |
| `-k`, `--key` | Velocity security key or BungeeCord token (required for VELOCITY/BUNGEE) | `no` |

### Example

```bash
java -jar build/libs/sword-fight-1.0-SNAPSHOT.jar -p 25565 -a ONLINE
```

## License

This project does not currently have a license.
