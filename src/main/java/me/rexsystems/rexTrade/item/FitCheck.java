package me.rexsystems.rexTrade.item;

import java.util.ArrayList;
import java.util.List;

public final class FitCheck {

    private FitCheck() {
    }

    public static boolean canFit(List<SlotContent> slots, SlotContent incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return true;
        }
        return canFitAll(slots, List.of(incoming));
    }

    public static boolean canFitAll(List<SlotContent> slots, List<SlotContent> incoming) {
        List<Working> working = new ArrayList<>();
        if (slots != null) {
            for (SlotContent slot : slots) {
                working.add(Working.from(slot));
            }
        }
        if (incoming == null) {
            return true;
        }
        for (SlotContent stack : incoming) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            int maxStack = Math.max(1, stack.maxStack());
            if (!place(working, stack.key(), stack.amount(), maxStack)) {
                return false;
            }
        }
        return true;
    }

    private static boolean place(List<Working> slots, String key, int amount, int maxStack) {
        int remaining = amount;
        for (Working slot : slots) {
            if (remaining <= 0) {
                return true;
            }
            if (slot.empty() || !key.equals(slot.key)) {
                continue;
            }
            int cap = Math.min(maxStack, Math.max(1, slot.maxStack));
            int space = cap - slot.amount;
            if (space <= 0) {
                continue;
            }
            int move = Math.min(space, remaining);
            slot.amount += move;
            remaining -= move;
        }
        for (Working slot : slots) {
            if (remaining <= 0) {
                return true;
            }
            if (!slot.empty()) {
                continue;
            }
            int move = Math.min(maxStack, remaining);
            slot.key = key;
            slot.maxStack = maxStack;
            slot.amount = move;
            remaining -= move;
        }
        return remaining <= 0;
    }

    private static final class Working {
        private String key;
        private int amount;
        private int maxStack;

        private static Working from(SlotContent slot) {
            Working working = new Working();
            if (slot == null || slot.isEmpty()) {
                working.key = "";
                working.amount = 0;
                working.maxStack = 0;
                return working;
            }
            working.key = slot.key();
            working.amount = slot.amount();
            working.maxStack = slot.maxStack();
            return working;
        }

        private boolean empty() {
            return amount <= 0 || key == null || key.isEmpty();
        }
    }
}
