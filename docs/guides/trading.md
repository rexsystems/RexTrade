---
title: Trading
description: Requests, the shared window, and when items move.
sidebar:
  order: 1
---

## Send a request

Sneak and right-click another player. That click is cancelled only when all three of these are true:

- you are sneaking
- the target is a player
- you have `rextrade.use`

Any other click continues as it normally would. A request is refused in a blocked world or gamemode, across worlds, or past `settings.max-distance`.

The other player runs `/rextrade accept` or `/rextrade deny`. The request expires after `settings.request-timeout-seconds`.

## The window

Accept opens one session for both players. Your items are on the left. Their items are on the right. You can only change your own side.

Putting an item in your offer removes it from your inventory. It lives in the session until the trade finishes or is cancelled. Taking it back out of the offer puts it in your inventory again.

Click a shulker box or barrel in either offer, with an empty cursor, to open its contents. The view does not let you take those items. Closing it returns you to the trade. Shift-click the shulker or barrel in your own offer to put it back in your inventory.

Currency buttons sit along the bottom. The accept button is on your side. Their button shows whether they have accepted. Changing an offer clears both acceptances.

## Confirm

Both players must accept. RexTrade then counts free slots and leftover stack space in both inventories. If either inventory cannot hold the incoming items, the trade stays open, both acceptances clear, and nothing is charged.

When both inventories fit, currency is taken from both players. A failed second take refunds the first take. Items move only after both takes and the payouts succeed. Each player then gets a summary, and one line is appended to `plugins/RexTrade/trades.log`.

## Stopping

These all return each offer to its owner:

- closing the window
- `/rextrade cancel`
- the other player quits or dies
- either player changes world
- the two players move farther apart than `settings.max-distance`
- the plugin is disabled

The return runs on that player's region scheduler. Whatever does not fit in the inventory is dropped at their location. It is not deleted.

On death, the offer is added to the death drops unless keep-inventory is on, in which case it goes back into the inventory.
