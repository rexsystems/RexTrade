package me.rexsystems.rexTrade.command;

import me.rexsystems.rexTrade.RexTrade;
import me.rexsystems.rexTrade.config.CurrencySpec;
import me.rexsystems.rexTrade.config.Settings;
import me.rexsystems.rexTrade.trade.TradeSession;
import me.rexsystems.rexTrade.util.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TradeCommand implements CommandExecutor, TabCompleter {

    private static final BigDecimal MAX_OFFER = new BigDecimal("1000000000000");

    private final RexTrade plugin;

    public TradeCommand(RexTrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            return help(sender);
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "accept" -> playerAction(sender, player -> plugin.trades().accept(player));
            case "deny" -> playerAction(sender, player -> plugin.trades().deny(player));
            case "cancel" -> playerAction(sender, this::cancel);
            case "offer" -> offer(sender, args);
            default -> help(sender);
        };
    }

    private boolean help(CommandSender sender) {
        Settings settings = plugin.getConfigManager().settings();
        MessageService.send(sender, settings.prefixed("help-header"));
        for (String line : settings.help()) {
            MessageService.send(sender, line);
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        Settings settings = plugin.getConfigManager().settings();
        if (!sender.hasPermission("rextrade.reload")) {
            MessageService.send(sender, settings.prefixed("no-permission"));
            return true;
        }
        plugin.reloadPlugin();
        MessageService.send(sender, plugin.getConfigManager().settings().prefixed("reload"));
        return true;
    }

    private void cancel(Player player) {
        if (plugin.trades().session(player) == null) {
            MessageService.send(player, plugin.getConfigManager().settings().prefixed("not-in-trade"));
            return;
        }
        plugin.trades().cancelClosed(player);
    }

    private boolean offer(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageService.send(sender, plugin.getConfigManager().settings().prefixed("players-only"));
            return true;
        }
        if (!player.hasPermission("rextrade.use")) {
            MessageService.send(player, plugin.getConfigManager().settings().prefixed("no-permission"));
            return true;
        }
        TradeSession session = plugin.trades().session(player);
        Settings settings = session == null ? plugin.getConfigManager().settings() : session.rules();
        if (args.length < 3) {
            MessageService.send(player, settings.prefixed("offer-usage"));
            return true;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(args[2]);
        } catch (NumberFormatException ex) {
            MessageService.send(player, settings.prefixed("offer-usage"));
            return true;
        }
        if (amount.signum() < 0) {
            amount = BigDecimal.ZERO;
        }
        if (amount.compareTo(MAX_OFFER) > 0) {
            amount = MAX_OFFER;
        }
        amount = amount.setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros();
        plugin.trades().offer(player, args[1], amount);
        return true;
    }

    private boolean playerAction(CommandSender sender, java.util.function.Consumer<Player> action) {
        if (!(sender instanceof Player player)) {
            MessageService.send(sender, plugin.getConfigManager().settings().prefixed("players-only"));
            return true;
        }
        action.accept(player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("help", "accept", "deny", "cancel", "offer", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("offer")) {
            Settings settings = plugin.getConfigManager().settings();
            if (sender instanceof Player player) {
                TradeSession session = plugin.trades().session(player);
                if (session != null) {
                    settings = session.rules();
                }
            }
            List<String> ids = new ArrayList<>();
            for (CurrencySpec currency : settings.currencies()) {
                ids.add(currency.id());
            }
            return filter(ids, args[1]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String needle = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(needle)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
