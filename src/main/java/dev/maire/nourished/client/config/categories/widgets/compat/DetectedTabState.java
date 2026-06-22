package dev.maire.nourished.client.config.categories.widgets.compat;

import com.mojang.blaze3d.platform.NativeImage;
import dev.maire.nourished.client.config.NourishedConfigScreen.CompatPending;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.NutrientClassificationLookup;
import dev.marie.MariesLib.compat.CompatCategory;
import dev.marie.MariesLib.compat.CompatEntry;
import dev.marie.MariesLib.compat.CompatReportEntry;
import dev.marie.MariesLib.compat.ModCompat;
import dev.marie.MariesLib.util.MarieRegistryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_CHIP_BORDER;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_CHIP_GREEN;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_CHIP_RED;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_CHIP_YELLOW;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_ROW_SEPARATOR;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_SUBTEXT;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.COL_TEXT;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.DETECTED_TOOLBAR_H;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.ROW_H;
import static dev.maire.nourished.client.config.categories.widgets.compat.CompatTabLayout.VIEWPORT_H;

public final class DetectedTabState {
    private final List<CompatReportEntry> detectedRows;
    private final Map<String, CompatEntry> builtInByModId;
    private final Runnable requestRebuild;
    private final Supplier<Boolean> editableSupplier;

    private final EditBox detectedSearchBox;
    private final Button detectedSortNameButton;
    private final Button detectedSortStatusButton;
    private final Button detectedSortCategoryButton;

    private final Map<String, int[]> detectedFoodCounts = new LinkedHashMap<>();
    private final Map<String, ResourceLocation> modLogoCache = new LinkedHashMap<>();

    private int scrollOffset;
    private long flashUntilMs;
    private String flashedModId;
    private int hoveredDetectedIndex = -1;
    private SortKey detectedSortKey = SortKey.DEFAULT;
    private boolean detectedNameDesc;
    private boolean detectedStatusMissingFirst;
    private boolean detectedCategoryDesc;
    private String lastSearchText = "";
    private boolean detectedFoodCountsComputed;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int detectedRowsY;
    private int detectedRowsH;
    private boolean isDraggingScrollbar;
    private double dragStartY;
    private int dragStartOffset;
    private int scrollbarTrackX;
    private int scrollbarTrackY;
    private int scrollbarTrackW;
    private int scrollbarTrackH;

    public DetectedTabState(
            Map<String, CompatPending> compatPending,
            Map<String, CompatEntry> builtInByModId,
            Runnable requestRebuild,
            Supplier<Boolean> editableSupplier
    ) {
        this.builtInByModId = builtInByModId;
        this.requestRebuild = requestRebuild;
        this.editableSupplier = editableSupplier;
        this.detectedRows = new ArrayList<>(ModCompat.getCompatReport());
        this.detectedSearchBox = new EditBox(
                Minecraft.getInstance().font,
                0,
                0,
                120,
                18,
                Component.literal("Search mods"));
        this.detectedSearchBox.setHint(Component.literal("Search name or mod id"));
        this.detectedSearchBox.setResponder(s -> {
            if (!s.equals(lastSearchText)) {
                lastSearchText = s;
                scrollOffset = 0;
                requestRebuild.run();
            }
        });
        this.detectedSortNameButton = Button.builder(Component.literal("Name \u2191"), b -> cycleDetectedSortName())
                .bounds(0, 0, 70, 18)
                .build();
        this.detectedSortStatusButton = Button.builder(Component.literal("Status"), b -> cycleDetectedSortStatus())
                .bounds(0, 0, 62, 18)
                .build();
        this.detectedSortCategoryButton = Button.builder(Component.literal("Category"), b -> cycleDetectedSortCategory())
                .bounds(0, 0, 78, 18)
                .build();
    }

    public void resetScrollOffset() {
        scrollOffset = 0;
    }

    public int getRowCount() {
        return detectedRows.size();
    }

    public int getBodyHeight() {
        return VIEWPORT_H;
    }

