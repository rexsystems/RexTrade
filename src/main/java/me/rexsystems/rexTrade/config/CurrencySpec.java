package me.rexsystems.rexTrade.config;

import java.math.BigDecimal;
import java.util.List;

public record CurrencySpec(
        String id,
        String type,
        String name,
        String material,
        BigDecimal increment,
        String balancePlaceholder,
        String giveCommand,
        String takeCommand
) {
    public boolean placeholder() {
        return "placeholder".equalsIgnoreCase(type);
    }
}
