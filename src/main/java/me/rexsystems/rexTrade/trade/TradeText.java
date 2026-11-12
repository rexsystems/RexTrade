package me.rexsystems.rexTrade.trade;

import me.rexsystems.rexTrade.config.CurrencySpec;
import me.rexsystems.rexTrade.config.Settings;
import me.rexsystems.rexTrade.util.CompactNumbers;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TradeText {

    private TradeText() {
    }

    public static String items(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return "nothing";
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
                continue;
            }
            String name = one(stack);
            counts.merge(name, stack.getAmount(), Integer::sum);
        }
        if (counts.isEmpty()) {
            return "nothing";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(entry.getValue()).append(' ').append(entry.getKey());
        }
        return builder.toString();
    }

    public static String one(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return "Air";
        }
        String raw = stack.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.toString();
    }

    public static String money(Settings rules, Map<String, BigDecimal> amounts) {
        if (rules == null || amounts == null || amounts.isEmpty()) {
            return "nothing";
        }
        StringBuilder builder = new StringBuilder();
        for (CurrencySpec currency : rules.currencies()) {
            BigDecimal amount = amounts.getOrDefault(currency.id(), BigDecimal.ZERO);
            if (amount.signum() == 0) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(CompactNumbers.format(amount, rules.formatMode())).append(' ').append(currency.name());
        }
        return builder.isEmpty() ? "nothing" : builder.toString();
    }

    public static String tax(Settings rules, Map<String, BigDecimal> left, Map<String, BigDecimal> right) {
        if (rules.taxPercent().signum() <= 0) {
            return "0";
        }
        StringBuilder builder = new StringBuilder();
        for (CurrencySpec currency : rules.currencies()) {
            BigDecimal offer = left.getOrDefault(currency.id(), BigDecimal.ZERO)
                    .add(right.getOrDefault(currency.id(), BigDecimal.ZERO));
            BigDecimal removed = me.rexsystems.rexTrade.money.Tax.tax(offer, rules.taxPercent());
            if (removed.signum() == 0) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(CompactNumbers.format(removed, rules.formatMode())).append(' ').append(currency.name());
        }
        return builder.isEmpty() ? "0" : builder.toString();
    }
}
