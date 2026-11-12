---
title: FAQ
description: Disconnects, inventory space, and tax.
sidebar:
  order: 1
---

## Do I get my items back if I disconnect?

Yes. Leaving the server cancels the trade. Your offer is put back into your inventory before the quit finishes. Their offer is returned to them. If your inventory cannot hold the return, the leftover is dropped at your location. The plugin does not delete it.

The same return happens on cancel, death, a world change, walking past `settings.max-distance`, and when the plugin disables. A request that expires or is denied never took items, so there is nothing to give back.

## Why did both of us accept and nothing moved?

The trade confirms only when both inventories can hold the incoming items. RexTrade counts empty slots and the space left in matching stacks. If either side is short, you get the no-space message, the acceptances clear, and the offers stay in the window. Currency is not taken in that case.

A currency charge can also stop the confirm. The plugin takes both offers first. If the second take fails, it refunds the first take and leaves the items in the session.

## What does tax 0 do?

`settings.tax-percent` defaults to `0`. At `0`, nothing is removed. The other player receives the full currency offer.

A percent above `0` is taken out of each currency offer and paid to nobody. The payer still loses the full offer. The receiver gets the offer minus the tax.
