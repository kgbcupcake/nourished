package dev.maire.nourished.client.config;

import dev.maire.nourished.client.NutrientUiColors;
import dev.maire.nourished.core.color.ColorRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Cloth Config row: 10×10 ARGB swatch, nutrient label, {@code #RRGGBB} field with validation, reset, and live HUD preview.
 */
public final class NutrientHudHexColorRowEntry extends TooltipListEntry<Integer> {

    public static final int SWATCH = 10;

    private final String key;
    private final Component nutrientLabel;
    private final int startArgb;
    private final String initialHex;
    private final EditBox hexBox;
    private final Button resetButton;
    private boolean suppressHexResponder;

    public NutrientHudHexColorRowEntry(String nutrientKey) {
        super(Component.empty(), () -> Optional.of(new Component[]{
                Component.translatable("config.nourished.hudColors.row.tooltip")
        }), false);
        this.key = nutrientKey;
        this.nutrientLabel = Component.translatable("nourished.screen.diet.bar." + nutrientKey);
        this.startArgb = ColorRegistry.getArgb(nutrientKey)
                .orElseGet(() -> NutrientUiColors.baseColorArgb(nutrientKey));
        this.initialHex = formatRgbHex(startArgb);
        this.hexBox = new EditBox(Minecraft.getInstance().font, 0, 0, 96, 18, Component.translatable("config.nourished.hudColors.hex"));
        this.hexBox.setMaxLength(7);
        this.hexBox.setFilter(NutrientHudHexColorRowEntry::hexInputFilter);
        this.hexBox.setHint(Component.translatable("config.nourished.hudColors.hexHint"));
        this.hexBox.setValue(initialHex);
        this.hexBox.setResponder(this::onHexChanged);
        this.resetButton = Button.builder(Component.translatable("config.nourished.hudColors.reset"), b -> onReset())
                .bounds(0, 0, 56, 18)
                .build();
        setErrorSupplier(this::computeError);
        pushLivePreview();
    }

    private static boolean hexInputFilter(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '#' || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
                continue;
            }
            return false;
        }
        return true;
    }

    private Optional<Component> computeError() {
        if (parseStrictRgbHex(hexBox.getValue()).isPresent()) {
            return Optional.empty();
        }
        if (hexBox.getValue().trim().isEmpty()) {
            return Optional.of(Component.translatable("config.nourished.hudColors.error.empty"));
        }
        return Optional.of(Component.translatable("config.nourished.hudColors.error.invalid"));
    }

    private void onHexChanged(String s) {
        if (suppressHexResponder) {
            return;
        }
        pushLivePreview();
        requestReferenceRebuilding();
    }

    private void pushLivePreview() {
        Optional<Integer> parsed = parseStrictRgbHex(hexBox.getValue());
        if (parsed.isPresent()) {
            NutrientUiColors.setOverride(key, parsed.get());
        } else {
            NutrientUiColors.setOverride(key, null);
        }
    }

    private void onReset() {
        ColorRegistry.remove(key);
        syncHexFromEffectiveColor();
        pushLivePreview();
        requestReferenceRebuilding();
    }

    /**
     * After {@link ColorRegistry#remove} for all keys (reset-all); re-read effective colors into each row.
     */
    public void syncAfterBulkReset() {
        syncHexFromEffectiveColor();
        pushLivePreview();
    }

    private void syncHexFromEffectiveColor() {
        int effective = ColorRegistry.getArgb(key).orElseGet(() -> NutrientUiColors.paletteOnlyArgb(key));
        suppressHexResponder = true;
        hexBox.setValue(formatRgbHex(effective));
        suppressHexResponder = false;
    }

    private static String formatRgbHex(int argb) {
        int rgb = argb & 0xFF_FF_FF;
        return String.format(Locale.ROOT, "#%06X", rgb);
    }

    /**
     * Strict {@code #RRGGBB} (hash required, exactly six hex digits).
     */
    static Optional<Integer> parseStrictRgbHex(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String s = raw.trim();
        if (s.length() != 7 || s.charAt(0) != '#') {
            return Optional.empty();
        }
        for (int i = 1; i < 7; i++) {
            if (!isHexDigit(s.charAt(i))) {
                return Optional.empty();
            }
        }
        try {
            int rgb = Integer.parseInt(s.substring(1), 16);
            return Optional.of(0xFF00_0000 | rgb);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private int previewArgb() {
        return parseStrictRgbHex(hexBox.getValue()).orElse(startArgb);
    }

    @Override
    public Integer getValue() {
        return parseStrictRgbHex(hexBox.getValue()).orElse(startArgb);
    }

    @Override
    public Optional<Integer> getDefaultValue() {
        return Optional.of(startArgb);
    }

    @Override
    public boolean isEdited() {
        Optional<Integer> cur = parseStrictRgbHex(hexBox.getValue());
        if (cur.isEmpty()) {
            return !hexBox.getValue().trim().equalsIgnoreCase(initialHex.trim());
        }
        int rgbA = cur.get() & 0x00_FF_FF_FF;
        int rgbB = startArgb & 0x00_FF_FF_FF;
        return rgbA != rgbB;
    }

    @Override
    public void save() {
        Optional<Integer> parsed = parseStrictRgbHex(hexBox.getValue());
        if (parsed.isEmpty()) {
            return;
        }
        int now = parsed.get();
        int pal = NutrientUiColors.paletteOnlyArgb(key);
        int nowRgb = now & 0x00_FF_FF_FF;
        int palRgb = pal & 0x00_FF_FF_FF;
        if (nowRgb == palRgb) {
            ColorRegistry.remove(key);
        } else {
            ColorRegistry.setArgb(key, now);
        }
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
            float delta
    ) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);

        int cy = y + (entryHeight - SWATCH) / 2;
        int sx = x + 6;
        int argb = previewArgb();
        graphics.fill(sx, cy, sx + SWATCH, cy + SWATCH, argb);
        graphics.renderOutline(sx, cy, SWATCH, SWATCH, 0xFF404040);

        var font = Minecraft.getInstance().font;
        int nameX = sx + SWATCH + 6;
        graphics.drawString(font, nutrientLabel, nameX, y + 6, getPreferredTextColor(), false);

        boolean invalid = parseStrictRgbHex(hexBox.getValue()).isEmpty();

        int resetW = 56;
        int hexW = Mth.clamp(entryWidth - 24 - SWATCH - 6 - 8 - resetW - 8, 72, 120);
        int resetX = x + entryWidth - resetW - 6;
        int hexX = resetX - 6 - hexW;

        hexBox.setX(hexX);
        hexBox.setY(y + 3);
        hexBox.setWidth(hexW);
        hexBox.setHeight(18);
        hexBox.setEditable(isEditable());

        resetButton.setX(hexX + hexW + 6);
        resetButton.setY(y + 3);
        resetButton.active = isEditable();

        if (invalid) {
            int border = 0xFFFF3333;
            graphics.renderOutline(hexX - 1, y + 2, hexW + 2, 20, border);
        }

        hexBox.render(graphics, mouseX, mouseY, delta);
        resetButton.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        List<net.minecraft.client.gui.components.events.GuiEventListener> list = new ArrayList<>();
        list.add(hexBox);
        list.add(resetButton);
        return list;
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return List.of(hexBox, resetButton);
    }
}
