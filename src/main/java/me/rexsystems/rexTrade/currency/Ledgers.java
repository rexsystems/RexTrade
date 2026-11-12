package me.rexsystems.rexTrade.currency;

import me.rexsystems.rexTrade.config.CurrencySpec;
import me.rexsystems.rexTrade.money.Ledger;
import org.bukkit.entity.Player;

public final class Ledgers {

    private Ledgers() {
    }

    public static Ledger forPlayer(Player player, CurrencySpec spec) {
        if (spec != null && spec.placeholder()) {
            return new PlaceholderLedger(player, spec);
        }
        return new VaultLedger(player, spec);
    }
}
