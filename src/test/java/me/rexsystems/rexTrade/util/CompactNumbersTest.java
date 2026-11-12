package me.rexsystems.rexTrade.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompactNumbersTest {

    @Test
    void thousandsUseK() {
        assertEquals("1K", CompactNumbers.format(1000, "short"));
    }

    @Test
    void millionsUseM() {
        assertEquals("10M", CompactNumbers.format(10_000_000L, "short"));
    }

    @Test
    void fullModeKeepsTheDigits() {
        assertEquals("1000", CompactNumbers.format(1000, "full"));
        assertEquals("10000000", CompactNumbers.format(new BigDecimal("10000000"), "full"));
    }

    @Test
    void oneDecimalWhenItIsNotWhole() {
        assertEquals("1.5K", CompactNumbers.format(1500, "short"));
        assertEquals("1M", CompactNumbers.format(1_000_000L, "short"));
        assertEquals("999", CompactNumbers.format(999, "short"));
    }
}
