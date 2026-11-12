---
title: Commands
description: RexTrade commands and aliases.
sidebar:
  order: 1
---

The command is `/rextrade`. Aliases are `/rtrade` and `/trade`.

| Command | What it does |
|---|---|
| `/rextrade help` | Prints the help lines from `messages.help` |
| `/rextrade accept` | Opens a session from the latest request sent to you |
| `/rextrade deny` | Declines that request |
| `/rextrade cancel` | Cancels your open trade and returns both offers |
| `/rextrade offer <currency> <amount>` | Sets your offer for that currency id |
| `/rextrade reload` | Reloads `config.yml` for the next request |

`/rextrade reload` does not close an open trade and does not replace that trade's rules.
