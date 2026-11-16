package dev.maire.nourished.client.micro;

import dev.maire.nourished.client.edit.BoxContentStyle;
import dev.maire.nourished.config.MicroClientConfig;
import dev.maire.nourished.core.history.IntakeHistory;
import dev.maire.nourished.core.micro.MicroAttachment;
import dev.maire.nourished.core.micro.MicroDef;
import dev.maire.nourished.core.micro.MicroProfileResolver;
import dev.maire.nourished.core.micro.MicroRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The nutrient page shared by the Nutritional Panel's Nutrients tab and the standalone screen: an
 * Overview tab with every nutrient, then one tab per nutrient with level, trend, recent sources and
 * best foods. Lays itself out to whatever width it's given.
 */
public final class NutrientDetailView {

    public static final int TAB_H = 14;
    private static final int ICON_STEP = 34;
    private static final int ROW_H = 20;
    // fixed so resizing the box clips instead of squeezing the bars
    private static final int BAR_W = 120;
    static final int TEXT = 0xFFE6E6E6;
    static final int MUTED = 0xFF9AA4AE;
    private static final int TRACK = 0xFF2A3038;

    private List<MicroDef> defs = List.of();
    /** 0 = Overview, i + 1 = defs.get(i). */
    private int selected;
    private final List<int[]> tabs = new ArrayList<>();
    private final List<int[]> rows = new ArrayList<>();
    private int[] hudButton;
    private ItemStack hovered = ItemStack.EMPTY;
    private BoxContentStyle st = BoxContentStyle.NONE;

    public NutrientDetailView(int selected) {
        this.selected = Math.max(0, selected);
    }

    /** Draws at (x, y) within width {@code w}; returns the height used. */
    public int render(GuiGraphics g, Font font, int x, int y, int w, int mouseX, int mouseY) {
        return render(g, font, x, y, w, mouseX, mouseY, BoxContentStyle.NONE);
    }

    /** Same, honoring the box's Move Text/Icons/Bars and Hide settings (tabs and buttons stay put). */
    public int render(GuiGraphics g, Font font, int x, int y, int w, int mouseX, int mouseY, BoxContentStyle style) {
        st = style;
        defs = List.copyOf(MicroRegistry.defs(true));
        selected = Math.min(selected, defs.size());
        hovered = ItemStack.EMPTY;
        rows.clear();
        hudButton = null;
        var player = Minecraft.getInstance().player;
        if (defs.isEmpty() || player == null) {
            g.drawString(font, Component.translatable("nourished.screen.nutrients.none"), x, y, MUTED);
            return 12;
        }
        Map<String, Float> levels = MicroAttachment.get(player);
        int top = y;
        y = drawTabs(g, font, x, y, w) + 6;
        if (selected == 0) {
            y = drawOverview(g, font, levels, x, y, w, mouseX, mouseY);
        } else {
            y = drawDetail(g, font, defs.get(selected - 1), levels, x, y, w, mouseX, mouseY);
        }
        return y - top;
    }

    /** The food under the mouse during the last render, for a caller that can draw tooltips. */
    public ItemStack hovered() {
        return hovered;
    }

    private int drawTabs(GuiGraphics g, Font font, int x0, int y0, int w) {
        tabs.clear();
        int x = x0;
        int y = y0;
        for (int i = 0; i <= defs.size(); i++) {
            boolean fixed = i == 0;
            String label = i == 0 ? Component.translatable("nourished.screen.nutrients.overview").getString()
                    : defs.get(i - 1).displayName();
            int color = fixed ? 0xFFE0C060 : 0xFF000000 | defs.get(i - 1).color();
            int tw = font.width(label) + 8;
            if (x + tw > x0 + w && x > x0) {
                x = x0;
                y += TAB_H + 2;
            }
            boolean sel = i == selected;
            g.fill(x, y, x + tw, y + TAB_H, sel ? 0xFF2E3640 : 0xFF1A1F25);
            if (sel) g.fill(x, y + TAB_H - 1, x + tw, y + TAB_H, color);
            g.drawString(font, label, x + 4, y + 3, sel ? color : MUTED);
            tabs.add(new int[]{x, y, tw});
            x += tw + 2;
        }
        return y + TAB_H;
    }

