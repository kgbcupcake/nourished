package dev.maire.nourished.client.config.categories.widgets;

import dev.maire.nourished.client.config.NourishedConfigSharedWidgets;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ModuleToggleListEntry extends TooltipListEntry<Boolean> {
    private static final int ICON_WIDTH = 24;
    private static final int ICON_PAD = 6;
    private static final int PILL_PREF_WIDTH = 130;
    private static final int PILL_MIN_WIDTH = 72;
    private static final int CHIP_WIDTH = 74;
    private static final int CHIP_HEIGHT = 12;
    private static final int GAP = 4;
    private static final int COL_LABEL = 0xFFE0E0E0;
    private static final int COL_HINT = 0xFFCC8844;
    private static final int COL_ON_TEXT = 0xFFB8F2B8;
    private static final int COL_OFF_TEXT = 0xFFF0B2B2;
    private static final int COL_CHIP_ON = 0xFF2C7F2C;
    private static final int COL_CHIP_OFF = 0xFF8A2F2F;
    private static final int COL_CHIP_BORDER = 0xFF1A1A1A;
    private static final int COL_CHIP_HOVER = 0x22FFFFFF;
    private static final int COL_ROW_SEPARATOR = 0x223A3A3A;
    private static final int COL_GROUP_BG = 0x55244A6C;
    private static final int COL_GROUP_BORDER = 0xAA5DA9DE;

    private final AtomicBoolean pending;
    private final String group;
    private final String dependsOnKey;
    private final Map<String, AtomicBoolean> modulePending;
    private final Component label;
    private int pillX;
    private int pillY;
    private int pillW = PILL_PREF_WIDTH;
    private static final int PILL_HEIGHT = 18;
    private boolean pillPressed;
    private float hoverAlpha;
    private long lastHoverUpdateMs;

public ModuleToggleListEntry(
            Component label,
            Component tooltip,
            AtomicBoolean pending,
            String group,
            String dependsOnKey,
            Map<String, AtomicBoolean> modulePending
    ) {
        super(label, () -> Optional.of(new Component[]{tooltip}), false);
        this.label = label;
        this.pending = pending;
        this.group = group;
        this.dependsOnKey = dependsOnKey;
        this.modulePending = modulePending;
    }

    private void togglePending() {
        boolean next = !this.pending.get();
        if (next && dependsOnKey != null) {
            AtomicBoolean dep = modulePending.get(dependsOnKey);
            if (dep != null && !dep.get()) {
                dep.set(true);
            }
        }
        this.pending.set(next);
    }

    private static String groupBadge(String group) {
        return switch (group) {
            case "core" -> "C";
            case "ui" -> "UI";
            default -> "+";
        };
    }

    @Override
    public boolean isEdited() {
        return true;
    }

    @Override
    public void save() {
        // Saved by outer screen save runnable.
    }

    @Override
    public Boolean getValue() {
        return pending.get();
    }

    @Override
    public Optional<Boolean> getDefaultValue() {
        return Optional.of(true);
    }

    @Override
    public int getItemHeight() {
        return 24;
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
            float delta) {
        Font font = Minecraft.getInstance().font;
        ModuleRowLayout layout = ModuleRowLayout.compute(x, entryWidth);

        pillX = layout.pillX();
        pillY = y + 1;
        pillW = layout.pillWidth();

        int iconY = y + 4;
        graphics.fill(layout.iconX(), iconY, layout.iconX() + ICON_WIDTH, iconY + 14, COL_GROUP_BG);
        graphics.renderOutline(layout.iconX(), iconY, ICON_WIDTH, 14, COL_GROUP_BORDER);
        String badge = groupBadge(group);
        int badgeX = layout.iconX() + (ICON_WIDTH - font.width(badge)) / 2;
        graphics.drawString(font, badge, badgeX, iconY + 3, 0xFFE8F4FF, false);

        String labelText = NourishedConfigSharedWidgets.ellipsize(font, label.getString(), layout.labelMaxWidth());
        graphics.drawString(font, labelText, layout.labelX(), y + 6, COL_LABEL, false);

        if (layout.showStatusChip()) {
            int chipX = layout.chipX();
            int chipY = y + 4;
            boolean isOn = pending.get();
            int chipFill = isOn ? COL_CHIP_ON : COL_CHIP_OFF;
            int chipText = isOn ? COL_ON_TEXT : COL_OFF_TEXT;
            graphics.fill(chipX, chipY, chipX + CHIP_WIDTH, chipY + CHIP_HEIGHT, chipFill);
            graphics.renderOutline(chipX, chipY, CHIP_WIDTH, CHIP_HEIGHT, COL_CHIP_BORDER);
            if (mouseX >= chipX && mouseX < chipX + CHIP_WIDTH && mouseY >= chipY && mouseY < chipY + CHIP_HEIGHT) {
                graphics.fill(chipX, chipY, chipX + CHIP_WIDTH, chipY + CHIP_HEIGHT, COL_CHIP_HOVER);
            }
            String chipLabel = isOn ? "ENABLED" : "DISABLED";
            int chipTextX = chipX + (CHIP_WIDTH - font.width(chipLabel)) / 2;
            graphics.drawString(font, chipLabel, chipTextX, chipY + 2, chipText, false);
        }

        if (layout.showDependencyHint() && dependsOnKey != null) {
            AtomicBoolean dep = modulePending.get(dependsOnKey);
            if (dep != null && !dep.get()) {
                String depLabel = Component.translatable("config.nourished." + dependsOnKey).getString();
                String depText = Component.translatable("config.nourished.modules.requires", depLabel).getString();
                graphics.drawString(
                        font,
                        NourishedConfigSharedWidgets.ellipsize(font, depText, layout.dependencyMaxWidth()),
                        layout.dependencyX(),
                        y + 15,
                        COL_HINT,
                        false
                );
            }
        }

        boolean isOn = pending.get();
        boolean pillHovered = mouseX >= pillX && mouseX < pillX + pillW && mouseY >= pillY && mouseY < pillY + PILL_HEIGHT;
        long now = System.currentTimeMillis();
        if (lastHoverUpdateMs == 0L) {
            lastHoverUpdateMs = now;
        }
        float dt = Math.min(0.05f, (now - lastHoverUpdateMs) / 1000.0f);
        lastHoverUpdateMs = now;
        float target = pillHovered ? 1.0f : 0.0f;
        float speed = 10.0f;
        hoverAlpha += (target - hoverAlpha) * Math.min(1.0f, dt * speed);
        int drawOffset = pillPressed ? 1 : 0;
        int px = pillX;
        int py = pillY + drawOffset;
        int pillBg = isEditable() ? (pillPressed ? 0xFF1D4258 : 0xFF234F6B) : 0xFF3A3A3A;
        graphics.fill(px, py, px + pillW, py + PILL_HEIGHT, pillBg);
        graphics.renderOutline(px, py, pillW, PILL_HEIGHT, 0xFF5DA9DE);
        if (!pillPressed && hoverAlpha > 0.01f) {
            int a = Math.max(0, Math.min(255, (int) (hoverAlpha * 0x22)));
            graphics.fill(px, py, px + pillW, py + PILL_HEIGHT, (a << 24) | 0x00FFFFFF);
        }
        String pillText = Component.translatable("config.nourished.modules.toggle").getString();
        int pillTextX = px + (pillW - font.width(pillText)) / 2;
        graphics.drawString(font, pillText, pillTextX, py + 5, 0xFFE8F4FF, false);
        graphics.fill(x, y + 23, x + entryWidth, y + 24, COL_ROW_SEPARATOR);
    }

    private record ModuleRowLayout(
            int iconX,
            int labelX,
            int labelMaxWidth,
            int chipX,
            int pillX,
            int pillWidth,
            int dependencyX,
            int dependencyMaxWidth,
            boolean showStatusChip,
            boolean showDependencyHint
    ) {
        private static ModuleRowLayout compute(int x, int entryWidth) {
            int iconX = x;
            int labelX = x + ICON_WIDTH + ICON_PAD;
            int rightEdge = x + entryWidth;

            int pillWidth = Math.min(PILL_PREF_WIDTH, Math.max(PILL_MIN_WIDTH, entryWidth / 3));
            int pillX = rightEdge - pillWidth;
            int chipX = pillX - GAP - CHIP_WIDTH;
            int minLabelWidth = 48;
            boolean showStatusChip = chipX >= labelX + minLabelWidth;
            int textRight = showStatusChip ? chipX - GAP : pillX - GAP;
            int labelMaxWidth = Math.max(24, textRight - labelX);
            if (labelMaxWidth < minLabelWidth && showStatusChip) {
                showStatusChip = false;
                textRight = pillX - GAP;
                labelMaxWidth = Math.max(24, textRight - labelX);
            }
            boolean showDependencyHint = showStatusChip && entryWidth >= 320;
            int dependencyX = showStatusChip ? chipX : labelX;
            int dependencyMaxWidth = showStatusChip
                    ? Math.max(24, pillX - GAP - dependencyX)
                    : labelMaxWidth;
            return new ModuleRowLayout(
                    iconX,
                    labelX,
                    labelMaxWidth,
                    chipX,
                    pillX,
                    pillWidth,
                    dependencyX,
                    dependencyMaxWidth,
                    showStatusChip,
                    showDependencyHint
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isEditable()
                && mouseX >= pillX && mouseX < pillX + pillW
                && mouseY >= pillY && mouseY < pillY + PILL_HEIGHT) {
            pillPressed = true;
            togglePending();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            pillPressed = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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