    public void hideWidgets() {
        detectedRowsY = -2000;
        detectedRowsH = 0;
        detectedSearchBox.setY(-2000);
        detectedSearchBox.setFocused(false);
        detectedSortNameButton.setY(-2000);
        detectedSortStatusButton.setY(-2000);
        detectedSortCategoryButton.setY(-2000);
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

        ensureDetectedFoodCountsComputed();
        detectedRowsY = listY + DETECTED_TOOLBAR_H;
        detectedRowsH = Math.max(0, listH - DETECTED_TOOLBAR_H);
        int toolbarX = listX + 4;
        int toolbarY = listY + 4;
        int toolbarW = Math.max(0, listW - 8);
        boolean narrowToolbar = toolbarW < 300;
        detectedSearchBox.setX(toolbarX);
        detectedSearchBox.setY(toolbarY);
        detectedSearchBox.setWidth(narrowToolbar ? toolbarW : Math.max(120, toolbarW - 220));
        detectedSearchBox.setHeight(18);
        detectedSearchBox.setEditable(editable);
        detectedSearchBox.render(graphics, mouseX, mouseY, delta);

        int btnY = toolbarY + 20;
        int btnGap = narrowToolbar ? 2 : 4;
        int sortBtnW = narrowToolbar
                ? Math.max(56, (toolbarW - btnGap * 2) / 3)
                : 78;
        int statusBtnW = narrowToolbar ? sortBtnW : 80;
        int categoryBtnW = narrowToolbar ? sortBtnW : 90;
        int btnX = toolbarX;
        detectedSortNameButton.setX(btnX);
        detectedSortNameButton.setY(btnY);
        detectedSortNameButton.setWidth(sortBtnW);
        detectedSortNameButton.active = editable;
        detectedSortNameButton.render(graphics, mouseX, mouseY, delta);
        detectedSortStatusButton.setX(btnX + sortBtnW + btnGap);
        detectedSortStatusButton.setY(btnY);
        detectedSortStatusButton.setWidth(statusBtnW);
        detectedSortStatusButton.active = editable;
        detectedSortStatusButton.render(graphics, mouseX, mouseY, delta);
        detectedSortCategoryButton.setX(btnX + sortBtnW + btnGap + statusBtnW + btnGap);
        detectedSortCategoryButton.setY(btnY);
        detectedSortCategoryButton.setWidth(categoryBtnW);
        detectedSortCategoryButton.active = editable;
        detectedSortCategoryButton.render(graphics, mouseX, mouseY, delta);

        graphics.enableScissor(listX, listY, listX + listW, listY + listH);
        renderDetectedRows(graphics, detectedRowsY, mouseX, mouseY);
        graphics.disableScissor();

        if (hoveredDetectedIndex >= 0) {
            List<CompatReportEntry> rows = filteredAndSortedDetectedRows();
            if (hoveredDetectedIndex < rows.size()) {
                CompatReportEntry row = rows.get(hoveredDetectedIndex);
                List<Component> tooltip = buildDetectedTooltip(row);
                graphics.renderTooltip(Minecraft.getInstance().font, tooltip, Optional.empty(), mouseX, mouseY);
            }
        }
    }

