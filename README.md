# Nick - 昵称模组

**中文** | [English](README_en.md) | [混合 mix](README_mix.md)

[![Modrinth](https://img.shields.io/modrinth/dt/VtG7yP1S?label=Modrinth%20Downloads)](https://modrinth.com/mod/nick-mod)

此模组由AI生成(opencode内的DeepSeek V4)

一个 Fabric 服务端模组，允许玩家修改自己的显示名称（昵称），支持团队颜色、前缀/后缀，并可通过昵称查找玩家。

## 命令

> `/nick` 需要服务端开启 Carpet 规则 `commandNick`（默认关闭）：
> `/carpet commandNick true`

### 设置昵称

| 命令                        | 权限   | 说明           |
| --------------------------- | ------ | -------------- |
| `/nick set <昵称>`        | 所有人 | 设置自己的昵称 |
| `/nick set <昵称> <目标>` | OP     | 设置他人的昵称 |
| `/nick reset`             | 所有人 | 重置自己的昵称 |
| `/nick reset <目标>`      | OP     | 重置他人的昵称 |

`<目标>` 支持玩家名、`@p`、`@a`、`@r`、`@s` 等选择器，也支持昵称。

### 昵称规则

- 长度上限 32 个字符
- 不能与**其他在线玩家**的游戏名相同（避免冒名顶替导致命令指错人）
- 不能与**其他玩家已用**的昵称重复
- 不能包含 `§`（颜色/格式代码）或控制字符

不满足时会返回提示，不会写入配置。

### 昵称查找

设置昵称后，所有命令皆可用昵称替代玩家名，例如：

- `/tp 小明`
- `/msg 小明`
- `/kick 小明`

**注意**：若昵称与真实玩家名同时存在，玩家名优先匹配。

### 昵称格式

- 单个词直接写：`/nick set 小明`
- 带空格的昵称用引号：`/nick set "小明 同学"`
- 会显示队伍的团队颜色、前缀和后缀

## 效果

- ✅ 聊天消息显示昵称
- ✅ Tab 列表显示昵称（所有客户端，无需安装模组）
- ✅ 头顶名签显示昵称（单人模式）/ 需客户端装模组（服务器模式）
- ✅ 支持客户端三种显示模式：仅昵称、昵称+原名、隐藏
- ✅ 团队颜色生效
- ✅ 昵称查找玩家

## 安装

### 服务端

1. 将 `nick-*.jar` 放入 `mods/` 目录（需要 Fabric API 与 Carpet）
2. 重启服务器

### 客户端（可选）

客户端安装后，头顶名签可在服务器模式下显示昵称，并可通过客户端配置调整显示方式。不装则仅聊天和 Tab 列表生效。

客户端配置界面需要 [Cloth Config](https://modrinth.com/mod/cloth-config)，不装则只是没有图形界面，功能不受影响。

## 客户端配置

客户端安装后自动生成 `config/nick-client.json`，可调整显示方式。默认模式与逐位置覆盖（头顶 / 聊天 / Tab 列表）均可设置。

| 值                      | 效果               | 示例              |
| ----------------------- | ------------------ | ----------------- |
| `"nick_only"`         | 仅显示昵称（默认） | `脏小豆`        |
| `"nick_and_original"` | 昵称+原名          | `[脏小豆]zxdnb` |
| `"hide"`              | 隐藏昵称，显示原名 | `zxdnb`         |

## 配置文件

`config/nick.json`，格式示例：

```json
{
  "zxdnb": {"nick": "脏小豆"},
  "dongchengqiao": {"nick": "董丞乔"}
}
```

键是玩家的游戏名（不是昵称），写入时会先写临时文件再原子替换，避免写坏文件。

## 技术信息

- Minecraft 版本：26.3
- 框架：Fabric Loader 0.19.5 / Fabric API 0.161.0+26.3
- Maven 组：`com.dongchengqiao.nick`
- 主类：`com.dongchengqiao.nick.Nick`
