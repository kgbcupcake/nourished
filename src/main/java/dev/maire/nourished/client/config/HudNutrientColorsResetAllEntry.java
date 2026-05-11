package dev.maire.nourished.client.config;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * Cloth Config row: invokes a callback that clears every nutrient color override and refreshes hex rows.
 */
public final class HudNutrientColorsResetAllEntry extends TooltipListEntry<Object> {

    private final Button button;

    public HudNutrientColorsResetAllEntry(Runnable onResetAll) {
        super(Component.empty(), () -> Optional.of(new Component[]{
                Component.translatable("config.nourished.hudColors.resetAll.tooltip")
        }), false);
        this.button = Button.builder(Component.translatable("config.nourished.hudColors.resetAll"), b -> onResetAll.run())
                .bounds(0, 0, 160, 20)
                .build();
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
    public void save() {
        // Persistence happens in per-row entries and global {@code ColorRegistry.save()} in screen runnable.
    }

    @Override
    public int getItemHeight() {
        return 26;
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
        button.setX(x + 6);
        button.setY(y + 3);
        button.setWidth(Math.min(200, entryWidth - 12));
        button.active = isEditable();
        button.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        return List.of(button);
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return List.of(button);
    }
}
