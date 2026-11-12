package me.rexsystems.rexTrade.item;

public record SlotContent(String key, int amount, int maxStack) {

    public SlotContent {
        if (amount < 0) {
            throw new IllegalArgumentException("amount");
        }
        if (maxStack < 0) {
            throw new IllegalArgumentException("maxStack");
        }
        if (key == null) {
            key = "";
        }
    }

    public static SlotContent empty() {
        return new SlotContent("", 0, 0);
    }

    public boolean isEmpty() {
        return amount <= 0 || key.isEmpty();
    }
}
