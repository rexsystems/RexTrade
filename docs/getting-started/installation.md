---
title: Installation
description: Put RexTrade on a Paper, Purpur, or Folia server.
sidebar:
  order: 1
---

1. Download the jar.
2. Put it in the server `plugins/` folder.
3. Start the server once so `plugins/RexTrade/config.yml` is written.
4. Edit the file.
5. Run `/rextrade reload`, or restart.

Vault is only needed for a currency whose `type` is `vault`. PlaceholderAPI is only needed for a currency whose `type` is `placeholder`.

`/rextrade reload` reads the files for the next request. It does not cancel a trade that is already open, and that trade keeps the timeout, distance, tax, currencies, and messages it started with.
