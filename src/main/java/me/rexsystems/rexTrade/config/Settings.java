package me.rexsystems.rexTrade.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Settings {

    private final boolean enabled;
    private final int requestTimeoutSeconds;
    private final double maxDistance;
    private final BigDecimal taxPercent;
    private final String numberFormat;
    private final List<String> blockedWorlds;
    private final List<String> blockedGamemodes;
    private final GuiSpec gui;
    private final List<CurrencySpec> currencies;
    private final Map<String, String> messages;
    private final List<String> help;
    private final List<String> summary;
    private final Map<String, SoundSpec> sounds;

    public Settings(boolean enabled, int requestTimeoutSeconds, double maxDistance, BigDecimal taxPercent,
                    String numberFormat, List<String> blockedWorlds, List<String> blockedGamemodes, GuiSpec gui,
                    List<CurrencySpec> currencies, Map<String, String> messages, List<String> help,
                    List<String> summary, Map<String, SoundSpec> sounds) {
        this.enabled = enabled;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        this.maxDistance = maxDistance;
        this.taxPercent = taxPercent;
        this.numberFormat = numberFormat;
        this.blockedWorlds = List.copyOf(blockedWorlds);
        this.blockedGamemodes = List.copyOf(blockedGamemodes);
        this.gui = gui;
        this.currencies = List.copyOf(currencies);
        this.messages = Map.copyOf(messages);
        this.help = List.copyOf(help);
        this.summary = List.copyOf(summary);
        this.sounds = Map.copyOf(sounds);
    }

    public boolean enabled() {
        return enabled;
    }

    public int requestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public double maxDistance() {
        return maxDistance;
    }

    public BigDecimal taxPercent() {
        return taxPercent;
    }

    public String numberFormat() {
        return numberFormat;
    }

    public GuiSpec gui() {
        return gui;
    }

    public List<CurrencySpec> currencies() {
        return currencies;
    }

    public CurrencySpec currency(String id) {
        if (id == null) {
            return null;
        }
        for (CurrencySpec currency : currencies) {
            if (currency.id().equalsIgnoreCase(id)) {
                return currency;
            }
        }
        return null;
    }

    public boolean worldBlocked(String world) {
        return matches(blockedWorlds, world);
    }

    public boolean gamemodeBlocked(String gamemode) {
        return matches(blockedGamemodes, gamemode);
    }

    public String message(String path) {
        return messages.getOrDefault(path, "");
    }

    public String prefix() {
        return message("prefix");
    }

    public String prefixed(String path) {
        return message(path).replace("<prefix>", prefix()).replace("{prefix}", prefix());
    }

    public List<String> help() {
        return help;
    }

    public List<String> summary() {
        return summary;
    }

    public SoundSpec sound(String name) {
        return sounds.get(name);
    }

    private static boolean matches(List<String> values, String candidate) {
        if (candidate == null || values.isEmpty()) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    public String formatMode() {
        return "full".equalsIgnoreCase(numberFormat) ? "full" : "short";
    }

    public String blockedWorldLabel(String world) {
        return world == null ? "" : world.toLowerCase(Locale.ROOT);
    }
}
