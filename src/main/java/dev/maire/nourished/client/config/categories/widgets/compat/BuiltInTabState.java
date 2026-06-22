package dev.maire.nourished.client.config.categories.widgets.compat;

import dev.maire.nourished.client.config.NourishedConfigScreen.CompatPending;
import dev.marie.MariesLib.compat.CompatEntry;
import dev.marie.MariesLib.compat.ModCompat;
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

import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.BUILTIN_PANEL_H;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_CHIP_BORDER;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_ROW_SEPARATOR;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_SUBTEXT;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_TAB_ACTIVE;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_TAB_BORDER_ACTIVE;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_TEXT;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.ROW_H;

public final class BuiltInTabState {
    private final Map<String, CompatPending> compatPending;
    private final Runnable requestRebuild;
    private final Supplier<Boolean> editableSupplier;

    private final List<CompatEntry> builtInRows;
    private final Map<String, CompatEntry> builtInByModId = new LinkedHashMap<>();
    private final Button builtInCodeButton;
    private final Button builtInTagButton;

    private int expandedBuiltInIndex = -1;
    private String expandedBuiltInModId;
    private int listX;
    private int listY;
    private int listW;
    private int listH;

    public BuiltInTabState(
            Map<String, CompatPending> compatPending,
            Runnable requestRebuild,
            Supplier<Boolean> editableSupplier
    ) {
        this.compatPending = compatPending;
        this.requestRebuild = requestRebuild;
        this.editableSupplier = editableSupplier;
        this.builtInRows = new ArrayList<>(ModCompat.getBuiltInEntries());
        for (CompatEntry row : builtInRows) {
            builtInByModId.put(row.modId(), row);
        }
        this.builtInCodeButton = buildInlineBuiltInToggleButton(true);
        this.builtInTagButton = buildInlineBuiltInToggleButton(false);
    }

    public Map<String, CompatEntry> getBuiltInByModId() {
        return builtInByModId;
    }

    public int getRowCount() {
        return builtInRows.size();
    }

    public int getExpandedPanelExtraHeight() {
        return expandedBuiltInIndex >= 0 ? BUILTIN_PANEL_H : 0;
    }

    public boolean hasExpandedPanel() {
        return expandedBuiltInIndex >= 0;
    }

    public void hideWidgets() {
        builtInCodeButton.setY(-2000);
        builtInTagButton.setY(-2000);
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
        renderBuiltInRows(graphics, listY, mouseX, mouseY);
    }

    public boolean handleMouseClicked(double mouseX, double mouseY, int button, int listX, int listY, int listW, int listH) {
        this.listX = listX;
        this.listY = listY;
        this.listW = listW;
        this.listH = listH;

        if (button == 0 && mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
            int cy = listY;
            for (int i = 0; i < builtInRows.size(); i++) {
                int rowTop = cy;
                int rowBottom = cy + ROW_H;
                if (mouseY >= rowTop && mouseY < rowBottom) {
                    if (expandedBuiltInIndex == i) {
                        expandedBuiltInIndex = -1;
                        expandedBuiltInModId = null;
                    } else {
                        expandedBuiltInIndex = i;
                        expandedBuiltInModId = builtInRows.get(i).modId();
                    }
                    requestRebuild.run();
                    return true;
                }
                cy += ROW_H;
                if (expandedBuiltInIndex == i) {
                    cy += BUILTIN_PANEL_H;
                }
            }
        }
        return false;
    }

    public void addChildren(List<GuiEventListener> out) {
        out.add(builtInCodeButton);
        out.add(builtInTagButton);
    }

    public void addNarratables(List<NarratableEntry> out) {
        out.add(builtInCodeButton);
        out.add(builtInTagButton);
    }

    private Button buildInlineBuiltInToggleButton(boolean code) {
        return Button.builder(Component.empty(), b -> {
                    if (expandedBuiltInModId == null) return;
                    CompatPending pending = compatPending.get(expandedBuiltInModId);
                    if (pending == null) return;
                    if (code) {
                        pending.codeCompat.set(!pending.codeCompat.get());
                    } else {
                        pending.tagCompat.set(!pending.tagCompat.get());
                    }
                    updateBuiltInPanelButtonLabels();
                })
                .bounds(0, 0, 90, 18)
                .build();
    }

    private void updateBuiltInPanelButtonLabels() {
        if (expandedBuiltInModId == null) {
            builtInCodeButton.setMessage(Component.literal("Code Compat OFF"));
            builtInTagButton.setMessage(Component.literal("Tag Compat OFF"));
            return;
        }
        CompatPending pending = compatPending.get(expandedBuiltInModId);
        boolean codeOn = pending != null && pending.codeCompat.get();
        boolean tagOn = pending != null && pending.tagCompat.get();
        builtInCodeButton.setMessage(Component.literal("Code Compat " + (codeOn ? "ON" : "OFF")));
        builtInTagButton.setMessage(Component.literal("Tag Compat " + (tagOn ? "ON" : "OFF")));
    }

