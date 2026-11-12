package me.rexsystems.rexTrade.money;

import java.math.BigDecimal;

public interface Ledger {

    boolean take(BigDecimal amount);

    void give(BigDecimal amount);
}
