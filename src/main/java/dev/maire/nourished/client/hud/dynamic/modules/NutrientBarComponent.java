package dev.maire.nourished.client.hud.dynamic.modules;

import dev.marie.framework.ui.api.MarieModuleSettings;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.ui.geometry.Anchor;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.geometry.Insets;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.geometry.Size;
import dev.marie.framework.ui.VisibilityRule;
import dev.marie.framework.ui.visibility.AnyOf;
import dev.marie.framework.ui.visibility.ConfigToggleVisibility;
import dev.marie.framework.ui.visibility.ThresholdVisibility;
import dev.maire.nourished.client.hud.dynamic.HudDrawHelpers;
import dev.maire.nourished.client.hud.dynamic.layout.HudLayout;
import dev.maire.nourished.client.hud.dynamic.visibility.HudVisibilityRules;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * First MarieUI consumer for a single nutrient row/column. Row geometry is not recomputed here —
 * it's taken from {@link HudLayout.Layout}, which stays the single source of truth for bar sizing.
 */
final class NutrientBarComponent implements MarieComponent {

    private final String nutrientKey;
    private final boolean verticalMode;
    private final HudLayout.Layout hudLayout;
    private final Map<String, Float> displayValues;

    /**
     * Text/icon render scale — the user's persisted per-panel adjustment alone (via {@link
     * dev.marie.framework.ui.edit.ContentScaleController#resolveContentScale}, resolved once by
     * {@code NutrientPanelContainer}), never {@code hudLayout}'s own scale. {@code hudLayout} still
     * drives every position/spacing value below (row height, icon/bar/label offsets, this row's own
     * {@link #bounds() slot}) — same separation the other 7 {@code ContentScaleController}-managed
     * modules maintain.
     */
    private final float contentScale;
    /** Icon size multiplier — independent of {@link #contentScale}, which sizes the text (see {@code MarieModuleSettings#iconScale}). */
    private final float iconScale;
    /** "Move Text and Icons" offset — moves the icon and the name label. */
    private final int textDx;
    private final int textDy;
    /** "Move Icons" offset — moves the icon. */
    private final int iconDx;
    private final int iconDy;
    /** "Move Bars" offset — moves the bar and its percentage text. */
    private final int barDx;
    private final int barDy;
    /** Bar size multiplier — scales the bar and, with it, the percentage text at its end (the text size does not touch that number). */
    private final float barScale;

    NutrientBarComponent(String nutrientKey, boolean verticalMode, HudLayout.Layout hudLayout, Map<String, Float> displayValues, float contentScale, float iconScale,
                          int textDx, int textDy, int iconDx, int iconDy, int barDx, int barDy, float barScale) {
        this.nutrientKey = nutrientKey;
        this.verticalMode = verticalMode;
        this.hudLayout = hudLayout;
        this.displayValues = displayValues;
        this.contentScale = contentScale;
        this.iconScale = iconScale;
        this.textDx = textDx;
        this.textDy = textDy;
        this.iconDx = iconDx;
        this.iconDy = iconDy;
        this.barDx = barDx;
        this.barDy = barDy;
        this.barScale = barScale;
    }

    @Override
    public String id() {
        return "nourished.hud.bar." + nutrientKey;
    }

    @Override
    public Constraint constraint() {
        if (verticalMode) {
            int textH = (int) Math.ceil(9 * hudLayout.labelScale());
            int contentH = textH + 2 + hudLayout.verticalBarH() + 2 + textH;
            // CENTER so HorizontalLayout vertically centers each column when the box is taller than
            // content needs, same reasoning as the horizontal-mode CENTER below.
            return Constraint.fixed(hudLayout.verticalColumnW(), contentH).withAnchor(Anchor.CENTER);
        }
        // naturalPanelW, not panelW: the on-screen box can be freely resized wider than content
        // needs (see HudEditTarget) without rescaling content, so content must size itself from its
        // own natural (scale-only) width, not whatever the box currently measures. CENTER anchor lets
        // VerticalLayout's existing horizontal-centering offset place each row in the middle of
        // whatever width is actually available, instead of pinning it flush to the left.
        int contentW = hudLayout.naturalPanelW() - hudLayout.scaledPad() * 2;
        Size preferred = new Size(contentW, hudLayout.rowH());
        Size minSize = new Size(0, hudLayout.rowH());
        Size maxSize = new Size(Integer.MAX_VALUE, hudLayout.rowH());
        return new Constraint(preferred, minSize, maxSize, false, false, true, false,
                Anchor.CENTER, Insets.NONE, Insets.NONE);
    }

    /**
     * Wraps HudVisibility/HudVisibilityRules' hide/show-above thresholds, OR'd with the nutrient-gain
     * flash window (via {@link MarieClientCache#flashAlpha}) so a bar temporarily reveals itself when
     * its value just increased, even while otherwise threshold-hidden.
     */
    @Override
    public VisibilityRule visibilityRule() {
        NourishedClientConfig cc = NourishedClientConfig.get();
        Float hideAtOrAbove = activeThreshold((float) cc.hudHideAboveThreshold());
        Float showAtOrAbove = activeThreshold((float) cc.hudShowAboveThreshold());
        VisibilityRule threshold = new ThresholdVisibility<>(
                () -> MarieClientCache.get().values.getOrDefault(nutrientKey, 0f),
                hideAtOrAbove,
                showAtOrAbove
        );
        if (!cc.hudRevealOnNutrientGain()) {
            return threshold;
        }
        VisibilityRule flashing = new ConfigToggleVisibility(() -> MarieClientCache.flashAlpha(nutrientKey) > 0f);
        return new AnyOf(threshold, flashing);
    }

