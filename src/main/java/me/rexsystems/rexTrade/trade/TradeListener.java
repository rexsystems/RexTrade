package me.rexsystems.rexTrade.trade;

import me.rexsystems.rexTrade.RexTrade;
import me.rexsystems.rexTrade.config.CurrencySpec;
import me.rexsystems.rexTrade.config.GuiSpec;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.math.BigDecimal;
import java.util.List;

public final class TradeListener implements Listener {

    private final RexTrade plugin;

    public TradeListener(RexTrade plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getClass() != PlayerInteractEntityEvent.class) {
            return;
        }
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof Player target)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isSneaking() || !player.hasPermission("rextrade.use")) {
            return;
        }
        event.setCancelled(true);
        plugin.trades().request(player, target);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof TradeMenus.PreviewHolder) {
            event.setCancelled(true);
            return;
        }
        if (!(top.getHolder() instanceof TradeMenus.TradeHolder holder)) {
            return;
        }
        TradeSession session = plugin.trades().session(holder.sessionId());
        if (session == null || !session.involves(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getClick() == ClickType.DOUBLE_CLICK || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() == top) {
            event.setCancelled(true);
            if (session.isTrading()) {
                handleTop(player, session, event);
            }
            return;
        }
        if (event.isShiftClick()) {
            event.setCancelled(true);
            if (session.isTrading()) {
                moveFromPlayer(player, session, event.getSlot());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof TradeMenus.PreviewHolder) {
            event.setCancelled(true);
            return;
        }
        if (!(top.getHolder() instanceof TradeMenus.TradeHolder)) {
            return;
        }
        int topSize = top.getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onCreative(InventoryCreativeEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof TradeMenus.PreviewHolder
                || top.getHolder() instanceof TradeMenus.TradeHolder) {
            if (event.getRawSlot() < top.getSize()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof TradeMenus.TradeHolder
                || event.getDestination().getHolder() instanceof TradeMenus.TradeHolder
                || event.getSource().getHolder() instanceof TradeMenus.PreviewHolder
                || event.getDestination().getHolder() instanceof TradeMenus.PreviewHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof TradeMenus.PreviewHolder holder) {
            TradeSession session = plugin.trades().session(holder.sessionId());
            if (session != null && session.isOpen()) {
                me.rexsystems.rexTrade.util.SchedulerUtils.runForPlayerLater(plugin, player, () -> {
                    Inventory view = session.view(player.getUniqueId());
                    if (view != null && session.isOpen()) {
                        player.openInventory(view);
                    }
                }, 1L);
            }
            return;
        }
        if (!(inventory.getHolder() instanceof TradeMenus.TradeHolder holder)) {
            return;
        }
        TradeSession session = plugin.trades().session(holder.sessionId());
        if (session == null) {
            return;
        }
        if (session.consumeSuppress(player.getUniqueId())) {
            return;
        }
        if (!session.isTrading()) {
            return;
        }
        plugin.trades().cancelClosed(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.trades().cancelQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        plugin.trades().cancelDeath(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorld(PlayerChangedWorldEvent event) {
        plugin.trades().cancelWorld(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameMode(PlayerGameModeChangeEvent event) {
        if (!plugin.getConfigManager().settings().gamemodeBlocked(event.getNewGameMode().name())) {
            return;
        }
        plugin.trades().cancelGameMode(event.getPlayer());
    }

    private void handleTop(Player player, TradeSession session, InventoryClickEvent event) {
        GuiSpec gui = session.rules().gui();
        int raw = event.getRawSlot();
        int ownIndex = indexOf(gui.ownSlots(), raw);
        if (ownIndex >= 0) {
            handleOwn(player, session, event, ownIndex);
            return;
        }
        int partnerIndex = indexOf(gui.partnerSlots(), raw);
        if (partnerIndex >= 0) {
            ItemStack shown = peekPartner(session, player, partnerIndex);
            if (ContainerPreview.isContainer(shown)
                    && (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT)) {
                openPreview(player, session, shown);
            }
            return;
        }
        if (raw == gui.ownAcceptSlot() && (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT)) {
            boolean ready = session.toggleAccept(player.getUniqueId());
            plugin.trades().refreshActor(session, player);
            if (ready) {
                plugin.trades().startConfirm(session);
            }
            return;
        }
        int currencyIndex = indexOf(gui.currencySlots(), raw);
        if (currencyIndex >= 0) {
            adjustCurrency(player, session, currencyIndex, event.getClick());
        }
    }

    private void handleOwn(Player player, TradeSession session, InventoryClickEvent event, int index) {
        ClickType click = event.getClick();
        if (event.isShiftClick()) {
            moveOfferToPlayer(player, session, index);
            return;
        }
        if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
            dropOffer(player, session, index, click == ClickType.DROP);
            return;
        }
        if (click == ClickType.NUMBER_KEY) {
            swap(player, session, index, event.getHotbarButton());
            return;
        }
        if (click == ClickType.SWAP_OFFHAND) {
            swapOffhand(player, session, index);
            return;
        }
        if (click == ClickType.LEFT || click == ClickType.RIGHT) {
            ItemStack cursor = player.getItemOnCursor();
            ItemStack shown = session.peek(player.getUniqueId(), index);
            if ((cursor == null || cursor.getType().isAir()) && ContainerPreview.isContainer(shown)) {
                openPreview(player, session, shown);
                return;
            }
        }
        if (click != ClickType.LEFT && click != ClickType.RIGHT) {
            return;
        }
        ItemStack cursor = player.getItemOnCursor();
        boolean held = cursor != null && !cursor.getType().isAir();
        if (held) {
            player.setItemOnCursor(null);
        }
        TradeSession.CursorEdit edit = session.applyCursor(player.getUniqueId(), index, held ? cursor : null, click == ClickType.RIGHT);
        player.setItemOnCursor(edit.cursor());
        plugin.trades().refreshActor(session, player);
    }

    private void moveFromPlayer(Player player, TradeSession session, int slot) {
        PlayerInventory inventory = player.getInventory();
        ItemStack stack = inventory.getItem(slot);
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return;
        }
        inventory.setItem(slot, null);
        int leftover = session.addOffer(player.getUniqueId(), stack);
        if (leftover > 0) {
            ItemStack back = stack.clone();
            back.setAmount(leftover);
            inventory.setItem(slot, back);
        }
        plugin.trades().refreshActor(session, player);
    }

    private void moveOfferToPlayer(Player player, TradeSession session, int index) {
        ItemStack taken = session.takeSlot(player.getUniqueId(), index);
        if (taken == null) {
            return;
        }
        List<ItemStack> leftover = me.rexsystems.rexTrade.item.ItemPlacer.place(player.getInventory(), List.of(taken));
        for (ItemStack stack : leftover) {
            int remain = session.addOffer(player.getUniqueId(), stack);
            if (remain > 0) {
                ItemStack drop = stack.clone();
                drop.setAmount(remain);
                player.getWorld().dropItem(player.getLocation(), drop);
            }
        }
        plugin.trades().refreshActor(session, player);
    }

    private void dropOffer(Player player, TradeSession session, int index, boolean one) {
        ItemStack taken = session.takeSlot(player.getUniqueId(), index);
        if (taken == null) {
            return;
        }
        if (one && taken.getAmount() > 1) {
            ItemStack single = taken.clone();
            single.setAmount(1);
            taken.setAmount(taken.getAmount() - 1);
            session.restoreSlot(player.getUniqueId(), index, taken);
            player.getWorld().dropItem(player.getLocation(), single);
        } else {
            player.getWorld().dropItem(player.getLocation(), taken);
        }
        plugin.trades().refreshActor(session, player);
    }

    private void swap(Player player, TradeSession session, int index, int hotbar) {
        if (hotbar < 0 || hotbar > 8) {
            return;
        }
        ItemStack hotbarStack = player.getInventory().getItem(hotbar);
        player.getInventory().setItem(hotbar, null);
        TradeSession.HotbarEdit edit = session.swapHotbar(player.getUniqueId(), index, hotbarStack);
        if (edit.restore() != null) {
            player.getInventory().setItem(hotbar, edit.restore());
        } else {
            player.getInventory().setItem(hotbar, edit.toHotbar());
        }
        plugin.trades().refreshActor(session, player);
    }

    private void swapOffhand(Player player, TradeSession session, int index) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        player.getInventory().setItemInOffHand(null);
        TradeSession.HotbarEdit edit = session.swapHotbar(player.getUniqueId(), index, offhand);
        if (edit.restore() != null) {
            player.getInventory().setItemInOffHand(edit.restore());
        } else {
            player.getInventory().setItemInOffHand(edit.toHotbar());
        }
        plugin.trades().refreshActor(session, player);
    }

    private void adjustCurrency(Player player, TradeSession session, int index, ClickType click) {
        List<CurrencySpec> currencies = session.rules().currencies();
        if (index >= currencies.size()) {
            return;
        }
        CurrencySpec currency = currencies.get(index);
        BigDecimal increment = currency.increment();
        if (click == ClickType.SHIFT_RIGHT) {
            session.setMoney(player.getUniqueId(), currency.id(), BigDecimal.ZERO);
        } else if (click == ClickType.SHIFT_LEFT) {
            session.addMoney(player.getUniqueId(), currency.id(), increment.multiply(BigDecimal.TEN));
        } else if (click == ClickType.RIGHT) {
            session.addMoney(player.getUniqueId(), currency.id(), increment.negate());
        } else if (click == ClickType.LEFT) {
            session.addMoney(player.getUniqueId(), currency.id(), increment);
        } else {
            return;
        }
        plugin.trades().refreshActor(session, player);
    }

    private void openPreview(Player player, TradeSession session, ItemStack source) {
        session.suppressClose(player.getUniqueId());
        player.openInventory(TradeMenus.preview(session, player.getUniqueId(), source));
    }

    private ItemStack peekPartner(TradeSession session, Player viewer, int index) {
        return session.peek(session.partner(viewer.getUniqueId()), index);
    }

    private static int indexOf(int[] slots, int raw) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == raw) {
                return i;
            }
        }
        return -1;
    }
}
