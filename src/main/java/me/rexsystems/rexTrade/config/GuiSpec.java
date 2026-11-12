package me.rexsystems.rexTrade.config;

import java.util.List;

public record GuiSpec(
        int size,
        String title,
        int[] ownSlots,
        int[] partnerSlots,
        int ownAcceptSlot,
        int partnerAcceptSlot,
        int[] currencySlots,
        int[] partnerCurrencySlots,
        int[] dividerSlots,
        String filler,
        String acceptMaterial,
        String waitingMaterial,
        String acceptName,
        String acceptedName,
        String partnerAcceptedName,
        String partnerWaitingName,
        String currencyName,
        List<String> currencyLore,
        String partnerCurrencyName,
        String previewTitle
) {
    public static GuiSpec defaults() {
        return new GuiSpec(
                54,
                "<dark_gray>Trade with <white>{partner}",
                new int[]{0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30},
                new int[]{5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35},
                45,
                53,
                new int[]{46, 47, 48},
                new int[]{50, 51, 52},
                new int[]{4, 13, 22, 31, 40, 49},
                "GRAY_STAINED_GLASS_PANE",
                "LIME_DYE",
                "RED_DYE",
                "<green>Accept",
                "<green>Accepted",
                "<green>{partner} accepted",
                "<gray>Waiting for {partner}",
                "<gold>{currency}",
                List.of(
                        "<gray>Offer: <white>{amount}",
                        "<gray>Left click <white>+{increment}",
                        "<gray>Right click <white>-{increment}",
                        "<gray>Shift-left click <white>+{large}",
                        "<gray>Shift-right click clears the offer"
                ),
                "<gray>{partner}<dark_gray>: <white>{amount}",
                "<dark_gray>{item}"
        );
    }
}
