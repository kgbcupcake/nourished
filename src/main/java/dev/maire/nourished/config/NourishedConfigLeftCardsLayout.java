package dev.maire.nourished.config;

import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.ClothConfigTabButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Dedicated layout controller for the config navigator.
 *
 * Keeps the existing Cloth categories/entries intact and only repositions
 * category tab widgets into a left-side card/box column.
 */
final class NourishedConfigLeftCardsLayout {
    private static final int NAV_LEFT = 10;
    private static final int NAV_TOP = 30;
    private static final int NAV_WIDTH = 108;
    private static final int NAV_ITEM_H = 18;
    private static final int NAV_GAP = 2;
    private static final int CONTENT_GAP = 8;
    private static final int CONTENT_RIGHT_PAD = 12;

    private NourishedConfigLeftCardsLayout() {}

    static void apply(Screen screen) {
        if (!(screen instanceof ClothConfigScreen cloth)) {
            return;
        }
        layoutLeftCards(cloth);
    }

    private static void layoutLeftCards(ClothConfigScreen cloth) {
        List<ClothConfigTabButton> tabs = getField(cloth, "tabButtons");
        if (tabs == null || tabs.isEmpty()) {
            return;
        }

        int y = NAV_TOP;
        for (ClothConfigTabButton tab : tabs) {
            if (tab == null) continue;
            tab.setX(NAV_LEFT);
            tab.setY(y);
            tab.setWidth(NAV_WIDTH);
            y += NAV_ITEM_H + NAV_GAP;
        }

        AbstractWidget left = getField(cloth, "buttonLeftTab");
        AbstractWidget right = getField(cloth, "buttonRightTab");
        hideWidget(left);
        hideWidget(right);

        if (cloth.listWidget != null) {
            int contentLeft = NAV_LEFT + NAV_WIDTH + CONTENT_GAP;
            int contentWidth = Math.max(120, cloth.width - contentLeft - CONTENT_RIGHT_PAD);
            cloth.listWidget.updateSize(contentWidth, cloth.height, cloth.listWidget.top, cloth.listWidget.bottom);
            cloth.listWidget.setLeftPos(contentLeft);
        }
    }

    private static void hideWidget(AbstractWidget widget) {
        if (widget == null) return;
        widget.visible = false;
        widget.active = false;
        widget.setX(-2000);
        widget.setY(-2000);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
