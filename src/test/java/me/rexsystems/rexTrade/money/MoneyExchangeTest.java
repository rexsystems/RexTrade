package me.rexsystems.rexTrade.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyExchangeTest {

    @Test
    void secondTakeFailureRefundsTheFirstTakeAndDoesNotMoveItems() {
        MemoryLedger first = new MemoryLedger("100");
        MemoryLedger second = new MemoryLedger("10");

        ExchangeResult result = MoneyExchange.exchange(
                first,
                second,
                new BigDecimal("40"),
                new BigDecimal("50"),
                new BigDecimal("50"),
                new BigDecimal("40"));

        assertFalse(result.isCompleted());
        assertTrue(result.isFirstTakeRefunded());
        assertFalse(result.wereItemsMoved());
        assertEquals(0, first.balance().compareTo(new BigDecimal("100")));
        assertEquals(0, second.balance().compareTo(new BigDecimal("10")));
    }

    @Test
    void bothTakesThenPayouts() {
        MemoryLedger first = new MemoryLedger("200");
        MemoryLedger second = new MemoryLedger("200");

        ExchangeResult result = MoneyExchange.exchange(
                first,
                second,
                new BigDecimal("100"),
                new BigDecimal("40"),
                new BigDecimal("36"),
                new BigDecimal("90"));

        assertTrue(result.isCompleted());
        assertFalse(result.wereItemsMoved());
        assertEquals(0, first.balance().compareTo(new BigDecimal("136")));
        assertEquals(0, second.balance().compareTo(new BigDecimal("250")));
    }

    @Test
    void failedFirstTakeDoesNotTouchTheSecondLedger() {
        MemoryLedger first = new MemoryLedger("5");
        MemoryLedger second = new MemoryLedger("80");

        ExchangeResult result = MoneyExchange.exchange(
                first,
                second,
                new BigDecimal("40"),
                new BigDecimal("10"),
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        assertFalse(result.isCompleted());
        assertFalse(result.isFirstTakeRefunded());
        assertEquals(0, first.balance().compareTo(new BigDecimal("5")));
        assertEquals(0, second.balance().compareTo(new BigDecimal("80")));
    }

    private static final class MemoryLedger implements Ledger {
        private BigDecimal balance;

        private MemoryLedger(String start) {
            this.balance = new BigDecimal(start);
        }

        private BigDecimal balance() {
            return balance;
        }

        @Override
        public boolean take(BigDecimal amount) {
            if (amount == null || amount.signum() <= 0) {
                return true;
            }
            if (balance.compareTo(amount) < 0) {
                return false;
            }
            balance = balance.subtract(amount);
            return true;
        }

        @Override
        public void give(BigDecimal amount) {
            if (amount == null || amount.signum() <= 0) {
                return;
            }
            balance = balance.add(amount);
        }
    }
}