    public boolean handleMouseScrolled(double mouseX, double mouseY, double deltaY, int listX, int listW) {
        if (mouseX >= listX && mouseX < listX + listW
                && mouseY >= detectedRowsY && mouseY < detectedRowsY + detectedRowsH) {
            int visibleRowCount = detectedVisibleRows();
            int maxOffset = Math.max(0, filteredAndSortedDetectedRows().size() - visibleRowCount);
            int deltaSteps = (int) -deltaY;
            if (deltaSteps == 0 && deltaY != 0.0d) {
                deltaSteps = deltaY > 0 ? -1 : 1;
            }
            scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset + deltaSteps));
            requestRebuild.run();
            return true;
        }
        return false;
    }

    public boolean handleMouseClicked(double mouseX, double mouseY, int button, int listX, int listY, int listW, int listH) {
        this.listX = listX;
        this.listY = listY;
        this.listW = listW;
        this.listH = listH;

        if (button == 0 && scrollbarTrackW > 0
                && mouseX >= scrollbarTrackX && mouseX < scrollbarTrackX + scrollbarTrackW
                && mouseY >= scrollbarTrackY && mouseY < scrollbarTrackY + scrollbarTrackH) {
            isDraggingScrollbar = true;
            dragStartY = mouseY;
            dragStartOffset = scrollOffset;
            return true;
        }
        if (button == 0 && mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
            List<CompatReportEntry> rows = filteredAndSortedDetectedRows();
            int yStart = listY + DETECTED_TOOLBAR_H;
            int visibleRows = detectedVisibleRows();
            int startIndex = Math.max(0, Math.min(scrollOffset, Math.max(0, rows.size() - visibleRows)));
            int endIndex = Math.min(rows.size(), startIndex + visibleRows + 1);
            for (int i = startIndex; i < endIndex; i++) {
                int ry = yStart + (i - startIndex) * ROW_H;
                if (mouseY >= ry && mouseY < ry + ROW_H) {
                    CompatReportEntry row = rows.get(i);
                    Minecraft.getInstance().keyboardHandler.setClipboard(row.modId());
                    flashedModId = row.modId();
                    flashUntilMs = System.currentTimeMillis() + 500L;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean handleMouseDragged(double mouseX, double mouseY, int button) {
        if (isDraggingScrollbar && scrollbarTrackH > 0) {
            List<CompatReportEntry> rows = filteredAndSortedDetectedRows();
            int visibleRows = detectedVisibleRows();
            int totalRows = rows.size();
            int maxOffset = Math.max(0, totalRows - visibleRows);
            if (maxOffset > 0) {
                double dragDelta = mouseY - dragStartY;
                int newOffset = dragStartOffset + (int) (dragDelta / scrollbarTrackH * totalRows);
                scrollOffset = Math.max(0, Math.min(maxOffset, newOffset));
                requestRebuild.run();
            }
            return true;
        }
        return false;
    }

    public boolean handleMouseReleased(int button) {
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        return false;
    }

    public void addChildren(List<GuiEventListener> out) {
        out.add(detectedSearchBox);
        out.add(detectedSortNameButton);
        out.add(detectedSortStatusButton);
        out.add(detectedSortCategoryButton);
    }

    public void addNarratables(List<NarratableEntry> out) {
        out.add(detectedSearchBox);
        out.add(detectedSortNameButton);
        out.add(detectedSortStatusButton);
        out.add(detectedSortCategoryButton);
    }

    private void cycleDetectedSortName() {
        if (detectedSortKey != SortKey.NAME) {
            detectedSortKey = SortKey.NAME;
            detectedNameDesc = false;
        } else if (!detectedNameDesc) {
            detectedNameDesc = true;
        } else {
            detectedSortKey = SortKey.DEFAULT;
        }
        updateDetectedSortButtonLabels();
        requestRebuild.run();
    }

    private void cycleDetectedSortStatus() {
        if (detectedSortKey != SortKey.STATUS) {
            detectedSortKey = SortKey.STATUS;
            detectedStatusMissingFirst = false;
        } else if (!detectedStatusMissingFirst) {
            detectedStatusMissingFirst = true;
        } else {
            detectedSortKey = SortKey.DEFAULT;
        }
        updateDetectedSortButtonLabels();
        requestRebuild.run();
    }

    private void cycleDetectedSortCategory() {
        if (detectedSortKey != SortKey.CATEGORY) {
            detectedSortKey = SortKey.CATEGORY;
            detectedCategoryDesc = false;
        } else if (!detectedCategoryDesc) {
            detectedCategoryDesc = true;
        } else {
            detectedSortKey = SortKey.DEFAULT;
        }
        updateDetectedSortButtonLabels();
        requestRebuild.run();
    }

    private void updateDetectedSortButtonLabels() {
        String nameLabel = detectedSortKey == SortKey.NAME
                ? (detectedNameDesc ? "Name \u2193" : "Name \u2191")
                : "Name \u2191\u2193";
        String statusLabel = detectedSortKey == SortKey.STATUS
                ? (detectedStatusMissingFirst ? "Status M\u2192L" : "Status L\u2192M")
                : "Status";
        String categoryLabel = detectedSortKey == SortKey.CATEGORY
                ? (detectedCategoryDesc ? "Category \u2193" : "Category \u2191")
                : "Category";
        detectedSortNameButton.setMessage(Component.literal(nameLabel));
        detectedSortStatusButton.setMessage(Component.literal(statusLabel));
        detectedSortCategoryButton.setMessage(Component.literal(categoryLabel));
    }

    private List<CompatReportEntry> filteredAndSortedDetectedRows() {
        String q = detectedSearchBox.getValue() == null ? "" : detectedSearchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<CompatReportEntry> rows = new ArrayList<>();
        for (CompatReportEntry row : detectedRows) {
            String name = row.displayName() == null ? "" : row.displayName().toLowerCase(Locale.ROOT);
            String id = row.modId() == null ? "" : row.modId().toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !name.contains(q) && !id.contains(q)) {
                continue;
            }
            rows.add(row);
        }
        Comparator<CompatReportEntry> byName = Comparator.comparing(
                row -> row.displayName() == null ? row.modId() : row.displayName(),
                String.CASE_INSENSITIVE_ORDER);
        Comparator<CompatReportEntry> byModId = Comparator.comparing(CompatReportEntry::modId, String.CASE_INSENSITIVE_ORDER);
        Comparator<CompatReportEntry> byCategory = Comparator.comparing(
                row -> row.category() == null ? "" : row.category().name(),
                String.CASE_INSENSITIVE_ORDER);
        Comparator<CompatReportEntry> byStatus = Comparator.comparing(CompatReportEntry::loaded).reversed();
        Comparator<CompatReportEntry> sort = switch (detectedSortKey) {
            case NAME -> (detectedNameDesc ? byName.reversed() : byName).thenComparing(byModId);
            case STATUS -> {
                Comparator<CompatReportEntry> status = detectedStatusMissingFirst
                        ? Comparator.comparing(CompatReportEntry::loaded)
                        : byStatus;
                yield status.thenComparing(byName);
            }
            case CATEGORY -> (detectedCategoryDesc ? byCategory.reversed() : byCategory).thenComparing(byName);
            default -> byStatus.thenComparing(byName);
        };
        rows.sort(sort);
        return rows;
    }

    private int detectedVisibleRows() {
        int rowsArea = detectedRowsH > 0 ? detectedRowsH : Math.max(0, VIEWPORT_H - DETECTED_TOOLBAR_H);
        return Math.max(1, rowsArea / ROW_H);
    }

    private DetectedRowLayout detectedRowLayout(int countsBadgeWidth, int conflictBadgeWidth, boolean needsScrollbar) {
        int pad = 4;
        int iconSize = 16;
        int statusW = 54;
        int gap = 4;
        int scrollReserve = needsScrollbar ? 10 : 0;
        int countsReserve = countsBadgeWidth > 0 ? countsBadgeWidth + pad : 0;

        int iconX = listX + pad;
        int statusX = iconX + iconSize + gap;
        int textX = statusX + statusW + gap;
        int rightEdge = listX + listW - pad - scrollReserve;
        int countsX = countsReserve > 0 ? rightEdge - countsBadgeWidth : rightEdge;

        int minTextWidth = 28;
        int minWidthForConflict = textX + minTextWidth + gap + conflictBadgeWidth + gap + countsReserve + scrollReserve;
        boolean drawConflict = listW >= minWidthForConflict;
        int conflictX = drawConflict ? countsX - gap - conflictBadgeWidth : -1;
        int textRight = drawConflict ? conflictX - gap : countsX - (countsReserve > 0 ? gap : 0);
        int textMaxWidth = Math.max(minTextWidth, textRight - textX);
        boolean drawVersion = textMaxWidth >= 40 && listW >= 210;

        return new DetectedRowLayout(iconX, statusX, textX, textMaxWidth, conflictX, countsX, drawConflict, drawVersion);
    }

    private void renderDetectedRows(GuiGraphics graphics, int yStart, int mouseX, int mouseY) {
        List<CompatReportEntry> rows = filteredAndSortedDetectedRows();
        hoveredDetectedIndex = -1;
        int visibleRows = detectedVisibleRows();
        int startIndex = Math.max(0, Math.min(scrollOffset, Math.max(0, rows.size() - visibleRows)));
        int endIndex = Math.min(rows.size(), startIndex + visibleRows + 1);
        var font = Minecraft.getInstance().font;
        boolean needsScrollbar = rows.size() > visibleRows;
        for (int i = startIndex; i < endIndex; i++) {
            CompatReportEntry row = rows.get(i);
            int ry = yStart + (i - startIndex) * ROW_H;
            if (ry + ROW_H < listY || ry > listY + listH) {
                continue;
            }
            String category = resolvedDetectedCategory(row);
            int tint = getCategoryColor(category);
            graphics.fill(listX, ry, listX + listW, ry + ROW_H, tint);
            if (mouseX >= listX && mouseX < listX + listW && mouseY >= ry && mouseY < ry + ROW_H) {
                hoveredDetectedIndex = i;
            }
            if (flashedModId != null && flashedModId.equals(row.modId()) && System.currentTimeMillis() < flashUntilMs) {
                graphics.fill(listX + 1, ry + 1, listX + listW - 1, ry + ROW_H - 1, 0x33FFFFFF);
            }

            String countsBadge = detectedCountBadgeText(row);
            int countsBadgeWidth = countsBadge.isEmpty() ? 0 : font.width(countsBadge);
            int conflictBadgeWidth = conflictBadgeWidth(row.conflictLevel(), font);
            DetectedRowLayout layout = detectedRowLayout(countsBadgeWidth, conflictBadgeWidth, needsScrollbar);

            int iconY = ry + (ROW_H - 16) / 2;
            renderDetectedIcon(graphics, row, layout.iconX(), iconY);
            drawStatusChip(graphics, layout.statusX(), ry + 4, row.loaded(), row.conflictLevel().ordinal() > 0);

            String modName = CompatTabUi.ellipsize(font, CompatTabUi.toTitleCase(row.displayName()), layout.textMaxWidth());
            graphics.drawString(font, modName, layout.textX(), ry + 5, COL_TEXT, false);

            int detailY = layout.drawVersion() ? ry + 14 : ry + 8;
            if (layout.drawVersion()) {
                String version = CompatTabUi.ellipsize(font, "v" + detectedModVersion(row.modId()), layout.textMaxWidth());
                graphics.drawString(font, version, layout.textX(), ry + 14, COL_SUBTEXT, false);
            }
            if (layout.drawConflict()) {
                drawConflictBadge(graphics, layout.conflictX(), ry + 12, row.conflictLevel());
            }
            if (!countsBadge.isEmpty()) {
                int countsColor = detectedCountBadgeColor(row);
                graphics.drawString(font, countsBadge, layout.countsX(), detailY, countsColor, false);
            }
            graphics.fill(listX, ry + ROW_H - 1, listX + listW, ry + ROW_H, COL_ROW_SEPARATOR);
        }
        renderDetectedScrollIndicator(graphics, rows.size(), visibleRows);
    }

    private static int conflictBadgeWidth(dev.marie.MariesLib.compat.ConflictLevel level, net.minecraft.client.gui.Font font) {
        String text = switch (level) {
            case FULL_CONFLICT -> "FULL CONFLICT";
            case PARTIAL_CONFLICT -> "PARTIAL";
            default -> "NONE";
        };
        return Math.max(52, font.width(text) + 8);
    }

    private record DetectedRowLayout(
            int iconX,
            int statusX,
            int textX,
            int textMaxWidth,
            int conflictX,
            int countsX,
            boolean drawConflict,
            boolean drawVersion
    ) {}

    private void renderDetectedScrollIndicator(GuiGraphics graphics, int totalRows, int visibleRows) {
        if (detectedRowsH <= 0 || totalRows <= visibleRows) {
            scrollbarTrackX = -1;
            scrollbarTrackY = -1;
            scrollbarTrackW = 0;
            scrollbarTrackH = 0;
            return;
        }
        scrollbarTrackW = 6;
        scrollbarTrackX = listX + listW - scrollbarTrackW - 2;
        scrollbarTrackY = detectedRowsY + 1;
        scrollbarTrackH = detectedRowsH - 2;
        graphics.fill(scrollbarTrackX, scrollbarTrackY, scrollbarTrackX + scrollbarTrackW, scrollbarTrackY + scrollbarTrackH, 0x66383838);

        int maxOffset = Math.max(1, totalRows - visibleRows);
        int thumbH = Math.max(14, (int) (scrollbarTrackH * (visibleRows / (double) totalRows)));
        int thumbTravel = Math.max(1, scrollbarTrackH - thumbH);
        int thumbY = scrollbarTrackY + (int) (thumbTravel * (scrollOffset / (double) maxOffset));
        graphics.fill(scrollbarTrackX, thumbY, scrollbarTrackX + scrollbarTrackW, thumbY + thumbH, 0xCC5DA9DE);
        graphics.renderOutline(scrollbarTrackX - 1, scrollbarTrackY - 1, scrollbarTrackW + 2, scrollbarTrackH + 2, 0x884A4A4A);
    }

    private void renderDetectedIcon(GuiGraphics graphics, CompatReportEntry row, int x, int y) {
        ResourceLocation logo = resolveModLogo(row.modId()).orElse(null);
        if (logo != null) {
            graphics.blit(logo, x, y, 0, 0, 16, 16, 16, 16);
            return;
        }
        int fallback = getSolidCategoryColor(resolvedDetectedCategory(row));
        graphics.fill(x, y, x + 16, y + 16, fallback);
        graphics.renderOutline(x, y, 16, 16, 0xAA000000);
    }

    private Optional<ResourceLocation> resolveModLogo(String modId) {
        if (modLogoCache.containsKey(modId)) {
            return Optional.ofNullable(modLogoCache.get(modId));
        }

        Optional<? extends ModContainer> modContainer = ModList.get().getModContainerById(modId);
        if (modContainer.isEmpty()) {
            modLogoCache.put(modId, null);
            return Optional.empty();
        }
        var modInfo = modContainer.get().getModInfo();
        var modFile = modInfo.getOwningFile().getFile();

        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(
                Nourished.MODID, "mod_icon/" + modId.toLowerCase(Locale.ROOT));

        List<String> candidates = new ArrayList<>(7);
        Optional<String> declared = modInfo.getLogoFile();
        if (declared.isPresent() && !declared.get().isBlank()) {
            candidates.add(declared.get());
        }
        // Fallbacks for mods that ship an icon but don't declare it in mods.toml.
        candidates.add("icon.png");
        candidates.add("logo.png");
        candidates.add("pack.png");
        candidates.add("assets/" + modId + "/icon.png");
        candidates.add("assets/" + modId + "/logo.png");
        candidates.add("assets/" + modId + "/icon.PNG");

        for (String candidatePath : candidates) {
            try {
                Path logoPath = modFile.findResource(candidatePath);
                if (logoPath == null || !Files.exists(logoPath)) {
                    continue;
                }
                try (InputStream stream = Files.newInputStream(logoPath)) {
                    NativeImage image = NativeImage.read(stream);
                    Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(image));
                    modLogoCache.put(modId, rl);
                    return Optional.of(rl);
                }
            } catch (IOException | RuntimeException e) {
                Nourished.LOGGER.debug("[Compat Config] Failed to load mod logo for {} from {}: {}",
                        modId, candidatePath, e.getMessage());
            }
        }

        modLogoCache.put(modId, null);
        return Optional.empty();
    }

    private String resolvedDetectedCategory(CompatReportEntry row) {
        CompatEntry builtIn = builtInByModId.get(row.modId());
        if (builtIn != null && builtIn.category() != null) {
            if (builtIn.category() == CompatCategory.SOURCE_MOD) {
                return "food_mod";
            }
            return builtIn.category().name().toLowerCase(Locale.ROOT);
        }
        if (row.category() == null) {
            Nourished.LOGGER.debug("[Compat Config] Null category for {}, defaulting to FOOD_MOD", row.modId());
            return "food_mod";
        }
        if (row.category() == CompatCategory.SOURCE_MOD) {
            return "food_mod";
        }
        return row.category().name().toLowerCase(Locale.ROOT);
    }

    private int getCategoryColor(String category) {
        return switch (category) {
            case "food_mod" -> 0x332C7F2C;
            case "farming_mod" -> 0x339C7A18;
            case "survival_overhaul" -> 0x338A2F2F;
            default -> 0;
        };
    }

    private int getSolidCategoryColor(String category) {
        return switch (category) {
            case "food_mod" -> 0xFF2C7F2C;
            case "farming_mod" -> 0xFF9C7A18;
            case "survival_overhaul" -> 0xFF8A2F2F;
            default -> 0xFF4E5C6A;
        };
    }

    private String detectedModVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    private void drawConflictBadge(GuiGraphics graphics, int x, int y, dev.marie.MariesLib.compat.ConflictLevel level) {
        String text;
        int bgColor;
        int borderColor;
        if (level == dev.marie.MariesLib.compat.ConflictLevel.FULL_CONFLICT) {
            text = "FULL CONFLICT";
            bgColor = 0xFF6B1A1A;
            borderColor = 0xFF8A2F2F;
        } else if (level == dev.marie.MariesLib.compat.ConflictLevel.PARTIAL_CONFLICT) {
            text = "PARTIAL";
            bgColor = 0xFF7A5A00;
            borderColor = 0xFF9C7A18;
        } else {
            text = "NONE";
            bgColor = 0xFF333333;
            borderColor = 0xFF555555;
        }
        int chipW = Math.max(52, Minecraft.getInstance().font.width(text) + 8);
        int chipH = 12;
        graphics.fill(x, y, x + chipW, y + chipH, bgColor);
        graphics.renderOutline(x, y, chipW, chipH, borderColor);
        int textX = x + (chipW - Minecraft.getInstance().font.width(text)) / 2;
        int textY = y + (chipH - 8) / 2;
        graphics.drawString(Minecraft.getInstance().font, text, textX, textY, 0xFFFFFFFF, false);
    }

    private void ensureDetectedFoodCountsComputed() {
        if (detectedFoodCountsComputed) {
            return;
        }
        detectedFoodCountsComputed = true;
        for (CompatReportEntry row : detectedRows) {
            if (!row.loaded()) {
                continue;
            }
            int totalFood = 0;
            int classified = 0;
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation id = MarieRegistryUtils.itemKey(item);
                if (id == null || !row.modId().equals(id.getNamespace())) {
                    continue;
                }
                ItemStack stack = item.getDefaultInstance();
                FoodProperties food = FoodNutritionRegistry.foodPropertiesForNutrition(stack, null);
                if (food == null) {
                    continue;
                }
                totalFood++;
                Map<String, Float> bars = NutrientClassificationLookup.resolveBars(stack, Minecraft.getInstance().level);
                if (bars != null && !bars.isEmpty()) {
                    classified++;
                }
            }
            detectedFoodCounts.put(row.modId(), new int[]{classified, totalFood});
        }
    }

    private String detectedCountBadgeText(CompatReportEntry row) {
        if (!row.loaded()) return "";
        int[] counts = detectedFoodCounts.get(row.modId());
        if (counts == null) return "[0/0 classified]";
        return "[" + counts[0] + "/" + counts[1] + " classified]";
    }

    private int detectedCountBadgeColor(CompatReportEntry row) {
        int[] counts = detectedFoodCounts.get(row.modId());
        if (counts == null || counts[1] <= 0) return COL_SUBTEXT;
        double ratio = counts[0] / (double) counts[1];
        if (ratio >= 1.0d) return 0xFF72D172;
        if (ratio > 0.5d) return 0xFFE0C15C;
        return 0xFFDD7272;
    }

    private List<Component> buildDetectedTooltip(CompatReportEntry row) {
        List<Component> out = new ArrayList<>();
        CompatEntry builtIn = builtInByModId.get(row.modId());
        out.add(Component.literal(CompatTabUi.toTitleCase(row.displayName())).withStyle(s -> s.withBold(true)));
        out.add(Component.literal("ID: " + row.modId()));
        out.add(Component.literal("Version: " + detectedModVersion(row.modId())));
        out.add(Component.literal("Category: " + (row.category() == null ? "UNKNOWN" : row.category().name())));
        String namespaces = builtIn != null && builtIn.namespaces() != null && !builtIn.namespaces().isEmpty()
                ? String.join(", ", builtIn.namespaces())
                : row.modId();
        out.add(Component.literal("Namespaces: " + namespaces));
        String conflictSummary = "none";
        if (builtIn != null && builtIn.conflictBehavior() != null) {
            List<String> bits = new ArrayList<>();
            if (builtIn.conflictBehavior().disableEffects()) bits.add("effects disabled");
            if (builtIn.conflictBehavior().disableDecay()) bits.add("decay disabled");
            if (builtIn.conflictBehavior().disableMemory()) bits.add("memory disabled");
            if (builtIn.conflictBehavior().disableHud()) bits.add("hud disabled");
            if (!bits.isEmpty()) conflictSummary = String.join(", ", bits);
        }
        out.add(Component.literal("Conflict behavior: " + conflictSummary));
        int[] counts = detectedFoodCounts.get(row.modId());
        int foodCount = counts == null ? 0 : counts[1];
        out.add(Component.literal("Food item count: " + foodCount));
        return out;
    }

    private void drawStatusChip(GuiGraphics graphics, int x, int y, boolean loaded, boolean conflict) {
        int chipW = 54;
        int chipH = 14;
        int col = conflict ? COL_CHIP_YELLOW : (loaded ? COL_CHIP_GREEN : COL_CHIP_RED);
        String text = conflict ? "CONFLICT" : (loaded ? "LOADED" : "MISSING");
        graphics.fill(x, y, x + chipW, y + chipH, col);
        graphics.renderOutline(x, y, chipW, chipH, COL_CHIP_BORDER);
        graphics.drawString(Minecraft.getInstance().font, text, x + 4, y + 3, 0xFFF0F0F0, false);
    }

    enum SortKey {
        DEFAULT,
        NAME,
        STATUS,
        CATEGORY
    }
}
