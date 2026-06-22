package dev.maire.nourished.client.config.categories.widgets.compat;

import dev.maire.nourished.config.NourishedLockRegistry;
import net.minecraft.client.gui.Font;

import java.util.function.Supplier;

import static dev.maire.nourished.client.config.NourishedConfigSharedWidgets.isMultiplayer;

final class CompatTabUi {
    private CompatTabUi() {}

    static String ellipsize(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        if (maxWidth <= ellipsisWidth) {
            return font.plainSubstrByWidth(text, maxWidth);
        }
        return font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + ellipsis;
    }

    static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : input.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            } else {
                result.append(' ');
                capitalizeNext = true;
            }
        }
        return result.toString().trim();
    }

    static boolean isToggleEditable(String modid, boolean code, Supplier<Boolean> editableSupplier) {
        String key = "compat." + modid + "." + (code ? "enableCodeCompat" : "enableTagCompat");
        if (NourishedLockRegistry.isLocked(key)) {
            return false;
        }
        return !(NourishedLockRegistry.isServerOnly(key) && isMultiplayer()) && editableSupplier.get();
    }
}