    private static Float activeThreshold(float raw) {
        float clamped = Math.max(0f, Math.min(1f, raw));
        return clamped < 1f - HudVisibilityRules.ZERO_EPSILON ? clamped : null;
    }

    /** Translucent white highlight over the bar while {@link MarieClientCache#flashAlpha} is decaying, matching the pre-MarieUI legacy renderer's treatment. */
    private void drawFlashOverlay(RenderContext context, int barX, int barY, int barW, int barH) {
        float flash = MarieClientCache.flashAlpha(nutrientKey);
        if (flash > 0f) {
            int a = (int) (flash * 80);
            int flashColor = (a << 24) | 0xFFFFFF;
            context.fillRect(barX, barY, barW, barH, flashColor);
        }
    }

    /**
     * {@link NutrientRegistry#getIconItem(String)} resolves/validates the icon id string once per
     * distinct id and caches the {@link net.minecraft.world.item.Item} forever — this only wraps
     * that cached, already-validated item in a fresh {@link ItemStack} per call, instead of
     * re-running {@link ResourceLocation#tryParse} and a {@link BuiltInRegistries#ITEM} lookup on
     * every HUD frame for every visible bar.
     */
    private static ItemStack resolveIconStack(String key) {
        return new ItemStack(NutrientRegistry.getIconItem(key));
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        // Real containment: this row's slot (bounds) is sized/positioned entirely from hudLayout,
        // unaffected by contentScale, so a persisted zoom above hudLayout's own proportions is
        // clipped at this row's own edges instead of overlapping neighboring rows — same pattern as
        // the other 7 ContentScaleController-managed modules' pushClip.
        // The per-row clip would cut off content moved out of its own row slot by the move offsets
        // (the panel-wide clip in NutrientPanelContainer still applies), so it only applies unshifted.
        if (textDx != 0 || textDy != 0 || iconDx != 0 || iconDy != 0 || barDx != 0 || barDy != 0) {
            renderContent(context, bounds);
            return;
        }
        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            renderContent(context, bounds);
        } finally {
            context.popClip();
        }
    }

    private void renderContent(RenderContext context, Bounds bounds) {
        float value = displayValues.getOrDefault(nutrientKey, 0f);
        String label = HudDrawHelpers.nutrientLabel(nutrientKey);
        int fillColor = HudDrawHelpers.barFillColor(nutrientKey, value);
        double textBrightness = NourishedClientConfig.get().hudTextBrightness();
        int pctColor = MarieModuleSettings.scaleBrightness(HudDrawHelpers.pctColor(nutrientKey, value), textBrightness);
        int bgColor = HudDrawHelpers.barBackgroundColor();
        int labelColor = MarieModuleSettings.scaleBrightness(HudDrawHelpers.labelColor(), textBrightness);
        String pctText = Math.round(value * 100f) + "%";
        var font = Minecraft.getInstance().font;

        if (verticalMode) {
            int textH = (int) Math.ceil(9 * barScale);
            int barW = Math.max(1, Math.round(hudLayout.verticalBarW() * barScale));
            int barH = Math.max(1, Math.round(hudLayout.verticalBarH() * barScale));
            int barX = bounds.x() + (bounds.width() - barW) / 2;
            int barY = bounds.y() + textH + 2;

            int pctSw = (int) Math.ceil(font.width(pctText) * barScale);
            int pctX = bounds.x() + (bounds.width() - pctSw) / 2;
            context.drawText(pctText, pctX + barDx, bounds.y() + barDy, pctColor, barScale);

            context.drawVerticalBar(barX + barDx, barY + barDy, barW, barH, value, bgColor, fillColor);
            drawFlashOverlay(context, barX + barDx, barY + barDy, barW, barH);

            int labelSw = (int) Math.ceil(font.width(label) * contentScale);
            int labelX = bounds.x() + (bounds.width() - labelSw) / 2;
            context.drawText(label, labelX + textDx, barY + barH + 2 + textDy, labelColor, contentScale);
        } else {
            int rowCenterY = bounds.y() + bounds.height() / 2;
            int textY = rowCenterY - (int) Math.ceil(9 * contentScale) / 2;
            int iconSize = hudLayout.iconSize();

            float tint = (float) NourishedClientConfig.get().hudIconBrightness();
            RenderSystem.setShaderColor(tint, tint, tint, 1f);
            try {
                context.drawItem(resolveIconStack(nutrientKey), bounds.x() + iconDx, rowCenterY - iconSize / 2 + iconDy, iconScale);
            } finally {
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }

            int labelX = bounds.x() + iconSize + HudDrawHelpers.ICON_LABEL_GAP;
            context.drawText(label, labelX + textDx, textY + textDy, labelColor, contentScale);

            int barX = labelX + hudLayout.maxLabelSw() + HudDrawHelpers.LABEL_BAR_GAP;
            int barW = Math.max(1, Math.round(hudLayout.barW() * barScale));
            int barH = Math.max(1, Math.round(HudDrawHelpers.BAR_H * barScale));
            int barY = rowCenterY - barH / 2;
            context.drawBar(barX + barDx, barY + barDy, barW, barH, value, bgColor, fillColor);
            drawFlashOverlay(context, barX + barDx, barY + barDy, barW, barH);

            // The number at the bar's end sizes and moves with the bar, not with the text.
            int pctX = barX + barW + HudDrawHelpers.BAR_PCT_GAP;
            int pctY = rowCenterY - (int) Math.ceil(9 * barScale) / 2;
            context.drawText(pctText, pctX + barDx, pctY + barDy, pctColor, barScale);
        }
    }
}
