package dev.maire.nourished.core.nutrition.curve;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.curve.math.CurveGrid;

/**
 * Named response-curve shapes for nutrient contribution scaling. Each maps
 * (food intensity, scanner confidence) -> a contribution multiplier.
 */
@ApiStatus.Internal
public enum NutrientCurvePreset {

    /**
     * Uniform 1.0 everywhere — mathematically identical to the legacy flat
     * scale/clamp math in FoodNutritionRegistry.computeDietDelta. This is the
     * default for every nutrient until a player or pack author opts into
     * something else, so existing balance is unchanged unless requested.
     */
    FLAT {
        @Override
        public CurveGrid bake() {
            return CurveGrid.flat(5, 5, 1.0f);
        }
    },

    /**
     * Multiplier decreases as intensity rises, regardless of confidence —
     * prevents one large, high-saturation meal from disproportionately
     * filling a bar compared to several smaller meals.
     */
    DIMINISHING {
        @Override
        public CurveGrid bake() {
            int cells = 5;
            float[] grid = new float[(cells + 1) * (cells + 1)];
            for (int xi = 0; xi <= cells; xi++) {
                float intensity = xi / (float) cells;
                float multiplier = 1.0f - (0.6f * intensity);
                for (int yi = 0; yi <= cells; yi++) {
                    grid[xi * (cells + 1) + yi] = multiplier;
                }
            }
            return new CurveGrid(cells, cells, grid);
        }
    },

    /**
     * Multiplier scales steeply with confidence, largely ignoring intensity —
     * rewards well-classified foods (high scanner confidence) with close to
     * full contribution, while poorly-classified foods contribute very little
     * even if "filling." Directly incentivizes good scanner spec content.
     */
    CONFIDENCE_GATED {
        @Override
        public CurveGrid bake() {
            int cells = 5;
            float[] grid = new float[(cells + 1) * (cells + 1)];
            for (int xi = 0; xi <= cells; xi++) {
                for (int yi = 0; yi <= cells; yi++) {
                    float confidence = yi / (float) cells;
                    float multiplier = confidence * confidence;
                    grid[xi * (cells + 1) + yi] = multiplier;
                }
            }
            return new CurveGrid(cells, cells, grid);
        }
    },

    /**
     * Bonus multiplier (&gt;1.0) when both intensity and confidence are high
     * simultaneously, tapering toward 1.0 (no bonus, no penalty) when either
     * axis is low. The cross-axis interaction case — this is the genuine
     * reason the curve is 2D rather than two independent 1D curves.
     */
    SYNERGY {
        @Override
        public CurveGrid bake() {
            int cells = 5;
            float[] grid = new float[(cells + 1) * (cells + 1)];
            for (int xi = 0; xi <= cells; xi++) {
                float intensity = xi / (float) cells;
                for (int yi = 0; yi <= cells; yi++) {
                    float confidence = yi / (float) cells;
                    float bonus = 0.5f * intensity * confidence;
                    grid[xi * (cells + 1) + yi] = 1.0f + bonus;
                }
            }
            return new CurveGrid(cells, cells, grid);
        }
    };

    public abstract CurveGrid bake();
}
