# RexTrade

Player to player trades for Paper, Purpur, and Folia. Shift-right-click someone to ask. When they accept, both of you fill one shared window. Items leave your inventory when they go into the offer, and they come back to you if the trade does not finish.

<p>
  <img src="https://img.shields.io/badge/Minecraft-1.21%2B-62B47A?style=for-the-badge" alt="Minecraft 1.21+">
  <img src="https://img.shields.io/badge/Java-21-blue?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21">
</p>

## Features

- Shift-right-click sends a request. Other clicks are left alone
- One shared session after accept, with both offer areas, currency buttons, and accept buttons
- Both players must accept, and both inventories must have room, or nothing moves
- Vault money and placeholder currencies
- Tax percent, default 0, removed from each currency offer and paid to nobody
- Shulker and barrel offers can be opened as a read-only view
- Cancel, death, quit, world change, distance, and plugin disable return items
- If a return does not fit, the leftover drops at that player's location
- Compact or full numbers, and a line appended to `plugins/RexTrade/trades.log`

## Requirements

- Java 21
- Paper, Purpur, or Folia 1.21+
- Vault, if you use the vault currency
- PlaceholderAPI, if you use a placeholder currency

## Install

1. Put the jar in `plugins/`
2. Restart
3. Edit `plugins/RexTrade/config.yml`
4. Run `/rextrade reload`

Reload does not cancel a trade that is already open. That session keeps the rules it started with.

## Commands

| Command | Permission |
|---|---|
| `/rextrade help` | everyone |
| `/rextrade accept` | `rextrade.use` |
| `/rextrade deny` | `rextrade.use` |
| `/rextrade cancel` | `rextrade.use` |
| `/rextrade offer <currency> <amount>` | `rextrade.use` |
| `/rextrade reload` | `rextrade.reload` |

Aliases: `/rtrade`, `/trade`

## Documentation

The pages in [docs/](docs/index.md) drop into a Starlight site at `src/content/docs/rextrade/`. Paste `docs/sidebar.mjs` into the site sidebar.

Full docs: [https://docs.rexsystems.me/rextrade/](https://docs.rexsystems.me/rextrade/)

## License

Licensed under the [GNU Affero General Public License v3.0](LICENSE).
