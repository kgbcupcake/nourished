package dev.maire.nourished.client.config;

import com.google.gson.JsonObject;

import dev.maire.nourished.client.config.NourishedConfigScreen;
import dev.maire.nourished.core.Nourished;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * Import from an export file or {@code NCF1:} share code, preview sections, then apply.
 */
public final class ImportConfigScreen extends Screen {

    public enum Phase {
        CHOOSE,
        PREVIEW
    }

    private final Screen returnTo;
    private final Screen reopenParent;
    private Phase phase = Phase.CHOOSE;
    private int sourceTab;
    private String codeDraft = "";
    private JsonObject pending;
    private EnumSet<ImportExportManager.Section> available = EnumSet.noneOf(ImportExportManager.Section.class);
    private EnumSet<ImportExportManager.Section> selectedSections = EnumSet.noneOf(ImportExportManager.Section.class);
    private List<Path> exportFiles = List.of();
    private int selectedFileIndex = -1;
    private EditBox codeBox;

    public ImportConfigScreen(Screen returnTo, Screen reopenParent) {
        super(Component.translatable("config.nourished.importExport.importTitle"));
        this.returnTo = returnTo;
        this.reopenParent = reopenParent;
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    private void switchTab(int tab) {
        if (sourceTab == 1 && codeBox != null) {
            codeDraft = codeBox.getValue();
        }
        sourceTab = tab;
        selectedFileIndex = -1;
        rebuild();
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        if (phase == Phase.CHOOSE) {
            initChoose(cx);
        } else {
            initPreview(cx);
        }
    }

    private void initChoose(int cx) {
        int y = this.height / 2 - 110;
        addRenderableWidget(Button.builder(tabLabel("config.nourished.importExport.tab.file", sourceTab == 0), b -> switchTab(0))
                .bounds(cx - 160, y, 155, 20)
                .build());
        addRenderableWidget(Button.builder(tabLabel("config.nourished.importExport.tab.code", sourceTab == 1), b -> switchTab(1))
                .bounds(cx + 5, y, 155, 20)
                .build());
        y += 28;

        if (sourceTab == 0) {
            try {
                exportFiles = ImportExportManager.listExportJsonFiles();
            } catch (Exception e) {
                Nourished.LOGGER.warn("[ImportConfigScreen] Failed to list exports", e);
                exportFiles = List.of();
            }
            int row = 0;
            int listTop = y;
            for (int i = 0; i < exportFiles.size() && row < 16; i++) {
                final int idx = i;
                Path p = exportFiles.get(i);
                boolean sel = idx == selectedFileIndex;
                Component line = Component.literal(sel ? "> " : "  ").append(Component.literal(p.getFileName().toString()));
                addRenderableWidget(Button.builder(line, b -> {
                            selectedFileIndex = idx;
                            rebuild();
                        })
                        .bounds(cx - 180, listTop + row * 22, 360, 20)
                        .build());
                row++;
            }
            y = listTop + Math.max(row, 1) * 22 + 8;
        } else {
            codeBox = new EditBox(this.font, cx - 180, y, 360, 20, Component.translatable("config.nourished.importExport.pasteCode"));
            codeBox.setMaxLength(524288);
            codeBox.setValue(codeDraft);
            codeBox.setResponder(s -> codeDraft = s);
            codeBox.setHint(Component.translatable("config.nourished.importExport.pasteCodeHint"));
            addRenderableWidget(codeBox);
            setInitialFocus(codeBox);
            y += 28;
        }

        int yBtn = Math.min(this.height / 2 + 85, y + 40);
        addRenderableWidget(
                Button.builder(Component.translatable("config.nourished.importExport.continue"), b -> tryParseAndPreview())
                        .bounds(cx - 160, yBtn, 155, 20)
                        .build());
        addRenderableWidget(
                Button.builder(Component.translatable("gui.cancel"), b -> Minecraft.getInstance().setScreen(returnTo))
                        .bounds(cx + 5, yBtn, 155, 20)
                        .build());
    }

    private static Component tabLabel(String key, boolean active) {
        MutableComponent c = Component.translatable(key);
        return active ? c.append(Component.literal(" \u2022")) : c;
    }

    private void tryParseAndPreview() {
        if (sourceTab == 1 && codeBox != null) {
            codeDraft = codeBox.getValue();
        }
        try {
            JsonObject root;
            if (sourceTab == 0) {
                if (exportFiles.isEmpty() || selectedFileIndex < 0 || selectedFileIndex >= exportFiles.size()) {
                    ImportExportToast.show(Component.translatable("config.nourished.importExport.pickFileFirst"));
                    return;
                }
                root = ImportExportManager.parseJsonFile(exportFiles.get(selectedFileIndex));
            } else {
                root = ImportExportManager.parseShareCode(codeDraft);
            }
            EnumSet<ImportExportManager.Section> found = ImportExportManager.sectionsPresent(root);
            if (found.isEmpty()) {
                ImportExportToast.show(Component.translatable("config.nourished.importExport.invalidBundle"));
                return;
            }
            this.pending = root;
            this.available = found;
            this.selectedSections = EnumSet.copyOf(found);
            this.phase = Phase.PREVIEW;
            rebuild();
        } catch (Exception e) {
            Nourished.LOGGER.warn("[ImportConfigScreen] Parse failed", e);
            ImportExportToast.show(Component.translatable("config.nourished.importExport.invalidCode"));
        }
    }

    private void initPreview(int cx) {
        int y = this.height / 2 - 100;
        for (ImportExportManager.Section s : ImportExportManager.Section.values()) {
            if (!available.contains(s)) {
                continue;
            }
            addRenderableWidget(
                    Button.builder(previewLine(s, selectedSections.contains(s)), b -> {
                                if (selectedSections.contains(s)) {
                                    selectedSections.remove(s);
                                } else {
                                    selectedSections.add(s);
                                }
                                if (selectedSections.isEmpty()) {
                                    selectedSections.add(s);
                                }
                                b.setMessage(previewLine(s, selectedSections.contains(s)));
                            })
                            .bounds(cx - 180, y, 360, 20)
                            .build());
            y += 22;
        }
        y += 10;
        addRenderableWidget(
                Button.builder(Component.translatable("config.nourished.importExport.applyImport"), b -> applySelected())
                        .bounds(cx - 160, y, 155, 20)
                        .build());
        addRenderableWidget(
                Button.builder(Component.translatable("config.nourished.importExport.back"), b -> {
                            phase = Phase.CHOOSE;
                            pending = null;
                            rebuild();
                        })
                        .bounds(cx + 5, y, 155, 20)
                        .build());
        y += 28;
        addRenderableWidget(
                Button.builder(Component.translatable("gui.cancel"), b -> Minecraft.getInstance().setScreen(returnTo))
                        .bounds(cx - 80, y, 160, 20)
                        .build());
    }

    private static Component previewLine(ImportExportManager.Section s, boolean on) {
        String mark = on ? "\u2713 " : "";
        return Component.literal(mark)
                .append(Component.translatable("config.nourished.importExport.section." + s.name().toLowerCase(Locale.ROOT)));
    }

    private void applySelected() {
        if (pending == null || selectedSections.isEmpty()) {
            ImportExportToast.show(Component.translatable("config.nourished.importExport.applyNothing"));
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        try {
            ImportExportManager.applyImport(pending, selectedSections);
            ImportExportToast.show(Component.translatable("config.nourished.importExport.importSuccess"));
            mc.setScreen(NourishedConfigScreen.create(reopenParent));
        } catch (Exception e) {
            Nourished.LOGGER.warn("[ImportConfigScreen] Apply import failed", e);
            ImportExportToast.show(Component.translatable("config.nourished.importExport.importFailed"));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, cx, this.height / 2 - 130, 0xFFFFFF);
        if (phase == Phase.CHOOSE && sourceTab == 0 && exportFiles.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("config.nourished.importExport.noExportFiles"),
                    cx,
                    this.height / 2 - 40,
                    0xA0A0A0);
        }
        if (phase == Phase.PREVIEW) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("config.nourished.importExport.previewHint"),
                    cx,
                    this.height / 2 - 120,
                    0xA0A0A0);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
