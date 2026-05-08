package dev.maire.nourished.client.config;

import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * Presets category footer: open import/export flows.
 */
public final class ImportExportButtonsWidget extends TooltipListEntry<Object> {

    private static final int BTN_H = 20;
    private static final int BTN_W = 120;
    private static final int GAP = 6;

    private final Button exportButton;
    private final Button importButton;

    public ImportExportButtonsWidget(Screen reopenParent) {
        super(
                Component.translatable("config.nourished.importExport.groupTitle"),
                () -> Optional.of(new Component[]{Component.translatable("config.nourished.importExport.groupTitle.desc")}),
                false);
        Minecraft mc = Minecraft.getInstance();
        Screen parentForScreens = reopenParent;
        this.exportButton = Button.builder(Component.translatable("config.nourished.importExport.export"), b ->
                        mc.setScreen(new ExportConfigScreen(getConfigScreen(), parentForScreens)))
                .bounds(0, 0, BTN_W, BTN_H)
                .build();
        this.importButton = Button.builder(Component.translatable("config.nourished.importExport.import"), b ->
                        mc.setScreen(new ImportConfigScreen(getConfigScreen(), parentForScreens)))
                .bounds(0, 0, BTN_W, BTN_H)
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
    }

    @Override
    public int getItemHeight() {
        return 28;
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        return List.of(exportButton, importButton);
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return List.of(exportButton, importButton);
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
        if (!isRowVisible(y, getItemHeight())) {
            exportButton.setY(-2000);
            importButton.setY(-2000);
            return;
        }
        int innerW = entryWidth - 8;
        int sx = x + 4;
        int cy = y + 4;
        int buttonWidth = Math.max(80, Math.min(BTN_W, (innerW - GAP) / 2));
        exportButton.setY(cy);
        exportButton.active = isEditable();
        exportButton.setWidth(buttonWidth);
        importButton.setY(cy);
        importButton.active = isEditable();
        importButton.setWidth(buttonWidth);
        importButton.setX(sx + Math.max(0, innerW - buttonWidth));
        exportButton.setX(sx + Math.max(0, innerW - 2 * buttonWidth - GAP));
        exportButton.render(graphics, mouseX, mouseY, delta);
        importButton.render(graphics, mouseX, mouseY, delta);
    }

    private boolean isRowVisible(int y, int h) {
        if (!(Minecraft.getInstance().screen instanceof ClothConfigScreen cloth) || cloth.listWidget == null) {
            return true;
        }
        return y < cloth.listWidget.bottom && y + h > cloth.listWidget.top;
    }
}
