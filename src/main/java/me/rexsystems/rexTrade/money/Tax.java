package me.rexsystems.rexTrade.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Tax {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    private Tax() {
    }

    public static BigDecimal tax(BigDecimal offer, BigDecimal percent) {
        if (offer == null || percent == null || offer.signum() <= 0 || percent.signum() <= 0) {
            return ZERO;
        }
        BigDecimal capped = percent.min(HUNDRED);
        return offer.multiply(capped).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal payout(BigDecimal offer, BigDecimal percent) {
        BigDecimal base = offer == null || offer.signum() <= 0 ? BigDecimal.ZERO : offer;
        return base.subtract(tax(base, percent));
    }
}
