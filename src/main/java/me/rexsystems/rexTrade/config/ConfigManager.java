package me.rexsystems.rexTrade.config;

import me.rexsystems.rexTrade.RexTrade;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConfigManager {

    private final RexTrade plugin;
    private Settings settings = fallback();

    public ConfigManager(RexTrade plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        new ConfigAutoUpdater(plugin).update();
        plugin.reloadConfig();
        this.settings = read(plugin.getConfig());
    }

    public void reload() {
        load();
    }

    public Settings settings() {
        return settings;
    }

    private Settings read(FileConfiguration config) {
        GuiSpec gui = readGui(config);
        List<CurrencySpec> currencies = readCurrencies(config);
        if (currencies.isEmpty()) {
            currencies = fallback().currencies();
        }
        Map<String, String> messages = new HashMap<>();
        ConfigurationSection messageSection = config.getConfigurationSection("messages");
        if (messageSection != null) {
            for (String key : messageSection.getKeys(false)) {
                if (messageSection.isString(key)) {
                    messages.put(key, messageSection.getString(key, ""));
                }
            }
        }
        if (!messages.containsKey("prefix")) {
            messages.put("prefix", "<gray>[<gold>Trade</gold>]</gray> ");
        }
        List<String> help = config.getStringList("messages.help");
        List<String> summary = config.getStringList("messages.summary");
        String format = config.getString("settings.number-format", "short");
        if (format == null || !format.equalsIgnoreCase("full")) {
            format = "short";
        } else {
            format = "full";
        }
        return new Settings(
                config.getBoolean("settings.enabled", true),
                Math.max(1, config.getInt("settings.request-timeout-seconds", 30)),
                Math.max(0D, config.getDouble("settings.max-distance", 10D)),
                decimal(config, "settings.tax-percent", "0"),
                format,
                config.getStringList("settings.blocked-worlds"),
                config.getStringList("settings.blocked-gamemodes"),
                gui,
                currencies,
                messages,
                help == null ? List.of() : help,
                summary == null ? List.of() : summary,
                readSounds(config)
        );
    }

    private GuiSpec readGui(FileConfiguration config) {
        GuiSpec defaults = GuiSpec.defaults();
        int rows = config.getInt("gui.rows", 6);
        int size = Math.max(1, Math.min(6, rows)) * 9;
        int[] own = ints(config.getIntegerList("gui.own-slots"), defaults.ownSlots());
        int[] partner = ints(config.getIntegerList("gui.partner-slots"), defaults.partnerSlots());
        int[] currency = ints(config.getIntegerList("gui.currency-slots"), defaults.currencySlots());
        int[] partnerCurrency = ints(config.getIntegerList("gui.partner-currency-slots"), defaults.partnerCurrencySlots());
        int[] divider = ints(config.getIntegerList("gui.divider-slots"), defaults.dividerSlots());
        int ownAccept = config.getInt("gui.own-accept-slot", defaults.ownAcceptSlot());
        int partnerAccept = config.getInt("gui.partner-accept-slot", defaults.partnerAcceptSlot());
        if (!valid(size, own, partner, currency, partnerCurrency, divider, ownAccept, partnerAccept)) {
            plugin.getLogger().warning("GUI slot layout is invalid. Using the default layout.");
            return defaults;
        }
        List<String> lore = config.getStringList("gui.currency-lore");
        if (lore == null || lore.isEmpty()) {
            lore = defaults.currencyLore();
        }
        return new GuiSpec(
                size,
                text(config, "gui.title", defaults.title()),
                own,
                partner,
                ownAccept,
                partnerAccept,
                currency,
                partnerCurrency,
                divider,
                text(config, "gui.filler", defaults.filler()),
                text(config, "gui.accept-material", defaults.acceptMaterial()),
                text(config, "gui.waiting-material", defaults.waitingMaterial()),
                text(config, "gui.accept-name", defaults.acceptName()),
                text(config, "gui.accepted-name", defaults.acceptedName()),
                text(config, "gui.partner-accepted-name", defaults.partnerAcceptedName()),
                text(config, "gui.partner-waiting-name", defaults.partnerWaitingName()),
                text(config, "gui.currency-name", defaults.currencyName()),
                List.copyOf(lore),
                text(config, "gui.partner-currency-name", defaults.partnerCurrencyName()),
                text(config, "gui.preview-title", defaults.previewTitle())
        );
    }

    private List<CurrencySpec> readCurrencies(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("currencies");
        List<CurrencySpec> currencies = new ArrayList<>();
        if (section == null) {
            return currencies;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection child = section.getConfigurationSection(id);
            if (child == null || id == null || id.isBlank()) {
                continue;
            }
            String type = child.getString("type", "vault");
            BigDecimal increment = decimal(child, "increment", "1");
            if (increment.signum() <= 0) {
                increment = BigDecimal.ONE;
            }
            currencies.add(new CurrencySpec(
                    id.toLowerCase(Locale.ROOT),
                    type == null ? "vault" : type.toLowerCase(Locale.ROOT),
                    child.getString("name", id),
                    child.getString("material", "GOLD_NUGGET"),
                    increment,
                    child.getString("balance-placeholder", ""),
                    child.getString("give-command", ""),
                    child.getString("take-command", "")
            ));
        }
        return currencies;
    }

    private Map<String, SoundSpec> readSounds(FileConfiguration config) {
        Map<String, SoundSpec> sounds = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("sounds");
        if (section == null) {
            return sounds;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child == null) {
                continue;
            }
            sounds.put(key, new SoundSpec(
                    child.getBoolean("enabled", false),
                    child.getString("sound", "UI_BUTTON_CLICK"),
                    (float) child.getDouble("volume", 1.0),
                    (float) child.getDouble("pitch", 1.0)
            ));
        }
        return sounds;
    }

    private static boolean valid(int size, int[] own, int[] partner, int[] currency, int[] partnerCurrency,
                                 int[] divider, int ownAccept, int partnerAccept) {
        if (own.length == 0 || own.length != partner.length) {
            return false;
        }
        boolean[] used = new boolean[size];
        int[][] groups = {own, partner, currency, partnerCurrency, divider, new int[]{ownAccept, partnerAccept}};
        for (int[] group : groups) {
            for (int slot : group) {
                if (slot < 0 || slot >= size || used[slot]) {
                    return false;
                }
                used[slot] = true;
            }
        }
        return true;
    }

    private static int[] ints(List<Integer> values, int[] fallback) {
        if (values == null || values.isEmpty()) {
            return fallback.clone();
        }
        int[] parsed = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Integer value = values.get(i);
            parsed[i] = value == null ? -1 : value;
        }
        return parsed;
    }

    private static String text(FileConfiguration config, String path, String fallback) {
        String value = config.getString(path);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static BigDecimal decimal(FileConfiguration config, String path, String fallback) {
        Object raw = config.get(path);
        return parseDecimal(raw, fallback);
    }

    private static BigDecimal decimal(ConfigurationSection section, String path, String fallback) {
        return parseDecimal(section.get(path), fallback);
    }

    private static BigDecimal parseDecimal(Object raw, String fallback) {
        if (raw == null) {
            return new BigDecimal(fallback);
        }
        try {
            return new BigDecimal(raw.toString());
        } catch (NumberFormatException ex) {
            return new BigDecimal(fallback);
        }
    }

    private static Settings fallback() {
        return new Settings(
                true,
                30,
                10D,
                BigDecimal.ZERO,
                "short",
                List.of(),
                List.of(),
                GuiSpec.defaults(),
                List.of(new CurrencySpec("money", "vault", "Money", "GOLD_INGOT", new BigDecimal("100"), "", "", "")),
                Map.of("prefix", "<gray>[<gold>Trade</gold>]</gray> "),
                List.of(),
                List.of(),
                Map.of()
        );
    }
}
