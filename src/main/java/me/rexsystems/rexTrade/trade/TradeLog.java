package me.rexsystems.rexTrade.trade;

import me.rexsystems.rexTrade.RexTrade;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class TradeLog {

    private final RexTrade plugin;
    private final Object lock = new Object();

    public TradeLog(RexTrade plugin) {
        this.plugin = plugin;
    }

    public void append(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create the RexTrade folder for trades.log.");
                return;
            }
            Path path = plugin.getDataFolder().toPath().resolve("trades.log");
            synchronized (lock) {
                Files.writeString(path, line + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not write trades.log: " + ex.getMessage());
        }
    }
}
