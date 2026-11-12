package me.rexsystems.rexTrade;

import me.rexsystems.rexTrade.command.TradeCommand;
import me.rexsystems.rexTrade.config.ConfigManager;
import me.rexsystems.rexTrade.trade.TradeListener;
import me.rexsystems.rexTrade.trade.TradeManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class RexTrade extends JavaPlugin {

    private ConfigManager configManager;
    private TradeManager trades;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.load();
        this.trades = new TradeManager(this);

        getServer().getPluginManager().registerEvents(new TradeListener(this), this);

        TradeCommand command = new TradeCommand(this);
        PluginCommand pluginCommand = getCommand("rextrade");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getLogger().info("RexTrade enabled.");
    }

    @Override
    public void onDisable() {
        if (trades != null) {
            trades.shutdown();
        }
        getLogger().info("RexTrade disabled.");
    }

    public void reloadPlugin() {
        configManager.reload();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TradeManager trades() {
        return trades;
    }
}
