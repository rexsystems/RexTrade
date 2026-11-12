---
title: RexTrade
description: Player to player item and currency trades for Paper, Purpur, and Folia.
---

RexTrade opens one shared trade after a player accepts a request. Each person fills their own offer. Items are removed from the player when they enter that offer. Both players have to accept, and both inventories have to fit the incoming items, before anything is exchanged.

Messages use MiniMessage. Legacy `&` codes, `&#RRGGBB`, and `#RRGGBB` also work.

## Files

```
plugins/RexTrade/
  config.yml
  trades.log
```

A reload that changes the file writes `config.yml.bak.<timestamp>` first, then adds any keys that were missing. Values you already set stay as they are. An open trade keeps the rules it started with.

## Next

- [Installation](/rextrade/getting-started/installation/)
- [Currencies](/rextrade/configuration/currencies/)
- [Trading](/rextrade/guides/trading/)
