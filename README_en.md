# Nick - Nickname Mod

[中文](README.md) | **English** | [混合 mix](README_mix.md)

[![Modrinth](https://img.shields.io/modrinth/dt/VtG7yP1S?label=Modrinth%20Downloads)](https://modrinth.com/mod/nick-mod)

> This mod is AI-generated (DeepSeek V4 via opencode)

A Fabric server-side mod that allows players to change their display name (nickname), supports team colors, prefixes/suffixes, and allows player lookup by nickname.

## Commands

> `/nick` requires the Carpet rule `commandNick` to be enabled (off by default):
> `/carpet commandNick true`

### Set Nickname

| Command                           | Permission | Description                     |
| --------------------------------- | ---------- | ------------------------------- |
| `/nick set <nickname>`          | Everyone   | Set your own nickname           |
| `/nick set <nickname> <target>` | OP         | Set another player's nickname   |
| `/nick reset`                   | Everyone   | Reset your own nickname         |
| `/nick reset <target>`          | OP         | Reset another player's nickname |

`<target>` supports player names, `@p`, `@a`, `@r`, `@s` and other selectors, as well as nicknames.

### Nickname Rules

- At most 32 characters
- Must not match another **online** player's game name (prevents impersonation that would make commands target the wrong player)
- Must not duplicate a nickname already used by another player
- Must not contain `§` (colour/format codes) or control characters

Violations are reported back instead of being written to the config.

### Nickname Lookup

Once a nickname is set, all commands can use the nickname in place of the player name, e.g.:

- `/tp Xiaoming`
- `/msg Xiaoming`
- `/kick Xiaoming`

**Note**: If a nickname and a real player name are identical, the real name takes priority.

### Nickname Format

- Single word: `/nick set Xiaoming`
- Nickname with spaces, use quotes: `/nick set "Xiao Ming"`
- Displays team color, prefix, and suffix when applicable

## Features

- ✅ Chat messages display nickname
- ✅ Tab list displays nickname (all clients, no mod required)
- ✅ Name tag displays nickname (singleplayer) / requires client mod (server mode)
- ✅ Per-location client display modes: nickname only, nickname+original, hide
- ✅ Team colors apply
- ✅ Lookup players by nickname (`/tp`, `/msg`, `/kick`, `@e[name=nickname]`, tab-completion)

## Installation

### Server

1. Place `nick-*.jar` into the `mods/` directory (Fabric API and Carpet required)
2. Restart the server

### Client (Optional)

When installed on the client, name tags display nicknames in server mode, and the client configuration screen becomes available. Without the client mod, chat and tab list still show nicknames.

The client config screen needs [Cloth Config](https://modrinth.com/mod/cloth-config). Without it there is simply no GUI; the mod itself keeps working.

## Client Configuration

When the client mod is installed, `config/nick-client.json` is auto-generated. It supports a global default mode plus per-location overrides for nametag, chat and tab list.

| Mode                    | Effect                           | Example              |
| ----------------------- | -------------------------------- | -------------------- |
| `"nick_only"`         | Show nickname only (default)     | `Xiao Ming`        |
| `"nick_and_original"` | Nickname + original name         | `[Xiao Ming]zxdnb` |
| `"hide"`              | Hide nickname, show original name | `zxdnb`            |

## Server Config

`config/nick.json`:

```json
{
  "zxdnb": {"nick": "Xiao Ming"},
  "dongchengqiao": {"nick": "Dong Chengqiao"}
}
```

Keys are game names (not nicknames). The file is written to a temp file first and then moved into place atomically, so an interrupted write cannot corrupt it.

## Technical Info

- Minecraft version: 26.3
- Framework: Fabric Loader 0.19.5 / Fabric API 0.161.0+26.3
- Maven group: `com.dongchengqiao.nick`
- Main class: `com.dongchengqiao.nick.Nick`
