package me.rexsystems.rexTrade.currency;

import me.rexsystems.rexTrade.config.CurrencySpec;
import me.rexsystems.rexTrade.money.Ledger;
import me.rexsystems.rexTrade.util.CompactNumbers;
import me.rexsystems.rexTrade.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

public final class PlaceholderLedger implements Ledger {

    private final Player player;
    private final CurrencySpec spec;

    public PlaceholderLedger(Player player, CurrencySpec spec) {
        this.player = player;
        this.spec = spec;
    }

    @Override
    public boolean take(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return true;
        }
        if (spec.takeCommand() == null || spec.takeCommand().isBlank()) {
            return false;
        }
        BigDecimal before = balance();
        if (before.compareTo(amount) < 0) {
            return false;
        }
        dispatch(spec.takeCommand(), amount);
        BigDecimal after = balance();
        BigDecimal delta = before.subtract(after);
        if (delta.compareTo(amount) < 0) {
            if (delta.signum() > 0) {
                dispatch(spec.giveCommand(), delta);
            }
            return false;
        }
        if (delta.compareTo(amount) > 0 && spec.giveCommand() != null && !spec.giveCommand().isBlank()) {
            dispatch(spec.giveCommand(), delta.subtract(amount));
        }
        return true;
    }

    @Override
    public void give(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        dispatch(spec.giveCommand(), amount);
    }

    private BigDecimal balance() {
        String raw = Placeholders.apply(player, spec.balancePlaceholder());
        return BalanceText.parse(raw);
    }

    private void dispatch(String command, BigDecimal amount) {
        if (command == null || command.isBlank() || player == null) {
            return;
        }
        String rendered = command
                .replace("{player}", player.getName())
                .replace("{amount}", CompactNumbers.plain(amount))
                .replace("%player%", player.getName())
                .replace("%amount%", CompactNumbers.plain(amount));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rendered);
    }
}
