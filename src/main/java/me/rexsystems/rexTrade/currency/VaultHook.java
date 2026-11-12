package me.rexsystems.rexTrade.currency;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultHook {

    private VaultHook() {
    }

    public static Economy economy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            return null;
        }
        return registration.getProvider();
    }

    public static boolean withdraw(OfflinePlayer player, double amount) {
        Economy economy = economy();
        if (economy == null || player == null || amount <= 0) {
            return amount <= 0;
        }
        if (!economy.has(player, amount)) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }

    public static void deposit(OfflinePlayer player, double amount) {
        Economy economy = economy();
        if (economy == null || player == null || amount <= 0) {
            return;
        }
        economy.depositPlayer(player, amount);
    }
}
