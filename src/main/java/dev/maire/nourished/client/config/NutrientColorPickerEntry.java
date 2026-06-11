package dev.maire.nourished.client.config;

import dev.marie.MariesLib.client.MarieValueColors;
import dev.marie.MariesLib.color.ColorRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cloth Config row: R/G/B sliders, hex field (bidirectional), and a small HSV color wheel; live-updates
 * {@link MarieValueColors#setOverride} for the HUD preview; {@link #save()} writes through {@link ColorRegistry}.
 */
public final class NutrientColorPickerEntry extends TooltipListEntry<Integer> {

    private static final int WHEEL_RADIUS = 22;
    private static final int SLIDER_W = 86;
    private static final int SLIDER_H = 20;
    private static final int ROW_H = SLIDER_H + 2;

    private final String key;
    private final int startPacked;
    private final boolean hadRegistryEntry;

    int r;
    int g;
    int b;

    private final ChannelSlider sliderR;
    private final ChannelSlider sliderG;
    private final ChannelSlider sliderB;
    private final EditBox hexBox;
    private boolean suppressHexResponder;

    private int wheelCx;
    private int wheelCy;

    public NutrientColorPickerEntry(String valueKey) {
        super(
                NutrientRegistry.getLabelComponent(valueKey),
                () -> Optional.of(new Component[]{Component.translatable("config.nourished.colors.picker.desc")}),
                false);
        this.key = valueKey;
        int initial = ColorRegistry.getArgb(valueKey)
                .orElseGet(() -> MarieValueColors.paletteOnlyArgb(valueKey));
        this.hadRegistryEntry = ColorRegistry.getArgb(valueKey).isPresent();
        this.r = (initial >> 16) & 0xFF;
        this.g = (initial >> 8) & 0xFF;
        this.b = initial & 0xFF;
        this.startPacked = packArgb();

        this.sliderR = new ChannelSlider(0, 0, SLIDER_W, SLIDER_H, 0);
        this.sliderG = new ChannelSlider(0, 0, SLIDER_W, SLIDER_H, 1);
        this.sliderB = new ChannelSlider(0, 0, SLIDER_W, SLIDER_H, 2);
        this.hexBox = new EditBox(Minecraft.getInstance().font, 0, 0, 168, 18, Component.translatable("config.nourished.colors.hex"));
        this.hexBox.setMaxLength(32);
        this.hexBox.setFilter(NutrientColorPickerEntry::hexFilter);
        this.hexBox.setResponder(this::onHexChanged);
        refreshHexBoxProgrammatically();
        pushLivePreview();
    }

    private static boolean hexFilter(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F')
                    || c == '#'
                    || c == 'x'
                    || c == 'X') {
                continue;
            }
            return false;
        }
        return true;
    }

    private void onHexChanged(String s) {
        if (suppressHexResponder) {
            return;
        }
        Optional<int[]> parsed = tryParseRgb(s);
        if (parsed.isEmpty()) {
            return;
        }
        int[] rgb = parsed.get();
        this.r = rgb[0];
        this.g = rgb[1];
        this.b = rgb[2];
        sliderR.syncFromRgb();
        sliderG.syncFromRgb();
        sliderB.syncFromRgb();
        pushLivePreview();
    }

    private void refreshHexBoxProgrammatically() {
        suppressHexResponder = true;
        hexBox.setValue(String.format("0x%08X", packArgb()));
        suppressHexResponder = false;
    }

    private void onRgbChangedFromSlider() {
        refreshHexBoxProgrammatically();
        pushLivePreview();
    }

    private void pushLivePreview() {
        MarieValueColors.setOverride(key, packArgb());
    }

    private int packArgb() {
        return 0xFF00_0000 | (Mth.clamp(r, 0, 255) << 16) | (Mth.clamp(g, 0, 255) << 8) | Mth.clamp(b, 0, 255);
    }

    private static Optional<int[]> tryParseRgb(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return Optional.empty();
        }
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) {
                int argb = Integer.decode(s);
                int rr = (argb >> 16) & 0xFF;
                int gg = (argb >> 8) & 0xFF;
                int bb = argb & 0xFF;
                return Optional.of(new int[]{rr, gg, bb});
            }
            if (s.startsWith("#")) {
                s = s.substring(1);
            }
            if (s.length() == 6 && s.chars().allMatch(ch -> isHexDigit((char) ch))) {
                int v = Integer.parseInt(s, 16);
                return Optional.of(new int[]{(v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF});
            }
            if (s.length() == 8 && s.chars().allMatch(ch -> isHexDigit((char) ch))) {
                int v = (int) Long.parseLong(s, 16);
                return Optional.of(new int[]{(v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF});
            }
        } catch (NumberFormatException ignored) {
        }
        return Optional.empty();
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    @Override
    public Integer getValue() {
        return packArgb();
    }

    @Override
    public Optional<Integer> getDefaultValue() {
        return Optional.of(startPacked);
    }

    @Override
    public boolean isEdited() {
        return packArgb() != startPacked;
    }

    @Override
    public void save() {
        int now = packArgb();
        int pal = MarieValueColors.paletteOnlyArgb(key);
        if (now == pal && !hadRegistryEntry) {
            return;
        }
        if (now == pal) {
            ColorRegistry.remove(key);
        } else {
            ColorRegistry.setArgb(key, now);
        }
    }

    @Override
    public int getItemHeight() {
        return ROW_H + 28 + WHEEL_RADIUS * 2 + 24;
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        List<net.minecraft.client.gui.components.events.GuiEventListener> list = new ArrayList<>();
        list.add(sliderR);
        list.add(sliderG);
        list.add(sliderB);
        list.add(hexBox);
        return list;
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return List.of(sliderR, sliderG, sliderB, hexBox);
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

        int sy = y + 14;
        int sx = x + 4;
        sliderR.setX(sx);
        sliderR.setY(sy);
        sliderG.setX(sx + SLIDER_W + 4);
        sliderG.setY(sy);
        sliderB.setX(sx + (SLIDER_W + 4) * 2);
        sliderB.setY(sy);

        sliderR.setWidth(SLIDER_W);
        sliderG.setWidth(SLIDER_W);
        sliderB.setWidth(SLIDER_W);
        sliderR.setHeight(SLIDER_H);
        sliderG.setHeight(SLIDER_H);
        sliderB.setHeight(SLIDER_H);

        int syHex = sy + ROW_H + 4;
        hexBox.setX(sx);
        hexBox.setY(syHex);
        hexBox.setWidth(Mth.clamp(entryWidth - SLIDER_W * 3 - 24 - WHEEL_RADIUS * 2, 100, 220));
        hexBox.setHeight(18);

        wheelCx = x + entryWidth - WHEEL_RADIUS - 8;
        wheelCy = sy + SLIDER_H / 2;
        drawColorWheel(graphics, wheelCx - WHEEL_RADIUS, wheelCy - WHEEL_RADIUS, WHEEL_RADIUS * 2);

        sliderR.active = isEditable();
        sliderG.active = isEditable();
        sliderB.active = isEditable();
        hexBox.setEditable(isEditable());

        sliderR.render(graphics, mouseX, mouseY, delta);
        sliderG.render(graphics, mouseX, mouseY, delta);
        sliderB.render(graphics, mouseX, mouseY, delta);
        hexBox.render(graphics, mouseX, mouseY, delta);

        graphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("config.nourished.colors.hex").getString(),
                sx,
                syHex - 10,
                getPreferredTextColor(),
                false);
    }

    private static void drawColorWheel(GuiGraphics g, int ox, int oy, int size) {
        int r = size / 2;
        int cx = ox + r;
        int cy = oy + r;
        for (int py = -r; py <= r; py++) {
            for (int px = -r; px <= r; px++) {
                double dist = Math.hypot(px, py);
                if (dist > r) {
                    continue;
                }
                float sat = (float) Mth.clamp(dist / r, 0f, 1f);
                float hue = (float) ((Math.atan2(py, px) + Math.PI) / (Math.PI * 2));
                int rgb = hsbToRgb(hue, sat, 1f);
                g.fill(cx + px, cy + py, cx + px + 1, cy + py + 1, 0xFF000000 | rgb);
            }
        }
        g.renderOutline(ox, oy, size, size, 0xFF404040);
    }

    /** HSV (h,s,v each 0..1) to RGB 0x00RRGGBB (no alpha). */
    private static int hsbToRgb(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        if (h < 0) {
            h += 1f;
        }
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - s * f);
        float t = v * (1 - s * (1 - f));
        float rr = 0, gg = 0, bb = 0;
        switch (i % 6) {
            case 0 -> {
                rr = v;
                gg = t;
                bb = p;
            }
            case 1 -> {
                rr = q;
                gg = v;
                bb = p;
            }
            case 2 -> {
                rr = p;
                gg = v;
                bb = t;
            }
            case 3 -> {
                rr = p;
                gg = q;
                bb = v;
            }
            case 4 -> {
                rr = t;
                gg = p;
                bb = v;
            }
            case 5 -> {
                rr = v;
                gg = p;
                bb = q;
            }
            default -> {
            }
        }
        int R = Mth.clamp((int) (rr * 255f), 0, 255);
        int G = Mth.clamp((int) (gg * 255f), 0, 255);
        int B = Mth.clamp((int) (bb * 255f), 0, 255);
        return (R << 16) | (G << 8) | B;
    }

    private boolean pickWheel(int mouseX, int mouseY) {
        int lx = mouseX - wheelCx;
        int ly = mouseY - wheelCy;
        double dist = Math.hypot(lx, ly);
        if (dist > WHEEL_RADIUS) {
            return false;
        }
        float sat = (float) Mth.clamp(dist / WHEEL_RADIUS, 0f, 1f);
        float hue = (float) ((Math.atan2(ly, lx) + Math.PI) / (Math.PI * 2));
        int rgb = hsbToRgb(hue, sat, 1f);
        this.r = (rgb >> 16) & 0xFF;
        this.g = (rgb >> 8) & 0xFF;
        this.b = rgb & 0xFF;
        sliderR.syncFromRgb();
        sliderG.syncFromRgb();
        sliderB.syncFromRgb();
        refreshHexBoxProgrammatically();
        pushLivePreview();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && pickWheel((int) mouseX, (int) mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && pickWheel((int) mouseX, (int) mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private final class ChannelSlider extends AbstractSliderButton {
        private final int channelIndex;

        ChannelSlider(int x, int y, int w, int h, int channelIndex) {
            super(x, y, w, h, Component.empty(), 0);
            this.channelIndex = channelIndex;
            syncFromRgb();
        }

        void syncFromRgb() {
            int v = switch (channelIndex) {
                case 0 -> r;
                case 1 -> g;
                default -> b;
            };
            this.value = v / 255.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int v = Mth.clamp((int) Math.round(value * 255.0), 0, 255);
            String label = switch (channelIndex) {
                case 0 -> "R";
                case 1 -> "G";
                default -> "B";
            };
            setMessage(Component.literal(label + ": " + v));
        }

        @Override
        protected void applyValue() {
            int v = Mth.clamp((int) Math.round(value * 255.0), 0, 255);
            switch (channelIndex) {
                case 0 -> NutrientColorPickerEntry.this.r = v;
                case 1 -> NutrientColorPickerEntry.this.g = v;
                default -> NutrientColorPickerEntry.this.b = v;
            }
            onRgbChangedFromSlider();
        }
    }
}
