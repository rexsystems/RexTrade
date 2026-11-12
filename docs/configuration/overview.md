---
title: Settings
description: Distance, tax, blocked worlds, GUI slots, and sounds.
sidebar:
  order: 1
---

`settings.enabled` controls new requests. Turning it off and reloading does not cancel a trade that is already open. Stopping the plugin does. On disable, every open offer is returned.

| Key | Default | Meaning |
|---|---|---|
| `settings.request-timeout-seconds` | `30` | How long `/rextrade accept` stays available |
| `settings.max-distance` | `10` | Walking farther than this, in the same world, cancels the trade |
| `settings.tax-percent` | `0` | Taken from each currency offer. `0` removes nothing |
| `settings.number-format` | `short` | `short` writes `1000` as `1K` and `10000000` as `10M`. `full` keeps every digit |
| `settings.blocked-worlds` | empty | New requests are refused in these worlds |
| `settings.blocked-gamemodes` | empty | New requests are refused in these gamemodes. Use names such as `SPECTATOR` |

Players must be in the same world to start. Changing world during a trade cancels it and returns both offers.

GUI slot lists are under `gui`. Your offer slots and their offer slots must be the same length, and no listed slot may be used twice. An invalid layout is replaced by the default 6-row window.

`sounds` names a Bukkit sound for `request`, `accept`, `deny`, `cancel`, and `complete`. Set `enabled: false` to skip one.

Every player-facing string under `messages` and `gui` goes through the same text parser. Placeholders look like `{partner}` and `{amount}`.
