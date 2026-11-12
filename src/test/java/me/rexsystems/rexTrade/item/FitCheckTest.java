package me.rexsystems.rexTrade.item;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FitCheckTest {

    @Test
    void usesPartialStacksAndFreeSlots() {
        List<SlotContent> slots = List.of(
                new SlotContent("stone", 60, 64),
                SlotContent.empty(),
                new SlotContent("dirt", 64, 64)
        );
        assertTrue(FitCheck.canFit(slots, new SlotContent("stone", 10, 64)));
        assertFalse(FitCheck.canFit(slots, new SlotContent("obsidian", 65, 64)));
    }

    @Test
    void partialStackAloneCannotHoldTheOverflow() {
        List<SlotContent> slots = List.of(new SlotContent("stone", 60, 64));
        assertTrue(FitCheck.canFit(slots, new SlotContent("stone", 4, 64)));
        assertFalse(FitCheck.canFit(slots, new SlotContent("stone", 5, 64)));
    }

    @Test
    void emptyIncomingFits() {
        assertTrue(FitCheck.canFit(List.of(SlotContent.empty()), SlotContent.empty()));
        assertTrue(FitCheck.canFit(List.of(), null));
    }

    @Test
    void severalStacksConsumeTheSameFreeSlots() {
        List<SlotContent> slots = List.of(SlotContent.empty(), new SlotContent("stone", 64, 64));
        assertFalse(FitCheck.canFitAll(slots, List.of(
                new SlotContent("diamond", 64, 64),
                new SlotContent("emerald", 1, 64)
        )));
        assertTrue(FitCheck.canFitAll(List.of(SlotContent.empty(), SlotContent.empty()), List.of(
                new SlotContent("diamond", 64, 64),
                new SlotContent("emerald", 1, 64)
        )));
    }
}