    private void renderBuiltInRows(GuiGraphics graphics, int yStart, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int pad = 4;
        int chipW = 72;
        int chipGap = 8;
        int textXMin = listX + pad + chipW + chipGap;
        int cy = yStart;
        for (int i = 0; i < builtInRows.size(); i++) {
            CompatEntry row = builtInRows.get(i);
            int ry = cy;
            if (ry + ROW_H < listY || ry > listY + listH) {
                if (expandedBuiltInIndex == i) {
                    builtInCodeButton.setY(-2000);
                    builtInTagButton.setY(-2000);
                }
                cy += ROW_H + (expandedBuiltInIndex == i ? BUILTIN_PANEL_H : 0);
                continue;
            }
            int textMaxW = Math.max(24, listX + listW - pad - textXMin);
            drawCategoryChip(graphics, listX + pad, ry + 4, row.category().name());
            String title = CompatTabUi.ellipsize(font, CompatTabUi.toTitleCase(row.displayName()), textMaxW);
            String summary = CompatTabUi.ellipsize(font, builtInSummary(row), textMaxW);
            graphics.drawString(font, title, textXMin, ry + 5, COL_TEXT, false);
            graphics.drawString(font, summary, textXMin, ry + 14, COL_SUBTEXT, false);
            graphics.fill(listX, ry + ROW_H - 1, listX + listW, ry + ROW_H, COL_ROW_SEPARATOR);
            if (expandedBuiltInIndex == i) {
                renderBuiltInSubPanel(graphics, row, ry + ROW_H, mouseX, mouseY);
            }
            cy += ROW_H + (expandedBuiltInIndex == i ? BUILTIN_PANEL_H : 0);
        }
        if (expandedBuiltInIndex < 0) {
            builtInCodeButton.setY(-2000);
            builtInTagButton.setY(-2000);
        }
    }

    private void renderBuiltInSubPanel(GuiGraphics graphics, CompatEntry row, int panelY, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int px = listX + 2;
        int pw = Math.max(0, listW - 4);
        int py = panelY;
        int ph = BUILTIN_PANEL_H;
        graphics.fill(px, py, px + pw, py + ph, 0x5518202A);
        graphics.renderOutline(px, py, pw, ph, COL_TAB_BORDER_ACTIVE);

        int chipW = 72;
        int titleMaxW = Math.max(24, pw - chipW - 16);
        String title = CompatTabUi.ellipsize(font, CompatTabUi.toTitleCase(row.displayName()), titleMaxW);
        graphics.drawString(font, title, px + 6, py + 4, COL_TEXT, false);
        int chipX = Math.min(px + pw - chipW - 6, px + 150);
        drawCategoryChip(graphics, chipX, py + 3, row.category().name());

        updateBuiltInPanelButtonLabels();
        int btnGap = 6;
        int minBtnW = 88;
        int btnW = Math.min(110, Math.max(minBtnW, (pw - 12 - btnGap) / 2));
        int btnY = py + 17;
        int btnX = px + 6;
        builtInCodeButton.setX(btnX);
        builtInCodeButton.setY(btnY);
        builtInCodeButton.setWidth(btnW);
        builtInCodeButton.active = CompatTabUi.isToggleEditable(row.modId(), true, editableSupplier);
        builtInCodeButton.render(graphics, mouseX, mouseY, 0.0f);
        builtInTagButton.setX(btnX + btnW + btnGap);
        builtInTagButton.setY(btnY);
        builtInTagButton.setWidth(btnW);
        builtInTagButton.active = CompatTabUi.isToggleEditable(row.modId(), false, editableSupplier);
        builtInTagButton.render(graphics, mouseX, mouseY, 0.0f);
    }

    private void drawCategoryChip(GuiGraphics graphics, int x, int y, String category) {
        int chipW = 72;
        int chipH = 14;
        graphics.fill(x, y, x + chipW, y + chipH, COL_TAB_ACTIVE);
        graphics.renderOutline(x, y, chipW, chipH, COL_CHIP_BORDER);
        String shortCat = category.length() > 10 ? category.substring(0, 10) : category;
        graphics.drawString(Minecraft.getInstance().font, shortCat, x + 4, y + 3, 0xFFF0F0F0, false);
    }

    private String builtInSummary(CompatEntry entry) {
        List<String> bullets = new ArrayList<>();
        if (entry.providesSourceTags()) bullets.add("provides tags");
        if (entry.handlesOwnValues()) bullets.add("handles own values");
        if (entry.conflictBehavior() != null) bullets.add("conflict rules");
        if (bullets.isEmpty()) bullets.add("baseline compat mapping");
        return String.join(", ", bullets);
    }
}