    private int drawOverview(GuiGraphics g, Font font, Map<String, Float> levels, int x, int y, int w,
                             int mouseX, int mouseY) {
        int barX = x + 110;
        int barW = BAR_W;
        for (MicroDef def : defs) {
            float level = clamp(levels.getOrDefault(def.key(), 0f));
            int color = 0xFF000000 | def.color();
            boolean over = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ROW_H - 2;
            if (over) g.fill(x - 2, y - 1, x + w, y + ROW_H - 3, 0x30FFFFFF);
            st.item(g, icon(def), x, y);
            st.text(g, font, def.displayName(), x + 20, y + 4, color);
            bar(g, def, level, barX, y + 4, barW, color);
            st.text(g, font, Component.literal(Math.round(level * 100) + "% ").append(status(def, level)),
                    barX + barW + 6, y + 4, TEXT);
            rows.add(new int[]{x, y, w, ROW_H - 2});
            y += ROW_H;
        }
        y += 4;
        boolean hud = MicroClientConfig.get() != null && MicroClientConfig.get().hudEnabled();
        Component label = Component.translatable(hud ? "nourished.screen.nutrients.hud_on" : "nourished.screen.nutrients.hud_off");
        int bw = font.width(label) + 10;
        boolean over = mouseX >= x && mouseX < x + bw && mouseY >= y && mouseY < y + TAB_H;
        g.fill(x, y, x + bw, y + TAB_H, over ? 0xFF3A4450 : 0xFF1A1F25);
        g.drawString(font, label, x + 5, y + 3, hud ? 0xFF70D070 : MUTED);
        hudButton = new int[]{x, y, bw, TAB_H};
        y += TAB_H + 4;
        st.text(g, font, Component.translatable("nourished.screen.nutrients.overview_hint"), x, y, MUTED);
        return y + 10;
    }

