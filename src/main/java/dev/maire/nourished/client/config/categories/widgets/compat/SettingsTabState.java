package dev.maire.nourished.client.config.categories.widgets.compat;

import dev.maire.nourished.client.config.NourishedConfigScreen.CompatPending;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_ROW_SEPARATOR;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_TEXT;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.ROW_H;

public final class SettingsTabState {
    private final Map<String, CompatPending> compatPending;
    private final Supplier<Boolean> editableSupplier;

    private final List<String> settingsRows;
    private final Map<String, Button> codeButtons = new LinkedHashMap<>();
    private final Map<String, Button> tagButtons = new LinkedHashMap<>();

    private int listX;
    private int listY;
    private int listW;
    private int listH;

    public SettingsTabState(
            Map<String, CompatPending> compatPending,
            Supplier<Boolean> editableSupplier
    ) {
        this.compatPending = compatPending;
        this.editableSupplier = editableSupplier;
        this.settingsRows = new ArrayList<>(compatPending.keySet());
        this.settingsRows.sort(String::compareTo);
        for (String modid : settingsRows) {
            codeButtons.put(modid, buildToggleButton(modid, true));
            tagButtons.put(modid, buildToggleButton(modid, false));
        }
    }

    public int getRowCount() {
        return settingsRows.size();
    }

    public void hideWidgets() {
        for (String modid : settingsRows) {
            Button codeBtn = codeButtons.get(modid);
            Button tagBtn = tagButtons.get(modid);
            if (codeBtn != null) codeBtn.setY(-2000);
            if (tagBtn != null) tagBtn.setY(-2000);
        }
    }

    public void renderBody(
            GuiGraphics graphics,
            int listX,
            int listY,
            int listW,
            int listH,
            int mouseX,
            int mouseY,
            float delta,
            boolean editable
    ) {
        this.listX = listX;
        this.listY = listY;
        this.listW = listW;
        this.listH = listH;
        renderSettingsRows(graphics, listY, mouseX, mouseY, delta);
    }

    public void addChildren(List<GuiEventListener> out) {
        out.addAll(codeButtons.values());
        out.addAll(tagButtons.values());
    }

    public void addNarratables(List<NarratableEntry> out) {
        out.addAll(codeButtons.values());
        out.addAll(tagButtons.values());
    }

    private Button buildToggleButton(String modid, boolean code) {
        return Button.builder(Component.empty(), b -> {
                    CompatPending pending = compatPending.get(modid);
                    if (pending == null) return;
                    if (code) {
                        pending.codeCompat.set(!pending.codeCompat.get());
                    } else {
                        pending.tagCompat.set(!pending.tagCompat.get());
                    }
                    updateToggleLabel(modid, code);
                })
                .bounds(0, 0, 90, 18)
                .build();
    }

    private void updateToggleLabel(String modid, boolean code) {
        CompatPending pending = compatPending.get(modid);
        Button btn = code ? codeButtons.get(modid) : tagButtons.get(modid);
        if (pending == null || btn == null) return;
        boolean value = code ? pending.codeCompat.get() : pending.tagCompat.get();
        btn.setMessage(Component.literal((code ? "Code " : "Tag ") + (value ? "ON" : "OFF")));
    }

    private void renderSettingsRows(GuiGraphics graphics, int yStart, int mouseX, int mouseY, float delta) {
        var font = Minecraft.getInstance().font;
        int pad = 4;
        int btnGap = 4;
        int minBtnW = 72;
        int btnW = 92;
        int btnBlockW = btnW * 2 + btnGap;
        int scrollbarReserve = 0;
        for (int i = 0; i < settingsRows.size(); i++) {
            String modid = settingsRows.get(i);
            int ry = yStart + i * ROW_H;
            Button codeBtn = codeButtons.get(modid);
            Button tagBtn = tagButtons.get(modid);
            if (ry + ROW_H < listY || ry > listY + listH) {
                if (codeBtn != null) codeBtn.setY(-2000);
                if (tagBtn != null) tagBtn.setY(-2000);
                continue;
            }

            updateToggleLabel(modid, true);
            updateToggleLabel(modid, false);

            int availableBtnW = Math.max(minBtnW * 2 + btnGap, listW - pad * 2);
            if (availableBtnW < btnBlockW) {
                btnW = Math.max(minBtnW, (availableBtnW - btnGap) / 2);
                btnBlockW = btnW * 2 + btnGap;
            }
            int rightX = listX + listW - btnBlockW - pad - scrollbarReserve;
            int labelMaxW = Math.max(24, rightX - (listX + pad) - 4);
            String label = CompatTabUi.ellipsize(font, CompatTabUi.toTitleCase(modid), labelMaxW);
            graphics.drawString(font, label, listX + pad, ry + 6, COL_TEXT, false);

            if (codeBtn != null) {
                codeBtn.setX(rightX);
                codeBtn.setY(ry + 3);
                codeBtn.setWidth(btnW);
                codeBtn.active = CompatTabUi.isToggleEditable(modid, true, editableSupplier);
                codeBtn.render(graphics, mouseX, mouseY, delta);
            }
            if (tagBtn != null) {
                tagBtn.setX(rightX + btnW + btnGap);
                tagBtn.setY(ry + 3);
                tagBtn.setWidth(btnW);
                tagBtn.active = CompatTabUi.isToggleEditable(modid, false, editableSupplier);
                tagBtn.render(graphics, mouseX, mouseY, delta);
            }
            graphics.fill(listX, ry + ROW_H - 1, listX + listW, ry + ROW_H, COL_ROW_SEPARATOR);
        }
    }
}
