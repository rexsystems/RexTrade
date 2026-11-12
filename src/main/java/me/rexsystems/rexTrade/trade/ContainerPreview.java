package me.rexsystems.rexTrade.trade;

import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

public final class ContainerPreview {

    private ContainerPreview() {
    }

    public static boolean isContainer(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        Material type = stack.getType();
        return type == Material.BARREL || type.name().endsWith("SHULKER_BOX");
    }

    public static ItemStack[] contents(ItemStack stack) {
        ItemStack[] items = new ItemStack[27];
        if (stack != null && stack.getItemMeta() instanceof BlockStateMeta meta
                && meta.getBlockState() instanceof Container container) {
            ItemStack[] found = container.getInventory().getContents();
            for (int i = 0; i < items.length && i < found.length; i++) {
                items[i] = found[i] == null || found[i].getType().isAir() ? null : found[i].clone();
            }
        }
        return items;
    }
}