    private int drawDetail(GuiGraphics g, Font font, MicroDef def, Map<String, Float> levels, int x, int y, int w,
                           int mouseX, int mouseY) {
        int color = 0xFF000000 | def.color();
        float level = clamp(levels.getOrDefault(def.key(), 0f));
        st.item(g, icon(def), x, y);
        st.text(g, font, def.displayName(), x + 22, y, color);
        st.text(g, font, Component.literal(Math.round(level * 100) + "%  ").append(status(def, level)), x + 22, y + 10, TEXT);
        int barX = x + 124;
        bar(g, def, level, barX, y + 3, BAR_W, color);

        y += 26;
        List<Float> samples = MicroClientHistory.samples(def.key());
        int gw = Math.min(160, w - 70);
        int gh = 28;
        st.fill(g, x, y, x + gw, y + gh, TRACK);
        if (samples.size() >= 2) {
            float step = (float) gw / (samples.size() - 1);
            for (int i = 0; i < samples.size(); i++) {
                int px = x + Math.round(i * step);
                int py = y + gh - 1 - Math.round((gh - 2) * clamp(samples.get(i)));
                st.fill(g, px, py, Math.min(px + 2, x + gw), py + 2, color);
            }
        }
        st.text(g, font, trend(samples), x + gw + 8, y + 10, TEXT);

        y += 34;
        st.text(g, font, Component.translatable("nourished.screen.nutrients.history"), x, y, MUTED);
        List<Float> days = IntakeHistoryChart.days(IntakeHistory.microId(def.key()));
        if (days.isEmpty()) {
            st.text(g, font, Component.translatable("nourished.screen.nutrients.history.none"), x, y + 12, MUTED);
        } else {
            IntakeHistoryChart.draw(g, font, days, x, y + 10, color, true, st);
        }

        y += 38;
        st.text(g, font, Component.translatable("nourished.screen.nutrients.recent"), x, y, MUTED);
        List<MicroClientHistory.Meal> meals = MicroClientHistory.mealsWith(def.key());
        int perRow = Math.max(1, w / ICON_STEP);
        int fx = x;
        for (int i = 0; i < Math.min(meals.size(), perRow); i++) {
            MicroClientHistory.Meal meal = meals.get(i);
            food(g, font, meal.stack(), gain(meal.stack(), meal.profile().getOrDefault(def.key(), 0f)), fx, y + 11, mouseX, mouseY);
            fx += ICON_STEP;
        }
        if (meals.isEmpty()) {
            st.text(g, font, Component.translatable("nourished.screen.nutrients.nothing_yet"), x, y + 14, MUTED);
        }

        y += 42;
        st.text(g, font, Component.translatable("nourished.screen.nutrients.best"), x, y, MUTED);
        fx = x;
        List<MicroTopFoods.Entry> best = MicroTopFoods.top(def.key());
        for (int i = 0; i < Math.min(best.size(), perRow); i++) {
            food(g, font, best.get(i).stack(), best.get(i).amount(), fx, y + 11, mouseX, mouseY);
            fx += ICON_STEP;
        }
        y += 40;
        if (!hovered.isEmpty()) {
            st.text(g, font, hovered.getHoverName(), x, y, TEXT);
        }
        return y + 10;
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        for (int i = 0; i < tabs.size(); i++) {
            if (inside(tabs.get(i), mouseX, mouseY, TAB_H)) {
                selected = i;
                return true;
            }
        }
        if (hudButton != null && inside(hudButton, mouseX, mouseY, hudButton[3])) {
            if (MicroClientConfig.get() != null) MicroClientConfig.get().toggleHud();
            return true;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (inside(rows.get(i), mouseX, mouseY, rows.get(i)[3])) {
                selected = i + 1;
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(int keyCode) {
        int count = defs.size() + 1;
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_TAB) {
            selected = (selected + 1) % count;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            selected = Math.floorMod(selected - 1, count);
            return true;
        }
        return false;
    }

    private static boolean inside(int[] r, double mx, double my, int h) {
        return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + h;
    }

    private void food(GuiGraphics g, Font font, ItemStack stack, float gain, int x, int y, int mouseX, int mouseY) {
        st.item(g, stack, x, y);
        int ix = st.iconX(x);
        int iy = st.iconY(y);
        if (mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 16) hovered = stack;
        // a pinned item with none of this nutrient shows without a number
        if (gain <= 0f) return;
        g.pose().pushPose();
        g.pose().translate(ix, iy + 18, 0);
        g.pose().scale(0.75f, 0.75f, 1f);
        g.drawString(font, "+" + Math.max(1, Math.round(gain * 100)) + "%", 0, 0, TEXT);
        g.pose().popPose();
    }

    private void bar(GuiGraphics g, MicroDef def, float level, int x, int y, int w, int color) {
        st.fill(g, x, y, x + w, y + 8, TRACK);
        st.fill(g, x, y, x + Math.round(w * level), y + 8, color);
        int lo = x + Math.round(w * def.lowThreshold());
        int hi = x + Math.round(w * def.highThreshold());
        st.fill(g, lo, y - 2, lo + 1, y + 10, 0xAAFFFFFF);
        st.fill(g, hi, y - 2, hi + 1, y + 10, 0xAAFFFFFF);
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static Component trend(List<Float> samples) {
        if (samples.size() < 2) return Component.translatable("nourished.screen.nutrients.trend.steady");
        float last = samples.get(samples.size() - 1);
        float before = samples.get(Math.max(0, samples.size() - 6));
        if (last - before > 0.02f) return Component.translatable("nourished.screen.nutrients.trend.rising");
        if (before - last > 0.02f) return Component.translatable("nourished.screen.nutrients.trend.falling");
        return Component.translatable("nourished.screen.nutrients.trend.steady");
    }

    private static Component status(MicroDef def, float level) {
        if (level < def.lowThreshold()) {
            return Component.translatable("nourished.screen.nutrients.status.low").withColor(0xE05050);
        }
        if (level > def.highThreshold()) {
            return Component.translatable("nourished.screen.nutrients.status.high").withColor(0x60C0F0);
        }
        return Component.translatable("nourished.screen.nutrients.status.ok").withColor(0x70D070);
    }

    private static float gain(ItemStack stack, float value) {
        FoodProperties food = stack.getFoodProperties(null);
        return food == null ? 0f : value * MicroProfileResolver.biteGain(food);
    }

    private static ItemStack icon(MicroDef def) {
        ResourceLocation id = ResourceLocation.tryParse(def.icon());
        Item item = id != null ? BuiltInRegistries.ITEM.get(id) : null;
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }
}
