package me.rexsystems.rexTrade.currency;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BalanceText {

    private static final Pattern NUMBER = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?");

    private BalanceText() {
    }

    public static BigDecimal parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        Matcher matcher = NUMBER.matcher(raw.replace(",", ""));
        if (!matcher.find()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(matcher.group());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}
