package me.rexsystems.rexTrade.trade;

import me.rexsystems.rexTrade.config.CurrencySpec;
import me.rexsystems.rexTrade.config.Settings;
import me.rexsystems.rexTrade.money.ExchangeResult;
import me.rexsystems.rexTrade.money.MoneyExchange;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TradeSession {

    public enum State {
        TRADING,
        CONFIRMING,
        DONE,
        CANCELLED
    }

    private final Object lock = new Object();
    private final UUID id = UUID.randomUUID();
    private final UUID playerA;
    private final UUID playerB;
    private final String nameA;
    private final String nameB;
    private final Settings rules;
    private final ItemStack[] offerA;
    private final ItemStack[] offerB;
    private final Map<String, BigDecimal> moneyA = new LinkedHashMap<>();
    private final Map<String, BigDecimal> moneyB = new LinkedHashMap<>();
    private final java.util.Set<UUID> suppressClose = new java.util.HashSet<>();
    private final Map<UUID, org.bukkit.inventory.Inventory> views = new LinkedHashMap<>();

    private State state = State.TRADING;
    private boolean acceptA;
    private boolean acceptB;
    private boolean abortRequested;
    private boolean fitA;
    private boolean fitB;
    private boolean fitASet;
    private boolean fitBSet;
    private boolean fitContinued;
    private io.papermc.paper.threadedregions.scheduler.ScheduledTask distanceTask;
    private Location placeA;
    private Location placeB;

    public TradeSession(UUID playerA, UUID playerB, String nameA, String nameB, Settings rules) {
        this.playerA = playerA;
        this.playerB = playerB;
        this.nameA = nameA;
        this.nameB = nameB;
        this.rules = rules;
        this.offerA = new ItemStack[rules.gui().ownSlots().length];
        this.offerB = new ItemStack[rules.gui().ownSlots().length];
        for (CurrencySpec currency : rules.currencies()) {
            moneyA.put(currency.id(), BigDecimal.ZERO);
            moneyB.put(currency.id(), BigDecimal.ZERO);
        }
    }

    public UUID id() {
        return id;
    }

    public UUID playerA() {
        return playerA;
    }

    public UUID playerB() {
        return playerB;
    }

    public Settings rules() {
        return rules;
    }

    public boolean involves(UUID player) {
        return playerA.equals(player) || playerB.equals(player);
    }

    public UUID partner(UUID player) {
        return playerA.equals(player) ? playerB : playerA;
    }

    public String nameOf(UUID player) {
        return playerA.equals(player) ? nameA : nameB;
    }

    public State state() {
        synchronized (lock) {
            return state;
        }
    }

    public boolean isTrading() {
        synchronized (lock) {
            return state == State.TRADING;
        }
    }

    public boolean isConfirming() {
        synchronized (lock) {
            return state == State.CONFIRMING;
        }
    }

    public boolean isOpen() {
        synchronized (lock) {
            return state == State.TRADING || state == State.CONFIRMING;
        }
    }

    public void remember(UUID player, Location location) {
        if (player == null || location == null || location.getWorld() == null) {
            return;
        }
        Location copy = location.clone();
        synchronized (lock) {
            if (playerA.equals(player)) {
                placeA = copy;
            } else if (playerB.equals(player)) {
                placeB = copy;
            }
        }
    }

    public Location location(UUID player) {
        synchronized (lock) {
            Location stored = playerA.equals(player) ? placeA : playerB.equals(player) ? placeB : null;
            return stored == null ? null : stored.clone();
        }
    }

    public void distanceTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
        synchronized (lock) {
            this.distanceTask = task;
        }
    }

    public void attachView(UUID player, org.bukkit.inventory.Inventory inventory) {
        synchronized (lock) {
            views.put(player, inventory);
        }
    }

    public org.bukkit.inventory.Inventory view(UUID player) {
        synchronized (lock) {
            return views.get(player);
        }
    }

    public void suppressClose(UUID player) {
        synchronized (lock) {
            suppressClose.add(player);
        }
    }

    public boolean consumeSuppress(UUID player) {
        synchronized (lock) {
            return suppressClose.remove(player);
        }
    }

    public boolean toggleAccept(UUID player) {
        synchronized (lock) {
            if (state != State.TRADING || !involves(player)) {
                return false;
            }
            if (playerA.equals(player)) {
                acceptA = !acceptA;
            } else {
                acceptB = !acceptB;
            }
            if (acceptA && acceptB) {
                state = State.CONFIRMING;
                fitA = false;
                fitB = false;
                fitASet = false;
                fitBSet = false;
                fitContinued = false;
                abortRequested = false;
                return true;
            }
            return false;
        }
    }

    public boolean reportFit(boolean sideA, boolean fits) {
        synchronized (lock) {
            if (state != State.CONFIRMING || fitContinued) {
                return false;
            }
            if (sideA) {
                fitA = fits;
                fitASet = true;
            } else {
                fitB = fits;
                fitBSet = true;
            }
            if (fitASet && fitBSet) {
                fitContinued = true;
                return true;
            }
            return false;
        }
    }

    public boolean bothFit() {
        synchronized (lock) {
            return fitA && fitB;
        }
    }

    public boolean abortRequested() {
        synchronized (lock) {
            return abortRequested;
        }
    }

    public void requestAbort() {
        synchronized (lock) {
            abortRequested = true;
        }
    }

    public void abortConfirm() {
        synchronized (lock) {
            if (state != State.CONFIRMING) {
                return;
            }
            state = State.TRADING;
            acceptA = false;
            acceptB = false;
            abortRequested = false;
            fitContinued = false;
        }
    }

    public ItemBundle cancelOrDefer() {
        synchronized (lock) {
            if (state == State.DONE || state == State.CANCELLED) {
                return null;
            }
            if (state == State.CONFIRMING) {
                abortRequested = true;
                return null;
            }
            return drain(State.CANCELLED);
        }
    }

    public ItemBundle forceClose() {
        synchronized (lock) {
            if (state == State.DONE || state == State.CANCELLED) {
                return null;
            }
            return drain(State.CANCELLED);
        }
    }

    public Finish finish(List<MoneyExchange.Leg> legs) {
        synchronized (lock) {
            if (state != State.CONFIRMING) {
                return Finish.idle();
            }
            ExchangeResult result = MoneyExchange.exchangeAll(legs);
            if (!result.isCompleted()) {
                state = State.TRADING;
                acceptA = false;
                acceptB = false;
                abortRequested = false;
                fitContinued = false;
                return Finish.moneyFailed(result);
            }
            ItemBundle bundle = drain(State.DONE);
            return Finish.exchanged(result, bundle);
        }
    }

    public ViewModel model(UUID viewer) {
        synchronized (lock) {
            boolean sideA = playerA.equals(viewer);
            return new ViewModel(
                    cloneAll(sideA ? offerA : offerB),
                    cloneAll(sideA ? offerB : offerA),
                    sideA ? acceptA : acceptB,
                    sideA ? acceptB : acceptA,
                    copyMoney(sideA ? moneyA : moneyB),
                    copyMoney(sideA ? moneyB : moneyA),
                    sideA ? nameB : nameA
            );
        }
    }

    public List<ItemStack> copyItems(UUID player) {
        synchronized (lock) {
            return cloneList(slots(player));
        }
    }

    public Map<String, BigDecimal> copyMoney(UUID player) {
        synchronized (lock) {
            return copyMoney(money(player));
        }
    }

    public BigDecimal money(UUID player, String currencyId) {
        synchronized (lock) {
            return money(player).getOrDefault(currencyId, BigDecimal.ZERO);
        }
    }

    public boolean setMoney(UUID player, String currencyId, BigDecimal amount) {
        synchronized (lock) {
            if (state != State.TRADING || !money(player).containsKey(currencyId)) {
                return false;
            }
            BigDecimal safe = amount == null || amount.signum() < 0 ? BigDecimal.ZERO : amount;
            money(player).put(currencyId, safe);
            acceptA = false;
            acceptB = false;
            return true;
        }
    }

    public boolean addMoney(UUID player, String currencyId, BigDecimal delta) {
        synchronized (lock) {
            if (state != State.TRADING || delta == null || !money(player).containsKey(currencyId)) {
                return false;
            }
            BigDecimal next = money(player).getOrDefault(currencyId, BigDecimal.ZERO).add(delta);
            if (next.signum() < 0) {
                next = BigDecimal.ZERO;
            }
            BigDecimal cap = new BigDecimal("1000000000000");
            if (next.compareTo(cap) > 0) {
                next = cap;
            }
            money(player).put(currencyId, next);
            acceptA = false;
            acceptB = false;
            return true;
        }
    }

    public ItemStack peek(UUID player, int index) {
        synchronized (lock) {
            ItemStack[] slots = slots(player);
            if (index < 0 || index >= slots.length || slots[index] == null) {
                return null;
            }
            return slots[index].clone();
        }
    }

    public int addOffer(UUID player, ItemStack incoming) {
        synchronized (lock) {
            if (incoming == null || incoming.getType().isAir() || incoming.getAmount() <= 0) {
                return 0;
            }
            if (state != State.TRADING) {
                return incoming.getAmount();
            }
            int remaining = incoming.getAmount();
            ItemStack[] slots = slots(player);
            int max = Math.max(1, incoming.getMaxStackSize());
            for (int i = 0; i < slots.length && remaining > 0; i++) {
                ItemStack slot = slots[i];
                if (slot == null || slot.getType().isAir() || !slot.isSimilar(incoming)) {
                    continue;
                }
                int space = Math.min(max, Math.max(1, slot.getMaxStackSize())) - slot.getAmount();
                if (space <= 0) {
                    continue;
                }
                int move = Math.min(space, remaining);
                slot.setAmount(slot.getAmount() + move);
                remaining -= move;
            }
            for (int i = 0; i < slots.length && remaining > 0; i++) {
                if (slots[i] != null && !slots[i].getType().isAir()) {
                    continue;
                }
                int move = Math.min(max, remaining);
                ItemStack part = incoming.clone();
                part.setAmount(move);
                slots[i] = part;
                remaining -= move;
            }
            if (remaining != incoming.getAmount()) {
                acceptA = false;
                acceptB = false;
            }
            return remaining;
        }
    }

    public ItemStack takeSlot(UUID player, int index) {
        synchronized (lock) {
            if (state != State.TRADING) {
                return null;
            }
            ItemStack[] slots = slots(player);
            if (index < 0 || index >= slots.length) {
                return null;
            }
            ItemStack stack = slots[index];
            slots[index] = null;
            if (stack != null && !stack.getType().isAir()) {
                acceptA = false;
                acceptB = false;
            }
            return stack;
        }
    }

    public void restoreSlot(UUID player, int index, ItemStack stack) {
        synchronized (lock) {
            ItemStack[] slots = slots(player);
            if (index < 0 || index >= slots.length || stack == null || stack.getType().isAir()) {
                return;
            }
            if (slots[index] == null || slots[index].getType().isAir()) {
                slots[index] = stack;
                return;
            }
            addLoose(slots, stack);
        }
    }

    public CursorEdit applyCursor(UUID player, int index, ItemStack cursor, boolean rightClick) {
        synchronized (lock) {
            if (state != State.TRADING) {
                return CursorEdit.keep(cursor);
            }
            ItemStack[] slots = slots(player);
            if (index < 0 || index >= slots.length) {
                return CursorEdit.keep(cursor);
            }
            CursorEdit edit = rightClick
                    ? rightClick(slots, index, slots[index], cursor)
                    : leftClick(slots, index, slots[index], cursor);
            if (edit.changed()) {
                acceptA = false;
                acceptB = false;
            }
            return edit;
        }
    }

    public HotbarEdit swapHotbar(UUID player, int index, ItemStack hotbar) {
        synchronized (lock) {
            if (state != State.TRADING) {
                return HotbarEdit.rejected(hotbar);
            }
            ItemStack[] slots = slots(player);
            if (index < 0 || index >= slots.length) {
                return HotbarEdit.rejected(hotbar);
            }
            ItemStack offer = slots[index];
            slots[index] = isAir(hotbar) ? null : hotbar;
            acceptA = false;
            acceptB = false;
            return HotbarEdit.swapped(offer);
        }
    }

    private CursorEdit leftClick(ItemStack[] slots, int index, ItemStack slot, ItemStack cursor) {
        if (isAir(cursor) && isAir(slot)) {
            return CursorEdit.keep(null);
        }
        if (isAir(cursor)) {
            slots[index] = null;
            return CursorEdit.change(slot);
        }
        if (isAir(slot)) {
            return placeAll(slots, index, cursor);
        }
        if (cursor.isSimilar(slot)) {
            int max = Math.max(1, Math.min(cursor.getMaxStackSize(), slot.getMaxStackSize()));
            int space = max - slot.getAmount();
            if (space > 0) {
                int move = Math.min(space, cursor.getAmount());
                slot.setAmount(slot.getAmount() + move);
                cursor.setAmount(cursor.getAmount() - move);
                return CursorEdit.change(cursor.getAmount() <= 0 ? null : cursor);
            }
        }
        slots[index] = cursor;
        return CursorEdit.change(slot);
    }

    private CursorEdit rightClick(ItemStack[] slots, int index, ItemStack slot, ItemStack cursor) {
        if (isAir(cursor)) {
            if (isAir(slot)) {
                return CursorEdit.keep(null);
            }
            int amount = slot.getAmount();
            int taken = (amount + 1) / 2;
            int keep = amount - taken;
            ItemStack picked = slot.clone();
            picked.setAmount(taken);
            if (keep <= 0) {
                slots[index] = null;
            } else {
                slot.setAmount(keep);
            }
            return CursorEdit.change(picked);
        }
        if (isAir(slot) || cursor.isSimilar(slot)) {
            if (!isAir(slot)) {
                int max = Math.max(1, slot.getMaxStackSize());
                if (slot.getAmount() >= max) {
                    slots[index] = cursor;
                    return CursorEdit.change(slot);
                }
                slot.setAmount(slot.getAmount() + 1);
            } else {
                ItemStack one = cursor.clone();
                one.setAmount(1);
                slots[index] = one;
            }
            cursor.setAmount(cursor.getAmount() - 1);
            return CursorEdit.change(cursor.getAmount() <= 0 ? null : cursor);
        }
        slots[index] = cursor;
        return CursorEdit.change(slot);
    }

    private CursorEdit placeAll(ItemStack[] slots, int index, ItemStack cursor) {
        int max = Math.max(1, cursor.getMaxStackSize());
        if (cursor.getAmount() <= max) {
            slots[index] = cursor;
            return CursorEdit.change(null);
        }
        ItemStack part = cursor.clone();
        part.setAmount(max);
        slots[index] = part;
        cursor.setAmount(cursor.getAmount() - max);
        return CursorEdit.change(cursor);
    }

    private ItemBundle drain(State next) {
        state = next;
        stopDistance();
        return new ItemBundle(playerA, drain(offerA), playerB, drain(offerB));
    }

    private void stopDistance() {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask task = distanceTask;
        distanceTask = null;
        if (task != null) {
            try {
                task.cancel();
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static List<ItemStack> drain(ItemStack[] slots) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = slots[i];
            slots[i] = null;
            if (!isAir(stack)) {
                items.add(stack);
            }
        }
        return items;
    }

    private void addLoose(ItemStack[] slots, ItemStack stack) {
        int leftover = stack.getAmount();
        int max = Math.max(1, stack.getMaxStackSize());
        for (int i = 0; i < slots.length && leftover > 0; i++) {
            if (slots[i] != null && !slots[i].getType().isAir()) {
                continue;
            }
            int move = Math.min(max, leftover);
            ItemStack part = stack.clone();
            part.setAmount(move);
            slots[i] = part;
            leftover -= move;
        }
    }

    private ItemStack[] slots(UUID player) {
        return playerA.equals(player) ? offerA : offerB;
    }

    private Map<String, BigDecimal> money(UUID player) {
        return playerA.equals(player) ? moneyA : moneyB;
    }

    private static Map<String, BigDecimal> copyMoney(Map<String, BigDecimal> source) {
        return new LinkedHashMap<>(source);
    }

    private static ItemStack[] cloneAll(ItemStack[] slots) {
        ItemStack[] copy = new ItemStack[slots.length];
        for (int i = 0; i < slots.length; i++) {
            copy[i] = isAir(slots[i]) ? null : slots[i].clone();
        }
        return copy;
    }

    private static List<ItemStack> cloneList(ItemStack[] slots) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : slots) {
            if (!isAir(stack)) {
                items.add(stack.clone());
            }
        }
        return items;
    }

    private static boolean isAir(ItemStack stack) {
        return stack == null || stack.getType().isAir() || stack.getAmount() <= 0;
    }

    public record CursorEdit(ItemStack cursor, boolean changed) {
        static CursorEdit keep(ItemStack cursor) {
            return new CursorEdit(cursor, false);
        }

        static CursorEdit change(ItemStack cursor) {
            return new CursorEdit(cursor, true);
        }
    }

    public record HotbarEdit(ItemStack toHotbar, ItemStack restore) {
        static HotbarEdit swapped(ItemStack toHotbar) {
            return new HotbarEdit(toHotbar, null);
        }

        static HotbarEdit rejected(ItemStack restore) {
            return new HotbarEdit(null, restore);
        }
    }

    public record ItemBundle(UUID first, List<ItemStack> firstItems, UUID second, List<ItemStack> secondItems) {
        public List<ItemStack> forPlayer(UUID player) {
            if (first.equals(player)) {
                return firstItems;
            }
            if (second.equals(player)) {
                return secondItems;
            }
            return List.of();
        }
    }

    public record ViewModel(ItemStack[] own, ItemStack[] partner, boolean ownAccepted, boolean partnerAccepted,
                            Map<String, BigDecimal> ownMoney, Map<String, BigDecimal> partnerMoney, String partnerName) {
    }

    public record Finish(boolean skipped, boolean completed, ExchangeResult exchange, ItemBundle bundle) {
        static Finish idle() {
            return new Finish(true, false, null, null);
        }

        static Finish moneyFailed(ExchangeResult exchange) {
            return new Finish(false, false, exchange, null);
        }

        static Finish exchanged(ExchangeResult exchange, ItemBundle bundle) {
            return new Finish(false, true, exchange, bundle);
        }
    }
}
