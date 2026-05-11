package dev.maire.nourished.client.config;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/** Section banner for HUD nutrient color customization (Cloth Config list row). */
public final class HudNutrientColorsSectionHeaderEntry extends TooltipListEntry<Object> {

    private static final int HEIGHT = 26;
    private static final int BG = 0x8824183A;
    private static final int BORDER = 0xFFFFD700;
    private static final int TEXT = 0xFFFFF8DC;

    public HudNutrientColorsSectionHeaderEntry() {
        super(Component.empty(), () -> Optional.empty(), false);
    }

    @Override
    public Object getValue() {
        return Boolean.FALSE;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public boolean isEdited() {
        return false;
    }

    @Override
    public void save() {}

    @Override
    public int getItemHeight() {
        return HEIGHT;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int index,
            int y,
            int x,
            int entryWidth,
            int entryHeight,
            int mouseX,
            int mouseY,
            boolean isHovered,
            float delta
    ) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        int h = HEIGHT - 2;
        graphics.fill(x, y, x + entryWidth, y + h, BG);
        graphics.renderOutline(x, y, entryWidth, h, BORDER);
        Component title = Component.translatable("config.nourished.hudColors.sectionTitle");
        graphics.drawString(Minecraft.getInstance().font, title, x + 8, y + 8, TEXT, false);
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        return List.of();
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return List.of();
    }
}
