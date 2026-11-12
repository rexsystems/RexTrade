package me.rexsystems.rexTrade.item;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ItemFit {

    private ItemFit() {
    }

    public static boolean canFit(ItemStack[] storage, List<ItemStack> incoming) {
        List<ItemStack> prototypes = new ArrayList<>();
        return FitCheck.canFitAll(toSlots(storage, prototypes), toSlots(incoming, prototypes));
    }

    private static List<SlotContent> toSlots(ItemStack[] stacks, List<ItemStack> prototypes) {
        List<SlotContent> slots = new ArrayList<>();
        if (stacks == null) {
            return slots;
        }
        for (ItemStack stack : stacks) {
            slots.add(toSlot(stack, prototypes));
        }
        return slots;
    }

    private static List<SlotContent> toSlots(List<ItemStack> stacks, List<ItemStack> prototypes) {
        List<SlotContent> slots = new ArrayList<>();
        if (stacks == null) {
            return slots;
        }
        for (ItemStack stack : stacks) {
            SlotContent slot = toSlot(stack, prototypes);
            if (!slot.isEmpty()) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static SlotContent toSlot(ItemStack stack, List<ItemStack> prototypes) {
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return SlotContent.empty();
        }
        int id = -1;
        for (int i = 0; i < prototypes.size(); i++) {
            if (prototypes.get(i).isSimilar(stack)) {
                id = i;
                break;
            }
        }
        if (id < 0) {
            ItemStack proto = stack.clone();
            proto.setAmount(1);
            prototypes.add(proto);
            id = prototypes.size() - 1;
        }
        int max = stack.getMaxStackSize();
        if (max <= 0) {
            max = 1;
        }
        return new SlotContent("i" + id, stack.getAmount(), max);
    }
}
