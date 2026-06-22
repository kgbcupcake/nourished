package dev.maire.nourished.core.nutrition.curve;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.curve.math.CurveGrid;

/**
 * A nutrient's resolved curve: which preset it was sourced from (for display/
 * round-tripping in config and Cloth Config), and the actual baked or
 * overridden grid to evaluate.
 *
 * @param nutrientKey the nutrient this curve applies to
 * @param presetId    the preset name if sourced from a NutrientCurvePreset,
 *                    or "custom" if loaded from a hand-authored/datapack/
 *                    KubeJS grid that doesn't match a named preset
 * @param grid        the actual grid to evaluate
 */
@ApiStatus.Internal
public record NutrientCurveDef(String nutrientKey, String presetId, CurveGrid grid) {

    public static final String CUSTOM_PRESET_ID = "custom";

    public static NutrientCurveDef fromPreset(String nutrientKey, NutrientCurvePreset preset) {
        return new NutrientCurveDef(nutrientKey, preset.name(), preset.bake());
    }

    public static NutrientCurveDef custom(String nutrientKey, CurveGrid grid) {
        return new NutrientCurveDef(nutrientKey, CUSTOM_PRESET_ID, grid);
    }
}
