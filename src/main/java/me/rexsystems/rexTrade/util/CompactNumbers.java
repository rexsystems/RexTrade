package me.rexsystems.rexTrade.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CompactNumbers {

    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final BigDecimal MILLION = new BigDecimal("1000000");
    private static final BigDecimal BILLION = new BigDecimal("1000000000");

    private CompactNumbers() {
    }

    public static String format(long amount, String mode) {
        return format(BigDecimal.valueOf(amount), mode);
    }

    public static String format(BigDecimal amount, String mode) {
        if (amount == null) {
            return "0";
        }
        if (mode != null && mode.equalsIgnoreCase("full")) {
            return plain(amount);
        }
        BigDecimal abs = amount.abs();
        String suffix;
        BigDecimal divisor;
        if (abs.compareTo(BILLION) >= 0) {
            suffix = "B";
            divisor = BILLION;
        } else if (abs.compareTo(MILLION) >= 0) {
            suffix = "M";
            divisor = MILLION;
        } else if (abs.compareTo(THOUSAND) >= 0) {
            suffix = "K";
            divisor = THOUSAND;
        } else {
            return plain(amount);
        }
        BigDecimal scaled = abs.divide(divisor, 1, RoundingMode.DOWN).stripTrailingZeros();
        String sign = amount.signum() < 0 ? "-" : "";
        return sign + scaled.toPlainString() + suffix;
    }

    public static String plain(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return amount.stripTrailingZeros().toPlainString();
    }
}
