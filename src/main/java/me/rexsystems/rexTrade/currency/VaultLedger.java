package me.rexsystems.rexTrade.currency;

import me.rexsystems.rexTrade.config.CurrencySpec;
import me.rexsystems.rexTrade.money.Ledger;
import me.rexsystems.rexTrade.util.CompactNumbers;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;

public final class VaultLedger implements Ledger {

    private final OfflinePlayer player;

    public VaultLedger(OfflinePlayer player, CurrencySpec spec) {
        this.player = player;
        if (spec != null && spec.placeholder()) {
            throw new IllegalArgumentException(spec.id());
        }
    }

    @Override
    public boolean take(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return true;
        }
        return VaultHook.withdraw(player, amount.doubleValue());
    }

    @Override
    public void give(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        VaultHook.deposit(player, amount.doubleValue());
    }

    public static String plain(BigDecimal amount) {
        return CompactNumbers.plain(amount);
    }
}
