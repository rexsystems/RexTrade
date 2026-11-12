package me.rexsystems.rexTrade.trade;

import me.rexsystems.rexTrade.RexTrade;
import me.rexsystems.rexTrade.item.ItemPlacer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ItemReturn {

    private final RexTrade plugin;
    private final ConcurrentLinkedQueue<Pending> pending = new ConcurrentLinkedQueue<>();

    public ItemReturn(RexTrade plugin) {
        this.plugin = plugin;
    }

    public void now(Player player, List<ItemStack> items) {
        deliver(player, copies(items), safeLocation(player), true);
    }

    public void later(Player player, List<ItemStack> items) {
        List<ItemStack> stacks = copies(items);
        if (stacks.isEmpty() || player == null) {
            return;
        }
        Location fallback = safeLocation(player);
        Pending delivery = new Pending(player, stacks, fallback);
        pending.add(delivery);
        if (!plugin.isEnabled()) {
            if (delivery.claim()) {
                deliver(player, stacks, fallback, true);
                pending.remove(delivery);
            }
            return;
        }
        player.getScheduler().run(plugin, task -> {
            if (delivery.claim()) {
                deliver(player, stacks, fallback, false);
                pending.remove(delivery);
            }
        }, () -> {
            if (delivery.claim()) {
                drop(fallback, stacks);
                pending.remove(delivery);
            }
        });
    }

    public void dropAt(Location location, List<ItemStack> items) {
        List<ItemStack> stacks = copies(items);
        if (stacks.isEmpty()) {
            return;
        }
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Trade items had nowhere to drop.");
            return;
        }
        Location where = location.clone();
        if (!plugin.isEnabled()) {
            drop(where, stacks);
            return;
        }
        me.rexsystems.rexTrade.util.SchedulerUtils.runAtLocation(plugin, where, () -> drop(where, stacks));
    }

    public void flush() {
        Pending delivery;
        while ((delivery = pending.poll()) != null) {
            if (delivery.claim()) {
                deliver(delivery.player, delivery.items, delivery.fallback, true);
            }
        }
    }

    private void deliver(Player player, List<ItemStack> items, Location fallback, boolean forceInventory) {
        if (items.isEmpty()) {
            return;
        }
        try {
            boolean useInventory = player != null && (forceInventory || (player.isOnline() && !player.isDead()));
            if (useInventory) {
                List<ItemStack> leftover = ItemPlacer.place(player.getInventory(), items);
                Location where = safeLocation(player);
                drop(where == null ? fallback : where, leftover);
                return;
            }
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Could not return items to a player inventory: " + ex.getMessage());
        }
        drop(fallback, items);
    }

    private void drop(Location location, List<ItemStack> items) {
        if (location == null || location.getWorld() == null || items == null) {
            return;
        }
        for (ItemStack stack : items) {
            if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
                continue;
            }
            try {
                location.getWorld().dropItem(location, stack.clone());
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Could not drop a returned item: " + ex.getMessage());
            }
        }
    }

    private static Location safeLocation(Player player) {
        if (player == null) {
            return null;
        }
        try {
            return player.getLocation().clone();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static List<ItemStack> copies(List<ItemStack> items) {
        List<ItemStack> copies = new ArrayList<>();
        if (items == null) {
            return copies;
        }
        for (ItemStack stack : items) {
            if (stack != null && !stack.getType().isAir() && stack.getAmount() > 0) {
                copies.add(stack.clone());
            }
        }
        return copies;
    }

    private static final class Pending {
        private final AtomicBoolean claimed = new AtomicBoolean();
        private final Player player;
        private final List<ItemStack> items;
        private final Location fallback;

        private Pending(Player player, List<ItemStack> items, Location fallback) {
            this.player = player;
            this.items = items;
            this.fallback = fallback;
        }

        private boolean claim() {
            return claimed.compareAndSet(false, true);
        }
    }
}
