# RexTrade

Player to player trades for Paper, Purpur, and Folia. Shift-right-click a player to send a request. Accept opens one shared window. Items are taken out of the owner inventory when they are placed in the offer, and they are returned if the trade stops.

**Java 21. Paper, Purpur, or Folia 1.21+.** Vault and PlaceholderAPI are optional.

## Features

- A request only cancels the click when you are sneaking, the target is a player, and you have `rextrade.use`
- Both players put items on their own side and set currency offers
- Both must accept. If either inventory cannot hold the incoming items, the trade stays open and nothing is exchanged
- Currency is taken from both players first. If the second take fails, the first take is refunded and items stay where they are
- `settings.tax-percent` defaults to 0. Tax is removed from each currency offer and is not paid to a player
- Right-click a shulker or barrel in your offer, or click one on their side, to open a view you cannot take from
- Disconnect, death, distance, world change, cancel, and plugin disable give the offers back
- A full inventory drops the leftover at that player's feet
- Successful trades append one line to `plugins/RexTrade/trades.log`

## Commands

| Command | Description | Permission |
|---|---|---|
| `/rextrade help` | Show help | everyone |
| `/rextrade accept` | Open the trade you were offered | `rextrade.use` |
| `/rextrade deny` | Decline a request | `rextrade.use` |
| `/rextrade cancel` | Stop the open trade and return both offers | `rextrade.use` |
| `/rextrade offer <currency> <amount>` | Set your offer for one currency | `rextrade.use` |
| `/rextrade reload` | Reload `config.yml` | `rextrade.reload` |

Aliases: `/rtrade`, `/trade`

## Permissions

| Permission | Default | Description |
|---|---|---|
| `rextrade.use` | true | Send and accept trade requests |
| `rextrade.reload` | op | Reload the configuration |

Documentation: https://docs.rexsystems.me/rextrade/
