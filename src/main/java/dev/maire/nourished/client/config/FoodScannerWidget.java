package dev.maire.nourished.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.tooling.scanner.UnassignedFoodScanner;
import dev.maire.nourished.tooling.scanner.ClassificationResult;
import dev.maire.nourished.tooling.scanner.ClassificationSignal;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cloth Config entry: scan unassigned foods with full heuristic classification,
 * reassign nutrients per row, and write generated datapack tags into the current singleplayer save.
 */
public final class FoodScannerWidget extends TooltipListEntry<Object> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int PAD = 4;
    private static final int ROW_H = 26;
    private static final int VISIBLE_ROWS = 9;
    private static final int LIST_VIEWPORT_H = VISIBLE_ROWS * ROW_H;
    private static final int HEADER_H = 28;

    private static final int COLOR_CONFIDENT = 0xFF90EE90;
    private static final int COLOR_UNCERTAIN = 0xFFFFD700;
    private static final int COLOR_DEFAULT = 0xFFFFFFFF;

    private final Button scanButton;
    private final Button writeButton;
    private final List<Row> rows = new ArrayList<>();
    private boolean hasRunScan;

    private int scroll;
    private int lastListX;
    private int lastListY;
    private int lastListW;
    private int lastListH;

    @Nullable
    private Row hoveredRow;

    public FoodScannerWidget() {
        super(
                Component.translatable("config.nourished.foodScanner.title"),
                () -> Optional.of(new Component[]{Component.translatable("config.nourished.foodScanner.desc")}),
                false);
        this.scanButton = Button.builder(Component.translatable("config.nourished.foodScanner.scan"), b -> runScan())
                .bounds(0, 0, 120, 20)
                .build();
        this.writeButton = Button.builder(Component.translatable("config.nourished.foodScanner.writeDatapack"), b -> writeDatapack())
                .bounds(0, 0, 160, 20)
                .build();
    }

    private void runScan() {
        rows.clear();
        scroll = 0;
        hasRunScan = true;
        List<String> keys = NutrientRegistry.getKeys();

        for (UnassignedFoodScanner.ScanHit hit : UnassignedFoodScanner.scan()) {
            ClassificationResult result = hit.result();
            String dominant = result != null ? result.dominant() : hit.fallbackNutrient();
            boolean uncertain = result != null && result.uncertain();
            float spread = result != null ? result.confidenceSpread() : 0f;
            List<ClassificationSignal> signals = result != null ? result.topSignals(3) : List.of();

            rows.add(new Row(hit.itemId(), hit.fallbackNutrient(), dominant, uncertain, spread, signals, keys));
        }
        requestReferenceRebuilding();
    }

    private void writeDatapack() {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            mc.getToasts().addToast(new FoodScannerNoWorldToast());
            return;
        }
        if (rows.isEmpty()) {
            return;
        }

        Map<String, List<String>> byNutrient = new LinkedHashMap<>();
        for (String k : NutrientRegistry.getKeys()) {
            byNutrient.put(k, new ArrayList<>());
        }
        for (Row row : rows) {
            List<String> list = byNutrient.get(row.assignedNutrient);
            if (list != null) {
                list.add(row.itemId.toString());
            }
        }

        String levelName = mc.getLevelSource() != null ? mc.getLevelSource().getName() : server.getWorldData().getLevelName();
        Path packRoot = Path.of(
                FMLPaths.GAMEDIR.get().toAbsolutePath().toString(),
                "saves",
                levelName,
                "datapacks",
                "nourished-generated"
        );
        Path tagsDir = packRoot.resolve("data").resolve("nourished").resolve("tags").resolve("item").resolve("nutrients");
        String outputPath = packRoot.toAbsolutePath().toString();

        try {
            int writtenCount = 0;
            Path packMetaPath = packRoot.resolve("pack.mcmeta");
            Files.createDirectories(packMetaPath.getParent());
            writePackMeta(packMetaPath);
            writtenCount++;

            for (Map.Entry<String, List<String>> e : byNutrient.entrySet()) {
                if (e.getValue().isEmpty()) {
                    continue;
                }
                Path tagFile = tagsDir.resolve(e.getKey() + ".json");
                Files.createDirectories(tagFile.getParent());
                writeTagFile(tagFile, e.getValue());
                writtenCount++;
            }

            Nourished.LOGGER.info("[Nourished] Dumped {} files to {}", writtenCount, outputPath);
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("[Nourished] Dumped " + writtenCount + " files to " + outputPath)
                        .withStyle(ChatFormatting.GREEN), false);
            }
        } catch (IOException ex) {
            Nourished.LOGGER.error("[Nourished] Dump failed while writing scanner datapack output", ex);
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        Component.literal("[Nourished] Dump failed: " + ex.getMessage()).withStyle(ChatFormatting.RED),
                        false
                );
            }
            return;
        }

        String toastPath = packRoot.toAbsolutePath().toString();
        Component msg = Component.translatable("config.nourished.foodScanner.wroteToast", toastPath);
        mc.getToasts().addToast(new FoodScannerWriteToast(msg));
    }

    private static void writePackMeta(Path path) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 48);
        pack.addProperty("description", "Nourished auto-generated nutrient tags");
        root.add("pack", pack);
        try (Writer w = Files.newBufferedWriter(path)) {
            GSON.toJson(root, w);
        }
    }

    private static void writeTagFile(Path path, List<String> itemIds) throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("replace", false);
        JsonArray values = new JsonArray();
        for (String id : itemIds) {
            values.add(id);
        }
        obj.add("values", values);
        try (Writer w = Files.newBufferedWriter(path)) {
            GSON.toJson(obj, w);
        }
    }

    private boolean canScan() {
        return Minecraft.getInstance().level != null;
    }

    private boolean canWrite() {
        return Minecraft.getInstance().getSingleplayerServer() != null && !rows.isEmpty();
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
        int h = HEADER_H + PAD;
        if (!rows.isEmpty()) {
            h += LIST_VIEWPORT_H + PAD + 24;
        } else {
            h += 14;
            if (hasRunScan) {
                h += 2;
            }
        }
        return h;
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        List<net.minecraft.client.gui.components.events.GuiEventListener> out = new ArrayList<>();
        out.add(scanButton);
        out.add(writeButton);
        for (Row row : rows) {
            out.add(row.nutrientButton);
        }
        return out;
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        List<net.minecraft.client.gui.narration.NarratableEntry> out = new ArrayList<>();
        out.add(scanButton);
        out.add(writeButton);
        for (Row row : rows) {
            out.add(row.nutrientButton);
        }
        return out;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (rows.isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }
        if (mouseX >= lastListX && mouseX < lastListX + lastListW
                && mouseY >= lastListY && mouseY < lastListY + lastListH) {
            int maxScroll = Math.max(0, rows.size() * ROW_H - LIST_VIEWPORT_H);
            scroll = Mth.clamp(scroll - (int) (deltaY * ROW_H), 0, maxScroll);
            requestReferenceRebuilding();
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
            float delta) {
        if (!isRowVisible(y, getItemHeight())) {
            scanButton.setY(-2000);
            writeButton.setY(-2000);
            for (Row row : rows) {
                row.nutrientButton.setY(-2000);
            }
            return;
        }
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);

        Minecraft mc = Minecraft.getInstance();
        int innerW = entryWidth - 8;
        int sx = x + 4;
        int cy = y + 16;

        scanButton.setX(sx + innerW - 120);
        scanButton.setY(cy);
        scanButton.setWidth(120);
        scanButton.active = isEditable() && canScan();
        scanButton.render(graphics, mouseX, mouseY, delta);
        cy += 24;

        if (rows.isEmpty()) {
            Component hint = canScan()
                    ? (hasRunScan
                    ? Component.translatable("config.nourished.foodScanner.noResults")
                    : Component.translatable("config.nourished.foodScanner.emptyHint"))
                    : Component.translatable("config.nourished.foodScanner.noWorld");
            graphics.drawString(mc.font, hint, sx, cy, 0xA0A0A0, false);
            writeButton.active = false;
            writeButton.setY(-2000);
            return;
        }

        lastListX = sx;
        lastListY = cy;
        lastListW = innerW;
        lastListH = LIST_VIEWPORT_H;

        graphics.fill(sx, cy, sx + innerW, cy + LIST_VIEWPORT_H, 0x66000000);
        graphics.renderOutline(sx, cy, innerW, LIST_VIEWPORT_H, 0xFF404040);

        int clipTop = cy;
        int clipBottom = cy + LIST_VIEWPORT_H;
        graphics.enableScissor(sx, clipTop, sx + innerW, clipBottom);

        hoveredRow = null;
        int y0 = cy - scroll;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int ry = y0 + i * ROW_H;
            if (ry + ROW_H < clipTop || ry > clipBottom) {
                row.nutrientButton.setY(-2000);
                continue;
            }

            boolean rowHovered = mouseX >= sx && mouseX < sx + innerW && mouseY >= ry && mouseY < ry + ROW_H;
            if (rowHovered) {
                hoveredRow = row;
                graphics.fill(sx, ry, sx + innerW, ry + ROW_H, 0x33FFFFFF);
            }

            int textColor = row.uncertain ? COLOR_UNCERTAIN : COLOR_CONFIDENT;

            String idStr = row.itemId.toString();
            int idMaxW = innerW - 240;
            if (mc.font.width(idStr) > idMaxW) {
                idStr = mc.font.plainSubstrByWidth(idStr, idMaxW - mc.font.width("...")) + "...";
            }
            graphics.drawString(mc.font, idStr, sx + 4, ry + 4, textColor, false);

            String spreadStr = String.format("%.1f", row.confidenceSpread);
            String confidenceLabel = row.uncertain ? "?" : "✓";
            graphics.drawString(mc.font, confidenceLabel + " " + spreadStr, sx + 4, ry + 14, 0xAAAAAA, false);

            int btnW = Math.min(110, innerW / 3);
            row.nutrientButton.setX(sx + innerW - btnW - 4);
            row.nutrientButton.setY(ry + 4);
            row.nutrientButton.setWidth(btnW);
            row.nutrientButton.active = isEditable();
            row.nutrientButton.render(graphics, mouseX, mouseY, delta);
        }

        graphics.disableScissor();

        cy += LIST_VIEWPORT_H + PAD;
        writeButton.setX(sx);
        writeButton.setY(cy);
        writeButton.setWidth(Math.min(200, innerW));
        writeButton.active = isEditable() && canWrite();
        writeButton.render(graphics, mouseX, mouseY, delta);

        if (hoveredRow != null && !hoveredRow.topSignals.isEmpty()) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(hoveredRow.itemId.toString()).withStyle(s -> s.withBold(true)));
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("Classification: " + hoveredRow.assignedNutrient));
            tooltip.add(Component.literal("Confidence Spread: " + String.format("%.2f", hoveredRow.confidenceSpread)));
            tooltip.add(Component.literal(hoveredRow.uncertain ? "Status: Uncertain (needs review)" : "Status: Confident"));
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("Top Signals:").withStyle(s -> s.withUnderlined(true)));
            for (ClassificationSignal sig : hoveredRow.topSignals) {
                tooltip.add(Component.literal("  " + sig.signalType() + ": " + sig.source()));
            }

            graphics.renderTooltip(mc.font, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    private boolean isRowVisible(int y, int h) {
        if (!(Minecraft.getInstance().screen instanceof ClothConfigScreen cloth) || cloth.listWidget == null) {
            return true;
        }
        return y < cloth.listWidget.bottom && y + h > cloth.listWidget.top;
    }

    private static final class Row {
        private final ResourceLocation itemId;
        private final String fallbackNutrient;
        private final boolean uncertain;
        private final float confidenceSpread;
        private final List<ClassificationSignal> topSignals;
        private final List<String> keys;
        private String assignedNutrient;
        private final Button nutrientButton;

        Row(ResourceLocation itemId,
            String fallbackNutrient,
            String dominant,
            boolean uncertain,
            float confidenceSpread,
            List<ClassificationSignal> topSignals,
            List<String> keys) {
            this.itemId = itemId;
            this.fallbackNutrient = fallbackNutrient;
            this.uncertain = uncertain;
            this.confidenceSpread = confidenceSpread;
            this.topSignals = topSignals;
            this.keys = keys;
            this.assignedNutrient = dominant;
            this.nutrientButton = Button.builder(Component.literal(assignedNutrient), b -> cycle())
                    .bounds(0, 0, 100, 18)
                    .build();
        }

        private void cycle() {
            int idx = Math.max(0, keys.indexOf(assignedNutrient));
            assignedNutrient = keys.get((idx + 1) % keys.size());
            nutrientButton.setMessage(Component.literal(assignedNutrient));
        }
    }

    private static final class FoodScannerNoWorldToast implements Toast {
        private static final long DISPLAY_MS = 4000L;
        private final Component line = Component.translatable("config.nourished.foodScanner.noWorld");

        @Override
        public int width() {
            return Math.min(360, Math.max(200, Minecraft.getInstance().font.width(line) + 24));
        }

        @Override
        public int height() {
            return 32;
        }

        @Override
        public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long timeSinceLastVisible) {
            var font = toastComponent.getMinecraft().font;
            guiGraphics.fill(0, 0, width(), height(), 0xF0100010);
            guiGraphics.renderOutline(0, 0, width(), height(), 0xFF505078);
            guiGraphics.drawString(font, line, 8, 12, 0xFFFFFF, false);
            return timeSinceLastVisible >= DISPLAY_MS ? Visibility.HIDE : Visibility.SHOW;
        }
    }

    private static final class FoodScannerWriteToast implements Toast {
        private static final long DISPLAY_MS = 5000L;
        private final Component line;

        FoodScannerWriteToast(Component line) {
            this.line = line;
        }

        @Override
        public int width() {
            return Math.min(360, Math.max(200, Minecraft.getInstance().font.width(line) + 24));
        }

        @Override
        public int height() {
            return 32;
        }

        @Override
        public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long timeSinceLastVisible) {
            var font = toastComponent.getMinecraft().font;
            guiGraphics.fill(0, 0, width(), height(), 0xF0100010);
            guiGraphics.renderOutline(0, 0, width(), height(), 0xFF505078);
            guiGraphics.drawString(font, line, 8, 12, 0xFFFFFF, false);
            return timeSinceLastVisible >= DISPLAY_MS ? Visibility.HIDE : Visibility.SHOW;
        }
    }
}
