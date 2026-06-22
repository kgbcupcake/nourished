package dev.maire.nourished.client.config.categories.widgets;

import dev.maire.nourished.client.config.NourishedConfigScreen.CompatPending;
import dev.maire.nourished.client.config.categories.widgets.compat.BuiltInTabState;
import dev.maire.nourished.client.config.categories.widgets.compat.DetectedTabState;
import dev.maire.nourished.client.config.categories.widgets.compat.SettingsTabState;
import dev.maire.nourished.config.NourishedConfig;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_BG;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_TAB_ACTIVE;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_TAB_BG;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_TAB_BORDER_ACTIVE;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_TAB_BORDER_INACTIVE;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.GAP;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.ROW_H;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.TAB_BAR_H;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.VIEWPORT_H;

public final class CompatTabbedListEntry extends TooltipListEntry<Object> {
    private final Map<String, CompatPending> compatPending;

    private final Button detectedTabButton;
    private final Button builtInTabButton;
    private final Button settingsTabButton;
    private final String detectedTabLabel;
    private final String builtInTabLabel;
    private final String settingsTabLabel;

    private final BuiltInTabState builtInTab;
    private final DetectedTabState detectedTab;
    private final SettingsTabState settingsTab;

    private int tabIndex;
    private int listX;
    private int listY;
    private int listW;
    private int listH;

    public CompatTabbedListEntry(NourishedConfig config, Map<String, CompatPending> compatPending) {
        super(
                Component.translatable("config.nourished.compat.title"),
                () -> Optional.of(new Component[]{Component.translatable("config.nourished.compat.desc")}),
                false
        );
        this.compatPending = compatPending;

        this.detectedTabLabel = Component.translatable("config.nourished.compat.tab.detected").getString();
        this.builtInTabLabel = Component.translatable("config.nourished.compat.tab.builtin").getString();
        this.settingsTabLabel = Component.translatable("config.nourished.compat.tab.settings").getString();

        this.detectedTabButton = buildTabButton(Component.literal(detectedTabLabel), 0);
        this.builtInTabButton = buildTabButton(Component.literal(builtInTabLabel), 1);
        this.settingsTabButton = buildTabButton(Component.literal(settingsTabLabel), 2);

        this.builtInTab = new BuiltInTabState(compatPending, this::requestReferenceRebuilding, this::isEditable);
        this.detectedTab = new DetectedTabState(
                compatPending,
                builtInTab.getBuiltInByModId(),
                this::requestReferenceRebuilding,
                this::isEditable
        );
        this.settingsTab = new SettingsTabState(compatPending, this::isEditable);
    }

    private Button buildTabButton(Component label, int idx) {
        return Button.builder(label, b -> {
                    tabIndex = idx;
                    detectedTab.resetScrollOffset();
                })
                .bounds(0, 0, 100, 20)
                .build();
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
        int bodyH = switch (tabIndex) {
            case 1 -> builtInTab.getRowCount() * ROW_H + builtInTab.getExpandedPanelExtraHeight();
            case 2 -> settingsTab.getRowCount() * ROW_H;
            default -> detectedTab.getBodyHeight();
        };
        return TAB_BAR_H + bodyH + 20;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (tabIndex == 0
                && detectedTab.handleMouseScrolled(mouseX, mouseY, deltaY, listX, listW)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
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
        int innerX = x + 4;
        int innerW = entryWidth - 8;
        int tabW = (innerW - GAP * 2) / 3;

        detectedTabButton.setX(innerX);
        detectedTabButton.setY(y);
        detectedTabButton.setWidth(tabW);
        builtInTabButton.setX(innerX + tabW + GAP);
        builtInTabButton.setY(y);
        builtInTabButton.setWidth(tabW);
        settingsTabButton.setX(innerX + (tabW + GAP) * 2);
        settingsTabButton.setY(y);
        settingsTabButton.setWidth(tabW);
        detectedTabButton.active = isEditable();
        builtInTabButton.active = isEditable();
        settingsTabButton.active = isEditable();
        drawTab(graphics, detectedTabButton, tabIndex == 0, detectedTabLabel);
        drawTab(graphics, builtInTabButton, tabIndex == 1, builtInTabLabel);
        drawTab(graphics, settingsTabButton, tabIndex == 2, settingsTabLabel);

        listX = innerX;
        listY = y + TAB_BAR_H;
        listW = innerW;
        listH = tabIndex == 0 ? VIEWPORT_H : rowCountForTab() * ROW_H;
        graphics.fill(listX, listY, listX + listW, listY + listH, COL_BG);
        graphics.renderOutline(listX, listY, listW, listH, 0xFF404040);

        boolean editable = isEditable();
        if (tabIndex == 0) {
            builtInTab.hideWidgets();
            settingsTab.hideWidgets();
            detectedTab.renderBody(graphics, listX, listY, listW, listH, mouseX, mouseY, delta, editable);
        } else if (tabIndex == 1) {
            detectedTab.hideWidgets();
            settingsTab.hideWidgets();
            builtInTab.renderBody(graphics, listX, listY, listW, listH, mouseX, mouseY, delta, editable);
        } else {
            detectedTab.hideWidgets();
            builtInTab.hideWidgets();
            settingsTab.renderBody(graphics, listX, listY, listW, listH, mouseX, mouseY, delta, editable);
        }
    }

    private int rowCountForTab() {
        return switch (tabIndex) {
            case 1 -> builtInTab.getRowCount();
            case 2 -> settingsTab.getRowCount();
            default -> detectedTab.getRowCount();
        };
    }

    private void drawTab(GuiGraphics graphics, Button button, boolean active, String label) {
        int fill = active ? COL_TAB_ACTIVE : COL_TAB_BG;
        int border = active ? COL_TAB_BORDER_ACTIVE : COL_TAB_BORDER_INACTIVE;
        int bx = button.getX();
        int by = button.getY();
        int bw = button.getWidth();
        int bh = 20;

        graphics.fill(bx, by, bx + bw, by + bh, fill);
        graphics.renderOutline(bx, by, bw, bh, border);

        var font = Minecraft.getInstance().font;
        int tx = bx + (bw - font.width(label)) / 2;
        int ty = by + (bh - 8) / 2;
        graphics.drawString(font, label, tx, ty, 0xFFF0F0F0, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tabIndex == 0 && detectedTab.handleMouseClicked(mouseX, mouseY, button, listX, listY, listW, listH)) {
            return true;
        }
        if (tabIndex == 1 && builtInTab.handleMouseClicked(mouseX, mouseY, button, listX, listY, listW, listH)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (tabIndex == 0 && detectedTab.handleMouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (tabIndex == 0 && detectedTab.handleMouseReleased(button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        List<GuiEventListener> out = new ArrayList<>();
        out.add(detectedTabButton);
        out.add(builtInTabButton);
        out.add(settingsTabButton);
        detectedTab.addChildren(out);
        builtInTab.addChildren(out);
        settingsTab.addChildren(out);
        return out;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        List<NarratableEntry> out = new ArrayList<>();
        out.add(detectedTabButton);
        out.add(builtInTabButton);
        out.add(settingsTabButton);
        detectedTab.addNarratables(out);
        builtInTab.addNarratables(out);
        settingsTab.addNarratables(out);
        return out;
    }
}
