package me.rexsystems.rexTrade.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxTest {

    @Test
    void zeroPercentRemovesNothing() {
        BigDecimal offer = new BigDecimal("100");
        assertEquals(0, Tax.tax(offer, BigDecimal.ZERO).compareTo(new BigDecimal("0.00")));
        assertEquals(0, Tax.payout(offer, new BigDecimal("0")).compareTo(new BigDecimal("100.00")));
    }

    @Test
    void tenPercentOfOneHundred() {
        BigDecimal offer = new BigDecimal("100");
        BigDecimal percent = new BigDecimal("10");
        assertEquals(0, Tax.tax(offer, percent).compareTo(new BigDecimal("10.00")));
        assertEquals(0, Tax.payout(offer, percent).compareTo(new BigDecimal("90.00")));
    }

    @Test
    void taxPlusPayoutEqualsOffer() {
        BigDecimal offer = new BigDecimal("10000000");
        BigDecimal percent = new BigDecimal("10");
        assertEquals(0, Tax.tax(offer, percent).add(Tax.payout(offer, percent)).compareTo(offer));
    }

    @Test
    void percentAboveOneHundredCannotTakeMoreThanTheOffer() {
        BigDecimal offer = new BigDecimal("50");
        assertEquals(0, Tax.tax(offer, new BigDecimal("150")).compareTo(new BigDecimal("50.00")));
        assertEquals(0, Tax.payout(offer, new BigDecimal("150")).compareTo(new BigDecimal("0.00")));
    }
}
