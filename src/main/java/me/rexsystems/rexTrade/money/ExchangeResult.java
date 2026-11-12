package me.rexsystems.rexTrade.money;

public final class ExchangeResult {

    private final boolean completed;
    private final boolean firstTakeRefunded;
    private final boolean itemsMoved;

    private ExchangeResult(boolean completed, boolean firstTakeRefunded) {
        this.completed = completed;
        this.firstTakeRefunded = firstTakeRefunded;
        this.itemsMoved = false;
    }

    public static ExchangeResult completed() {
        return new ExchangeResult(true, false);
    }

    public static ExchangeResult firstTakeFailed() {
        return new ExchangeResult(false, false);
    }

    public static ExchangeResult secondTakeRefunded() {
        return new ExchangeResult(false, true);
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isFirstTakeRefunded() {
        return firstTakeRefunded;
    }

    public boolean wereItemsMoved() {
        return itemsMoved;
    }
}
