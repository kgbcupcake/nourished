package dev.maire.nourished.client.config;

import com.google.gson.JsonObject;

import dev.maire.nourished.core.Nourished;
import dev.marie.framework.client.ImportExportToast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.EnumSet;

/**
 * Checklist of exportable sections, then writes JSON and opens the share-code dialog.
 */
public final class ExportConfigScreen extends Screen {

    private final Screen returnTo;
    private final Screen reopenParent;
    private final boolean[] sectionOn = new boolean[NourishedImportExport.Section.values().length];

    public ExportConfigScreen(Screen returnTo, Screen reopenParent) {
        super(Component.translatable("config.nourished.importExport.exportTitle"));
        this.returnTo = returnTo;
        this.reopenParent = reopenParent;
        for (int i = 0; i < sectionOn.length; i++) {
            sectionOn[i] = true;
        }
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int y0 = this.height / 2 - 90;
        NourishedImportExport.Section[] secs = NourishedImportExport.Section.values();
        for (int i = 0; i < secs.length; i++) {
            final int idx = i;
            NourishedImportExport.Section s = secs[i];
            addRenderableWidget(
                    Button.builder(sectionLabel(s, sectionOn[idx]), b -> {
                                sectionOn[idx] = !sectionOn[idx];
                                b.setMessage(sectionLabel(s, sectionOn[idx]));
                            })
                            .bounds(cx - 160, y0 + i * 22, 320, 20)
                            .build());
        }
        int yBtn = y0 + secs.length * 22 + 12;
        addRenderableWidget(
                Button.builder(Component.translatable("config.nourished.importExport.exportAction"), b -> doExport())
                        .bounds(cx - 160, yBtn, 155, 20)
                        .build());
        addRenderableWidget(
                Button.builder(Component.translatable("gui.cancel"), b -> Minecraft.getInstance().setScreen(returnTo))
                        .bounds(cx + 5, yBtn, 155, 20)
                        .build());
    }

    private static Component sectionLabel(NourishedImportExport.Section s, boolean on) {
        String mark = on ? "\u2713 " : "";
        return Component.literal(mark).append(Component.translatable("config.nourished.importExport.section." + s.labelKey()));
    }

    private void doExport() {
        EnumSet<NourishedImportExport.Section> sel = EnumSet.noneOf(NourishedImportExport.Section.class);
        NourishedImportExport.Section[] secs = NourishedImportExport.Section.values();
        for (int i = 0; i < secs.length; i++) {
            if (sectionOn[i]) {
                sel.add(secs[i]);
            }
        }
        if (sel.isEmpty()) {
            ImportExportToast.show(Component.translatable("config.nourished.importExport.exportNothing"));
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        try {
            JsonObject root = NourishedImportExport.buildExportRoot(sel);
            Path file = NourishedImportExport.writeExportFile(root);
            String share = NourishedImportExport.buildShareCode(root);
            String rel = "config/nourished/exports/" + file.getFileName();
            ImportExportToast.show(Component.translatable("config.nourished.importExport.exportedFileToast", rel));
            mc.setScreen(new ExportResultScreen(reopenParent, rel, share));
        } catch (Exception e) {
            Nourished.LOGGER.warn("[ExportConfigScreen] Export failed", e);
            ImportExportToast.show(Component.translatable("config.nourished.importExport.exportFailed"));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 110, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
