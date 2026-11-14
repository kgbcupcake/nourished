package dev.maire.nourished.client.micro;

import dev.maire.nourished.core.micro.MicroAttachment;
import dev.maire.nourished.core.micro.MicroDef;
import dev.maire.nourished.core.micro.MicroProfileResolver;
import dev.maire.nourished.core.micro.MicroRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** One nutrient at a time: level, trend, what you've been eating for it, and the best foods for it. */
public final class NutrientDetailScreen extends Screen {

    private static final int W = 300;
    private static final int H = 206;
    private static final int TAB_H = 14;
    private static final int ICON = 18;
    private static final int BG = 0xE0101418;
    private static final int BORDER = 0xFF3A4450;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int MUTED = 0xFF9AA4AE;
    private static final int TRACK = 0xFF2A3038;

    @Nullable
    private final Screen parent;
    private final List<MicroDef> defs;
    private int selected;
    private int left;
    private int top;

    public NutrientDetailScreen(@Nullable Screen parent, int selected) {
        super(Component.translatable("nourished.screen.nutrients.title"));
        this.parent = parent;
        this.defs = List.copyOf(MicroRegistry.defs(true));
        this.selected = defs.isEmpty() ? 0 : Math.floorMod(selected, defs.size());
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, BG);
        g.drawString(font, title, left + 6, top + 5, TEXT);
        if (defs.isEmpty() || minecraft == null || minecraft.player == null) {
            g.drawString(font, Component.translatable("nourished.screen.nutrients.none"), left + 6, top + 24, MUTED);
            return;
        }
        drawTabs(g);
        MicroDef def = defs.get(selected);
        int color = 0xFF000000 | def.color();
        float level = Math.max(0f, Math.min(1f, MicroAttachment.level(minecraft.player, def.key())));

        int y = top + 20 + TAB_H + 6;
        g.renderItem(icon(def), left + 6, y);
        g.drawString(font, def.displayName(), left + 28, y, color);
        Component status = status(def, level);
        g.drawString(font, Component.literal(Math.round(level * 100) + "%  ").append(status), left + 28, y + 10, TEXT);

        int barX = left + 130;
        int barW = W - 136;
        g.fill(barX, y + 3, barX + barW, y + 11, TRACK);
        g.fill(barX, y + 3, barX + Math.round(barW * level), y + 11, color);
        marker(g, barX + Math.round(barW * def.lowThreshold()), y + 1, y + 13);
        marker(g, barX + Math.round(barW * def.highThreshold()), y + 1, y + 13);

        y += 26;
        drawTrend(g, def, color, left + 6, y);

        y += 36;
        g.drawString(font, Component.translatable("nourished.screen.nutrients.recent"), left + 6, y, MUTED);
        List<MicroClientHistory.Meal> meals = MicroClientHistory.mealsWith(def.key());
        ItemStack hovered = ItemStack.EMPTY;
        int x = left + 6;
        for (int i = 0; i < Math.min(meals.size(), 8); i++) {
            MicroClientHistory.Meal meal = meals.get(i);
            float gain = gain(meal.stack(), meal.profile().getOrDefault(def.key(), 0f));
            if (drawFood(g, meal.stack(), gain, x, y + 11, mouseX, mouseY)) hovered = meal.stack();
            x += ICON + 16;
        }
        if (meals.isEmpty()) {
            g.drawString(font, Component.translatable("nourished.screen.nutrients.nothing_yet"), left + 6, y + 14, MUTED);
        }

        y += 42;
        g.drawString(font, Component.translatable("nourished.screen.nutrients.best"), left + 6, y, MUTED);
        x = left + 6;
        for (MicroTopFoods.Entry entry : MicroTopFoods.top(def.key())) {
            if (drawFood(g, entry.stack(), entry.amount(), x, y + 11, mouseX, mouseY)) hovered = entry.stack();
            x += ICON + 16;
        }

        if (!hovered.isEmpty()) {
            g.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    private void drawTabs(GuiGraphics g) {
        int x = left + 6;
        int y = top + 20;
        for (int i = 0; i < defs.size(); i++) {
            MicroDef def = defs.get(i);
            int w = font.width(def.displayName()) + 8;
            boolean sel = i == selected;
            g.fill(x, y, x + w, y + TAB_H, sel ? 0xFF2E3640 : 0xFF1A1F25);
            if (sel) g.fill(x, y + TAB_H - 1, x + w, y + TAB_H, 0xFF000000 | def.color());
            g.drawString(font, def.displayName(), x + 4, y + 3, sel ? 0xFF000000 | def.color() : MUTED);
            x += w + 2;
        }
    }

    private int tabAt(double mouseX, double mouseY) {
        int x = left + 6;
        int y = top + 20;
        if (mouseY < y || mouseY >= y + TAB_H) return -1;
        for (int i = 0; i < defs.size(); i++) {
            int w = font.width(defs.get(i).displayName()) + 8;
            if (mouseX >= x && mouseX < x + w) return i;
            x += w + 2;
        }
        return -1;
    }

    private void drawTrend(GuiGraphics g, MicroDef def, int color, int x, int y) {
        List<Float> samples = MicroClientHistory.samples(def.key());
        int w = 160;
        int h = 28;
        g.fill(x, y, x + w, y + h, TRACK);
        if (samples.size() >= 2) {
            float step = (float) w / (samples.size() - 1);
            for (int i = 0; i < samples.size(); i++) {
                int px = x + Math.round(i * step);
                int py = y + h - 1 - Math.round((h - 2) * Math.max(0f, Math.min(1f, samples.get(i))));
                g.fill(px, py, px + 2, py + 2, color);
            }
        }
        g.drawString(font, trend(samples), x + w + 8, y + 10, TEXT);
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

    /** Draws an item with its per-serving gain under it; returns whether the mouse is over it. */
    private boolean drawFood(GuiGraphics g, ItemStack stack, float gain, int x, int y, int mouseX, int mouseY) {
        g.renderItem(stack, x, y);
        String label = "+" + Math.max(1, Math.round(gain * 100)) + "%";
        g.pose().pushPose();
        g.pose().translate(x, y + 18, 0);
        g.pose().scale(0.75f, 0.75f, 1f);
        g.drawString(font, label, 0, 0, TEXT);
        g.pose().popPose();
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private static float gain(ItemStack stack, float value) {
        FoodProperties food = stack.getFoodProperties(null);
        return food == null ? 0f : value * MicroProfileResolver.biteGain(food);
    }

    private static void marker(GuiGraphics g, int x, int y0, int y1) {
        g.fill(x, y0, x + 1, y1, 0xAAFFFFFF);
    }

    private static ItemStack icon(MicroDef def) {
        var id = net.minecraft.resources.ResourceLocation.tryParse(def.icon());
        var item = id != null ? net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id) : null;
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tab = tabAt(mouseX, mouseY);
        if (tab >= 0) {
            selected = tab;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!defs.isEmpty() && (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_TAB)) {
            selected = (selected + 1) % defs.size();
            return true;
        }
        if (!defs.isEmpty() && keyCode == GLFW.GLFW_KEY_LEFT) {
            selected = Math.floorMod(selected - 1, defs.size());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
