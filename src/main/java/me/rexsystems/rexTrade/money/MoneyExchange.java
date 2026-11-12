package me.rexsystems.rexTrade.money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class MoneyExchange {

    private MoneyExchange() {
    }

    public static ExchangeResult exchange(Ledger first, Ledger second, BigDecimal firstAmount, BigDecimal secondAmount,
                                          BigDecimal payoutToFirst, BigDecimal payoutToSecond) {
        return exchangeAll(List.of(new Leg(first, second, firstAmount, secondAmount, payoutToFirst, payoutToSecond)));
    }

    public static ExchangeResult exchangeAll(List<Leg> legs) {
        if (legs == null || legs.isEmpty()) {
            return ExchangeResult.completed();
        }
        // Both takes finish before any payout. A failed second take pays the first take back.
        // This object never moves items; the caller does that only after a completed result.
        List<Refund> refunds = new ArrayList<>();
        for (Leg leg : legs) {
            if (leg == null) {
                continue;
            }
            BigDecimal leftTake = positive(leg.leftTake());
            BigDecimal rightTake = positive(leg.rightTake());
            if (leftTake.signum() > 0) {
                if (!safeTake(leg.left(), leftTake)) {
                    refund(refunds);
                    return ExchangeResult.firstTakeFailed();
                }
                refunds.add(new Refund(leg.left(), leftTake));
            }
            if (rightTake.signum() > 0) {
                if (!safeTake(leg.right(), rightTake)) {
                    refund(refunds);
                    return ExchangeResult.secondTakeRefunded();
                }
                refunds.add(new Refund(leg.right(), rightTake));
            }
        }
        for (Leg leg : legs) {
            if (leg == null) {
                continue;
            }
            safeGive(leg.left(), leg.leftGain());
            safeGive(leg.right(), leg.rightGain());
        }
        return ExchangeResult.completed();
    }

    private static boolean safeTake(Ledger ledger, BigDecimal amount) {
        if (ledger == null) {
            return false;
        }
        try {
            return ledger.take(amount);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static void safeGive(Ledger ledger, BigDecimal amount) {
        if (ledger == null || amount == null || amount.signum() <= 0) {
            return;
        }
        ledger.give(amount);
    }

    private static void refund(List<Refund> refunds) {
        for (int i = refunds.size() - 1; i >= 0; i--) {
            Refund refund = refunds.get(i);
            safeGive(refund.ledger, refund.amount);
        }
    }

    private static BigDecimal positive(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return amount;
    }

    public record Leg(Ledger left, Ledger right, BigDecimal leftTake, BigDecimal rightTake, BigDecimal leftGain,
                      BigDecimal rightGain) {
    }

    private record Refund(Ledger ledger, BigDecimal amount) {
    }
}
