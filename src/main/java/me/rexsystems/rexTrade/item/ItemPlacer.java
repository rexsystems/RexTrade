package me.rexsystems.rexTrade.item;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

public final class ItemPlacer {

    private ItemPlacer() {
    }

    public static List<ItemStack> place(PlayerInventory inventory, List<ItemStack> incoming) {
        List<ItemStack> leftovers = new ArrayList<>();
        if (inventory == null || incoming == null || incoming.isEmpty()) {
            return leftovers;
        }
        ItemStack[] storage = inventory.getStorageContents();
        for (ItemStack stack : incoming) {
            if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
                continue;
            }
            int remaining = stack.getAmount();
            int max = Math.max(1, stack.getMaxStackSize());
            for (int i = 0; i < storage.length && remaining > 0; i++) {
                ItemStack slot = storage[i];
                if (slot == null || slot.getType().isAir() || !slot.isSimilar(stack)) {
                    continue;
                }
                int cap = Math.min(max, Math.max(1, slot.getMaxStackSize()));
                int space = cap - slot.getAmount();
                if (space <= 0) {
                    continue;
                }
                int move = Math.min(space, remaining);
                slot.setAmount(slot.getAmount() + move);
                remaining -= move;
            }
            for (int i = 0; i < storage.length && remaining > 0; i++) {
                ItemStack slot = storage[i];
                if (slot != null && !slot.getType().isAir()) {
                    continue;
                }
                int move = Math.min(max, remaining);
                ItemStack part = stack.clone();
                part.setAmount(move);
                storage[i] = part;
                remaining -= move;
            }
            if (remaining > 0) {
                ItemStack left = stack.clone();
                left.setAmount(remaining);
                leftovers.add(left);
            }
        }
        inventory.setStorageContents(storage);
        return leftovers;
    }
}
