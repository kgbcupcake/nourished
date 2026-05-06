package dev.maire.nourished.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.maire.nourished.Nourished;
import dev.maire.nourished.nutrition.NutrientRegistry;
import dev.maire.nourished.nutrition.UnassignedFoodScanner;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.LevelResource;

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
 * Cloth Config entry: scan unassigned foods (same rules as {@code /nourished get_unassigned_foods}),
 * reassign nutrients per row, and write generated datapack tags into the current singleplayer save.
 */
public final class FoodScannerWidget extends TooltipListEntry<Object> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int PAD = 4;
    private static final int ROW_H = 22;
    private static final int VISIBLE_ROWS = 9;
    private static final int LIST_VIEWPORT_H = VISIBLE_ROWS * ROW_H;
    private static final int HEADER_H = 28;

    private final Button scanButton;
    private final Button writeButton;
    private final List<Row> rows = new ArrayList<>();
    private boolean hasRunScan;

    private int scroll;
    private int lastListX;
    private int lastListY;
    private int lastListW;
    private int lastListH;

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
            rows.add(new Row(hit.itemId(), hit.fallbackNutrient(), keys));
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

        Path root = server.getWorldPath(LevelResource.ROOT);
        Path packRoot = root.resolve("datapacks").resolve("nourished-generated");
        Path tagsDir = packRoot.resolve("data").resolve("nourished").resolve("tags").resolve("item").resolve("nutrients");

        try {
            Files.createDirectories(tagsDir);
            writePackMeta(packRoot.resolve("pack.mcmeta"));

            for (Map.Entry<String, List<String>> e : byNutrient.entrySet()) {
                if (e.getValue().isEmpty()) {
                    continue;
                }
                Path tagFile = tagsDir.resolve(e.getKey() + ".json");
                writeTagFile(tagFile, e.getValue());
            }
        } catch (IOException ex) {
            Nourished.LOGGER.error("[FoodScannerWidget] Failed to write datapack", ex);
            return;
        }

        String levelName = server.getWorldData().getLevelName();
        String toastPath = "saves/" + levelName + "/datapacks/nourished-generated/";
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

        int y0 = cy - scroll;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int ry = y0 + i * ROW_H;
            if (ry + ROW_H < clipTop || ry > clipBottom) {
                row.nutrientButton.setY(-2000);
                continue;
            }
            String idStr = row.itemId.toString();
            int idMaxW = innerW - 200;
            if (mc.font.width(idStr) > idMaxW) {
                idStr = mc.font.plainSubstrByWidth(idStr, idMaxW - mc.font.width("...")) + "...";
            }
            graphics.drawString(mc.font, idStr, sx + 4, ry + 6, 0xFFFFFF, false);
            String fb = row.fallbackNutrient;
            Component fbLabel = Component.translatable("config.nourished.foodScanner.fallbackShort", fb);
            graphics.drawString(mc.font, fbLabel, sx + innerW / 2 - 40, ry + 6, 0xCCCCCC, false);

            int btnW = Math.min(110, innerW / 3);
            row.nutrientButton.setX(sx + innerW - btnW - 4);
            row.nutrientButton.setY(ry + 2);
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
    }

    private static final class Row {
        private final ResourceLocation itemId;
        private final String fallbackNutrient;
        private final List<String> keys;
        private String assignedNutrient;
        private final Button nutrientButton;

        Row(ResourceLocation itemId, String fallbackNutrient, List<String> keys) {
            this.itemId = itemId;
            this.fallbackNutrient = fallbackNutrient;
            this.keys = keys;
            this.assignedNutrient = fallbackNutrient;
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
