package dev.maire.nourished.client.hud;

import dev.marie.framework.client.MarieClientCache;
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
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

    NutrientBarComponent(String nutrientKey, boolean verticalMode, HudLayout.Layout hudLayout, Map<String, Float> displayValues) {
        this.nutrientKey = nutrientKey;
        this.verticalMode = verticalMode;
        this.hudLayout = hudLayout;
        this.displayValues = displayValues;
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
            return Constraint.fixed(hudLayout.verticalColumnW(), contentH);
        }
        int contentW = hudLayout.panelW() - hudLayout.scaledPad() * 2;
        Size preferred = new Size(contentW, hudLayout.rowH());
        Size minSize = new Size(0, hudLayout.rowH());
        Size maxSize = new Size(Integer.MAX_VALUE, hudLayout.rowH());
        return new Constraint(preferred, minSize, maxSize, true, false, true, false,
                Anchor.TOP_LEFT, Insets.NONE, Insets.NONE);
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

    private static ItemStack resolveIconStack(String key) {
        String iconId = NutrientRegistry.getIcon(key);
        ResourceLocation iconLoc = ResourceLocation.tryParse(iconId);
        var item = iconLoc == null
                ? Items.APPLE
                : BuiltInRegistries.ITEM.getOptional(iconLoc).orElse(Items.APPLE);
        return new ItemStack(item);
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        float value = displayValues.getOrDefault(nutrientKey, 0f);
        String label = HudDrawHelpers.nutrientLabel(nutrientKey);
        int fillColor = HudDrawHelpers.barFillColor(nutrientKey, value);
        int pctColor = HudDrawHelpers.pctColor(nutrientKey, value);
        int bgColor = HudDrawHelpers.barBackgroundColor();
        int labelColor = HudDrawHelpers.labelColor();
        String pctText = Math.round(value * 100f) + "%";
        float labelScale = hudLayout.labelScale();
        var font = Minecraft.getInstance().font;

        if (verticalMode) {
            int textH = (int) Math.ceil(9 * labelScale);
            int barW = hudLayout.verticalBarW();
            int barH = hudLayout.verticalBarH();
            int barX = bounds.x() + (bounds.width() - barW) / 2;
            int barY = bounds.y() + textH + 2;

            int pctSw = (int) Math.ceil(font.width(pctText) * labelScale);
            int pctX = bounds.x() + (bounds.width() - pctSw) / 2;
            context.drawText(pctText, pctX, bounds.y(), pctColor, labelScale);

            context.drawVerticalBar(barX, barY, barW, barH, value, bgColor, fillColor);
            drawFlashOverlay(context, barX, barY, barW, barH);

            int labelSw = (int) Math.ceil(font.width(label) * labelScale);
            int labelX = bounds.x() + (bounds.width() - labelSw) / 2;
            context.drawText(label, labelX, barY + barH + 2, labelColor, labelScale);
        } else {
            int rowCenterY = bounds.y() + bounds.height() / 2;
            int textY = rowCenterY - (int) Math.ceil(9 * labelScale) / 2;
            int iconSize = hudLayout.iconSize();

            context.drawItem(resolveIconStack(nutrientKey), bounds.x(), rowCenterY - iconSize / 2, iconSize / 16f);

            int labelX = bounds.x() + iconSize + HudDrawHelpers.ICON_LABEL_GAP;
            context.drawText(label, labelX, textY, labelColor, labelScale);

            int barX = labelX + hudLayout.maxLabelSw() + HudDrawHelpers.LABEL_BAR_GAP;
            int barY = rowCenterY - HudDrawHelpers.BAR_H / 2;
            context.drawBar(barX, barY, hudLayout.barW(), HudDrawHelpers.BAR_H, value, bgColor, fillColor);
            drawFlashOverlay(context, barX, barY, hudLayout.barW(), HudDrawHelpers.BAR_H);

            int pctX = barX + hudLayout.barW() + HudDrawHelpers.BAR_PCT_GAP;
            context.drawText(pctText, pctX, textY, pctColor, labelScale);
        }
    }
}
