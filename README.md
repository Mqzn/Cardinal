# Cardinal
Cardinal is a professional punishment managing plugin designed to gather every essential moderation feature into one powerful suite.

## Setup Instructions
1. **Download the Plugin**: Obtain the latest version of [Cardinal](https://github.com/Mqzn/Cardinal/releases/tag/v1.0.0)
2. **Install the Plugin**: Place the downloaded `.jar` file into your server's `plugins` directory.
3. **Restart the Server**: Restart your Minecraft server to load the plugin.

## Punishments system
A comprehensive and easy-to-use punishment plugin for Minecraft servers, designed to help administrators manage player behavior efficiently.

### ✨Initial Features

#### Commands

| Syntax | Description | Permission |
|---|---|---|
| `/ban <player> [-s] [duration] [reason...]` | Bans a player from the server. | `cardinal.punishments.ban` |
| `/unban <user> [reason...]` | Unbans a player from the server. | `cardinal.punishments.unban` |
| `/mute <user> [-s] [duration] [reason]` | Mutes a player on the server. | `cardinal.punishments.mute` |
| `/unmute <target> [reason...]` | Unmutes a player on the server. | `cardinal.punishments.unmute` |
| `/kick <user> [-s] [reason...]` | Kicks a player from the server. | `cardinal.punishments.kick` |
| `/warn <user> [-s] [reason...]` | Issues a warning to a player. | *(no base permission annotation yet; see notes below)* |
| `/history` | Opens the punishment history GUI (recent punishments). | *(no base permission annotation yet)* |

##### Permissions

| Permission | What it allows | Used by |
|---|---|---|
| `cardinal.punishments.ban` | Use the `/ban` command. | `/ban` |
| `cardinal.punishments.unban` | Use the `/unban` command. | `/unban` |
| `cardinal.punishments.mute` | Use the `/mute` command. | `/mute` |
| `cardinal.punishments.unmute` | Use the `/unmute` command. | `/unmute` |
| `cardinal.punishments.kick` | Use the `/kick` command. | `/kick` |
| `cardinal.punishments.silent` | Use the silent flag (`-s` / `--silent`) on supported commands. | `/ban -s`, `/kick -s` *(only checked when `-s` is used)*; `/mute`, `/warn` *(currently checked even when `-s` isn’t used)* |
| `cardinal.punishments.override` | Override/update an existing active punishment (e.g., change duration/reason on an already-muted/banned target). | `/ban`, `/mute` when target is already punished |
| `cardinal.punishments.notify` | Receive staff notifications about punishments. | Punishment broadcasts/notifications (staff-facing) |

> Notes:
> - `warn` and `history` currently have **no base `@Permission(...)` annotation** in code, so access depends on the command framework defaults and your server’s permission setup.

  ✅ Temporary Mute Command: Mute a player for a specified duration with an optional reason.

  ✅ Permanent Mute Command: Permanently mute a player with an optional reason.

  ✅ Unmute Command: Unmute a previously muted player.

  ✅ Temporary Ban Command: Ban a player for a specified duration with an optional reason.

  ✅ Permanent Ban Command: Permanently ban a player with an optional reason.

  ✅ Unban Command: Unban a previously banned player.

  ✅ Kick Command: Kick a player from the server with an optional reason.

  ✅ Warning System: Issue warnings to players, with a clear history for each player.

  ⬜ Punishment History System: View a player's complete punishment history (mutes, bans, warnings).

  ⬜ Template system with ladder configuration.

  ✅ Configurable Messages: Customize all punishment messages sent to players and staff.

  ✅ Mongo Database Integration.
  
  ⬜ MySQL Database Integration

  ✅ Staff Notifications: Notify online staff members of new punishments.
   
  ✅ Developer API
  
  ⬜ Developer API Documentation

### 🚀 Upcoming Features (TODO)
  GUI for Punishment Management: An in-game graphical user interface for easier management. <br>
  IP Ban Support: Ability to ban players by their IP address. <br>
  Advanced Warning Tiers: Implement a tiered warning system that automatically triggers actions (e.g., mute after 3 warnings).
