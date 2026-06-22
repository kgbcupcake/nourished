package dev.maire.nourished.client.config.categories.widgets;

import dev.maire.nourished.client.config.NourishedConfigSharedWidgets;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StyledChipTextEntry extends TooltipListEntry<Object> {
    private static final int HEIGHT = 22;
    private static final int BG = 0x661C1C1C;
    private static final int TEXT = 0xFFE0E0E0;
    private final Component label;
    private final int borderColor;

public StyledChipTextEntry(Component label, int borderColor) {
        super(label, Optional::empty, false);
        this.label = label;
        this.borderColor = borderColor;
    }

    @Override
    public boolean isEdited() {
        return true;
    }

    @Override
    public void save() {}

    @Override
    public Object getValue() {
        return Boolean.FALSE;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

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
        graphics.fill(x, y, x + entryWidth, y + HEIGHT - 1, BG);
        graphics.renderOutline(x, y, entryWidth, HEIGHT - 1, borderColor);
        String text = NourishedConfigSharedWidgets.ellipsize(
                Minecraft.getInstance().font,
                label.getString(),
                Math.max(0, entryWidth - 12)
        );
        graphics.drawString(Minecraft.getInstance().font, text, x + 6, y + 7, TEXT, false);
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

