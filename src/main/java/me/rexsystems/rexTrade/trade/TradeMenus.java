package me.rexsystems.rexTrade.trade;

import me.rexsystems.rexTrade.config.CurrencySpec;
import me.rexsystems.rexTrade.config.GuiSpec;
import me.rexsystems.rexTrade.config.Settings;
import me.rexsystems.rexTrade.util.CompactNumbers;
import me.rexsystems.rexTrade.util.TextParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TradeMenus {

    private TradeMenus() {
    }

    public static Inventory create(TradeSession session, UUID viewer, String partnerName) {
        GuiSpec gui = session.rules().gui();
        TradeHolder holder = new TradeHolder(session.id(), viewer);
        Component title = line(gui.title(), Map.of("partner", partnerName));
        Inventory inventory = Bukkit.createInventory(holder, gui.size(), title);
        holder.attach(inventory);
        return inventory;
    }

    public static Inventory preview(TradeSession session, UUID viewer, ItemStack source) {
        ItemStack[] contents = ContainerPreview.contents(source);
        PreviewHolder holder = new PreviewHolder(session.id(), viewer);
        String name = TradeText.one(source);
        Component title = line(session.rules().gui().previewTitle(), Map.of("item", name));
        int size = Math.max(9, (int) Math.ceil(contents.length / 9.0) * 9);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.attach(inventory);
        for (int i = 0; i < contents.length && i < size; i++) {
            inventory.setItem(i, contents[i]);
        }
        return inventory;
    }

    public static void render(TradeSession session, UUID viewer) {
        Inventory inventory = session.view(viewer);
        if (inventory == null) {
            return;
        }
        TradeSession.ViewModel model = session.model(viewer);
        Settings rules = session.rules();
        GuiSpec gui = rules.gui();
        ItemStack filler = named(material(gui.filler(), Material.GRAY_STAINED_GLASS_PANE), " ");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler.clone());
        }
        paintOffer(inventory, gui.ownSlots(), model.own());
        paintOffer(inventory, gui.partnerSlots(), model.partner());
        inventory.setItem(gui.ownAcceptSlot(), acceptButton(gui, model.ownAccepted(), model.partnerName()));
        inventory.setItem(gui.partnerAcceptSlot(), partnerButton(gui, model.partnerAccepted(), model.partnerName()));
        paintCurrencies(inventory, gui, rules, model.ownMoney(), model.partnerMoney(), model.partnerName(), true);
        paintCurrencies(inventory, gui, rules, model.partnerMoney(), model.ownMoney(), model.partnerName(), false);
    }

    private static void paintOffer(Inventory inventory, int[] slots, ItemStack[] items) {
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = i < items.length ? items[i] : null;
            inventory.setItem(slots[i], stack == null ? null : stack.clone());
        }
    }

    private static void paintCurrencies(Inventory inventory, GuiSpec gui, Settings rules, Map<String, BigDecimal> amounts,
                                        Map<String, BigDecimal> other, String partner, boolean own) {
        int[] slots = own ? gui.currencySlots() : gui.partnerCurrencySlots();
        List<CurrencySpec> currencies = rules.currencies();
        for (int i = 0; i < slots.length && i < currencies.size(); i++) {
            CurrencySpec currency = currencies.get(i);
            BigDecimal amount = amounts.getOrDefault(currency.id(), BigDecimal.ZERO);
            String shown = CompactNumbers.format(amount, rules.formatMode());
            if (own) {
                BigDecimal large = currency.increment().multiply(BigDecimal.TEN);
                Map<String, String> placeholders = Map.of(
                        "currency", currency.name(),
                        "amount", shown,
                        "increment", CompactNumbers.format(currency.increment(), rules.formatMode()),
                        "large", CompactNumbers.format(large, rules.formatMode()),
                        "id", currency.id(),
                        "partner", partner
                );
                ItemStack button = named(material(currency.material(), Material.GOLD_NUGGET),
                        apply(gui.currencyName(), placeholders));
                ItemMeta meta = button.getItemMeta();
                List<Component> lore = new ArrayList<>();
                for (String line : gui.currencyLore()) {
                    lore.add(line(line, placeholders));
                }
                meta.lore(lore);
                button.setItemMeta(meta);
                inventory.setItem(slots[i], button);
            } else {
                inventory.setItem(slots[i], named(material(currency.material(), Material.GOLD_NUGGET),
                        apply(gui.partnerCurrencyName(), Map.of(
                                "partner", partner,
                                "amount", shown,
                                "currency", currency.name()
                        ))));
            }
        }
    }

    private static ItemStack acceptButton(GuiSpec gui, boolean accepted, String partner) {
        String template = accepted ? gui.acceptedName() : gui.acceptName();
        Material material = material(accepted ? gui.acceptMaterial() : gui.waitingMaterial(),
                accepted ? Material.LIME_DYE : Material.RED_DYE);
        return named(material, apply(template, Map.of("partner", partner)));
    }

    private static ItemStack partnerButton(GuiSpec gui, boolean accepted, String partner) {
        String template = accepted ? gui.partnerAcceptedName() : gui.partnerWaitingName();
        Material material = material(accepted ? gui.acceptMaterial() : gui.waitingMaterial(),
                accepted ? Material.LIME_DYE : Material.RED_DYE);
        return named(material, apply(template, Map.of("partner", partner)));
    }

    private static ItemStack named(Material material, String text) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(line(text, Map.of()));
        stack.setItemMeta(meta);
        return stack;
    }

    private static String apply(String template, Map<String, String> placeholders) {
        return TextParser.apply(template, placeholders);
    }

    private static Component line(String template, Map<String, String> placeholders) {
        return TextParser.parse("<!italic>" + TextParser.apply(template, placeholders));
    }

    private static Material material(String name, Material fallback) {
        if (name == null) {
            return fallback;
        }
        Material material = Material.matchMaterial(name.trim());
        return material == null ? fallback : material;
    }

    public static final class TradeHolder implements InventoryHolder {
        private final UUID sessionId;
        private final UUID viewerId;
        private Inventory inventory;

        public TradeHolder(UUID sessionId, UUID viewerId) {
            this.sessionId = sessionId;
            this.viewerId = viewerId;
        }

        public void attach(Inventory inventory) {
            this.inventory = inventory;
        }

        public UUID sessionId() {
            return sessionId;
        }

        public UUID viewerId() {
            return viewerId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public static final class PreviewHolder implements InventoryHolder {
        private final UUID sessionId;
        private final UUID viewerId;
        private Inventory inventory;

        public PreviewHolder(UUID sessionId, UUID viewerId) {
            this.sessionId = sessionId;
            this.viewerId = viewerId;
        }

        public void attach(Inventory inventory) {
            this.inventory = inventory;
        }

        public UUID sessionId() {
            return sessionId;
        }

        public UUID viewerId() {
            return viewerId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
