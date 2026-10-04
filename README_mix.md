# Nick - 昵称模组 / Nickname Mod

[中文](README.md) | [English](README_en.md) | **混合 mix**

[![Modrinth](https://img.shields.io/modrinth/dt/VtG7yP1S?label=Modrinth%20Downloads)](https://modrinth.com/mod/nick-mod)

> 此模组由AI生成(opencode内的DeepSeek V4) / AI-generated (DeepSeek V4 via opencode)

一个 Fabric 服务端模组，允许玩家修改自己的显示名称（昵称），支持团队颜色、前缀/后缀，并可通过昵称查找玩家。

A Fabric server-side mod that allows players to change their display name (nickname), supports team colors, prefixes/suffixes, and allows player lookup by nickname.

---

## 命令 / Commands

> `/nick` 需要开启 Carpet 规则 `commandNick`（默认关闭）：`/carpet commandNick true`
> `/nick` requires the Carpet rule `commandNick` (off by default): `/carpet commandNick true`

### 设置昵称 / Set Nickname

| 命令 / Command                              | 权限 / Permission | 说明 / Description                             |
| ------------------------------------------- | ----------------- | ---------------------------------------------- |
| `/nick set <昵称/nickname>`               | 所有人 Everyone   | 设置自己的昵称 Set your own nickname           |
| `/nick set <昵称/nickname> <目标/target>` | OP                | 设置他人的昵称 Set another player's nickname   |
| `/nick reset`                             | 所有人 Everyone   | 重置自己的昵称 Reset your own nickname         |
| `/nick reset <目标/target>`               | OP                | 重置他人的昵称 Reset another player's nickname |

`<目标/target>` 支持玩家名、`@p`、`@a`、`@r`、`@s` 等选择器，也支持昵称。Supports player names, selectors and nicknames.

### 昵称规则 / Nickname Rules

- 最多 32 个字符 At most 32 characters
- 不能与其他在线玩家的游戏名相同 Must not match another online player's game name
- 不能与其他玩家已用的昵称重复 Must not duplicate another player's nickname
- 不能包含 `§` 或控制字符 Must not contain `§` or control characters

---

## 效果 / Features

| 功能 Feature                                                                                                                       | 状态 Status |
| ---------------------------------------------------------------------------------------------------------------------------------- | ----------- |
| 聊天消息显示昵称 Chat messages display nickname                                                                                    | ✅          |
| Tab 列表显示昵称（所有客户端，无需模组）Tab list displays nickname (all clients, no mod required)                                  | ✅          |
| 头顶名签显示昵称（单人模式/需客户端装模组）Name tag shows nickname (singleplayer / requires client mod on server)                  | ✅          |
| 每位置客户端显示模式（仅昵称/昵称+原名/隐藏）Per-location client display modes (nickname only/nickname+original/hide)                | ✅          |
| 团队颜色生效 Team colors apply                                                                                                     | ✅          |
| 昵称查找玩家 Lookup players by nickname                                                                                            | ✅          |

---

## 安装 / Installation

### 服务端 / Server

1. 将 `nick-*.jar` 放入 `mods/` 目录（需要 Fabric API 和 Carpet）Place `nick-*.jar` into the `mods/` directory (Fabric API and Carpet required)
2. 重启服务器 Restart the server

### 客户端（可选）/ Client (Optional)

客户端安装后，头顶名签可在服务器模式下显示昵称，并可通过客户端配置调整每位置的显示方式。不装则仅聊天和 Tab 列表生效。

When installed on the client, name tags display nicknames in server mode, and the per-location client configuration screen is available. Without the mod, chat and tab list still work.

客户端配置界面需要 [Cloth Config](https://modrinth.com/mod/cloth-config)，不装则只是没有图形界面。The config screen needs Cloth Config; without it the mod still works, just without a GUI.

---

## 客户端配置 / Client Configuration

自动生成 `config/nick-client.json`，支持全局默认模式 + 每位置覆盖（头顶 / 聊天 / Tab 列表）。Auto-generated, global default + per-location overrides (nametag / chat / tab list).

| 值 Value                | 效果 Effect                                     | 示例 Example         |
| ----------------------- | ----------------------------------------------- | -------------------- |
| `"nick_only"`         | 仅显示昵称（默认）Show nickname only            | `Xiao Ming`        |
| `"nick_and_original"` | 昵称+原名 Nickname + original                   | `[Xiao Ming]zxdnb` |
| `"hide"`              | 隐藏昵称，显示原名 Hide nickname, show original | `zxdnb`            |

---

## 配置文件 / Server Config

`config/nick.json`:

```json
{
  "zxdnb": {"nick": "脏小豆"},
  "dongchengqiao": {"nick": "董丞乔"}
}
```

键是游戏名（不是昵称）。写入采用临时文件 + 原子替换，避免写坏配置。
Keys are game names (not nicknames); the file is replaced atomically to avoid corruption.

---

## Tests

```
./gradlew test
```

Also runs as part of `./gradlew build`, and in CI.

| Test | Covers |
| --- | --- |
| `CommandTreeCompatibilityTest` | the command tree must not contain an argument type a client without the mod cannot resolve |
| `NickDisplayTextTest` | the three display modes, and stripping team decoration |
| `NickNamesTest` | nickname length, character and formatting-code rules |
| `UnicodeStringsTest` | unquoted CJK and quoted argument parsing |

`CommandTreeCompatibilityTest` is the important one. The server pushes its whole command tree to
every client and argument types travel as numeric registry ids; a mod-provided type is unknown to a
client without the mod, the node degrades into a `RootCommandNode` that its parent silently skips,
and the whole `/nick set` branch vanishes from that client's tree - no error, no disconnect, just
missing completion and syntax hints. Nobody spots that by reading code, so it has a test, and that
test carries a negative control to prove it can actually fail.

---

## 技术信息 / Technical Info

- Minecraft 版本 Version: 26.3
- 框架 Framework: Fabric Loader 0.19.5 / Fabric API 0.161.0+26.3
- Maven 组 Group: `com.dongchengqiao.nick`
- 主类 Main class: `com.dongchengqiao.nick.Nick`
