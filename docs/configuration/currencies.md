---
title: Currencies
description: Vault balances and placeholder currencies.
sidebar:
  order: 2
---

Each entry under `currencies` is one button in the trade window. The id is what `/rextrade offer` uses.

```yaml
currencies:
  money:
    type: vault
    name: "Money"
    material: GOLD_INGOT
    increment: 100
  tokens:
    type: placeholder
    name: "Tokens"
    material: SUNFLOWER
    increment: 10
    balance-placeholder: "%tokens_balance%"
    give-command: "tokens give {player} {amount}"
    take-command: "tokens take {player} {amount}"
```

## Vault

`type: vault` withdraws and deposits through the Vault economy. If Vault has no economy, a trade that offers this currency does not confirm.

Left click adds `increment`. Right click subtracts it. Shift-left click adds ten increments. Shift-right click clears the offer. `/rextrade offer money 1500` sets an exact amount.

## Placeholder

`type: placeholder` reads `balance-placeholder` for the player and runs `take-command` and `give-command` from the console. `{player}` and `{amount}` are replaced. The take counts only when the balance placeholder drops by the amount. If it does not, the charge fails.

The take command has to update the balance before it returns. A command that changes the balance on a later tick is treated as a failed charge.

## Tax and order

`settings.tax-percent` is applied to each currency offer separately. The plugin takes the full offer from both players first. If the second take fails, the first take is given back and no items move. After both takes succeed, each player receives the other offer minus tax. The tax is deleted. It is not deposited anywhere.

With tax at `0`, the other player receives the full offer.

With tax at `10` and an offer of `100`, the payer loses `100` and the other player receives `90`.
