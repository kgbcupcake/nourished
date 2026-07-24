package dev.maire.nourished.client.hud;

import dev.maire.nourished.client.hud.dynamic.visibility.HudVisibilityRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudVisibilityRulesTest {

    private static final List<String> KEYS = List.of("proteins", "grains", "fruits");

    @Test
    @DisplayName("Defaults show all non-zero bars")
    void defaultsShowAllNonZero() {
        Map<String, Float> nutrients = Map.of(
                "proteins", 0.5f,
                "grains", 0.0f,
                "fruits", 0.9f
        );

        List<String> visible = HudVisibilityRules.filter(nutrients, KEYS, false, 1.0f, 1.0f, Set.of());

        assertEquals(List.of("proteins", "fruits"), visible);
    }

    @Test
    @DisplayName("hideAbove=0.8 hides high values when showAbove is disabled")
    void hideAboveWithoutShowAbove() {
        Map<String, Float> nutrients = Map.of(
                "proteins", 0.50f,
                "grains", 0.85f,
                "fruits", 0.90f
        );

        List<String> visible = HudVisibilityRules.filter(nutrients, KEYS, false, 0.8f, 1.0f, Set.of());

        assertEquals(List.of("proteins"), visible);
    }

    @Test
    @DisplayName("showAbove=0.674 does not hide 50% bars on a new world")
    void showAboveDoesNotHideMidRangeFromUserConfig() {
        Map<String, Float> nutrients = Map.of(
                "proteins", 0.50f,
                "grains", 0.50f,
                "fruits", 0.50f
        );

        List<String> visible = HudVisibilityRules.filter(nutrients, KEYS, false, 1.0f, 0.674f, Set.of());

        assertEquals(KEYS, visible);
    }

    @Test
    @DisplayName("showAbove overrides hideAbove for high nutrients")
    void showAboveOverridesHideAbove() {
        Map<String, Float> nutrients = Map.of(
                "proteins", 0.50f,
                "grains", 0.85f,
                "fruits", 0.90f
        );

        List<String> visible = HudVisibilityRules.filter(nutrients, KEYS, false, 0.8f, 0.674f, Set.of());

        assertEquals(KEYS, visible);
    }

    @Test
    @DisplayName("showAbove below hideAbove keeps only the middle band")
    void showAboveBelowHideAboveCreatesBand() {
        Map<String, Float> nutrients = Map.of(
                "proteins", 0.50f,
                "grains", 0.85f,
                "fruits", 0.90f
        );

        List<String> visible = HudVisibilityRules.filter(nutrients, KEYS, false, 0.8f, 0.9f, Set.of());

        assertEquals(List.of("proteins", "fruits"), visible);
    }

    @Test
    @DisplayName("Zero rows respect showZero toggle regardless of showAbove")
    void zeroRowsRespectToggle() {
        Map<String, Float> nutrients = Map.of(
                "proteins", 0.0f,
                "grains", 0.50f,
                "fruits", 0.0f
        );

        List<String> hiddenZeros = HudVisibilityRules.filter(nutrients, KEYS, false, 1.0f, 0.674f, Set.of());
        assertEquals(List.of("grains"), hiddenZeros);

        List<String> shownZeros = HudVisibilityRules.filter(nutrients, KEYS, true, 1.0f, 0.674f, Set.of());
        assertEquals(KEYS, shownZeros);
    }

    @Test
    @DisplayName("showZero with both thresholds at defaults shows everything")
    void showZeroWithDefaultThresholdsShowsAll() {
        Map<String, Float> nutrients = Map.of(
                "proteins", 0.0f,
                "grains", 0.50f,
                "fruits", 0.90f
        );

        List<String> visible = HudVisibilityRules.filter(nutrients, KEYS, true, 1.0f, 1.0f, Set.of());

        assertEquals(KEYS, visible);
    }

    @Test
    @DisplayName("hideAbove=0.344 hides nutrients at 50% when showAbove is disabled")
    void hideAboveHidesMidRangeWhenShowAboveDisabled() {
        Map<String, Float> nutrients = Map.of(
                "proteins", 0.50f,
                "grains", 0.50f,
                "fruits", 0.50f
        );

        List<String> visible = HudVisibilityRules.filter(nutrients, KEYS, false, 0.344f, 1.0f, Set.of());

        assertTrue(visible.isEmpty());
    }

    @Test
    @DisplayName("showAbove=0.0 re-enables all hidden non-zero bars")
    void showAboveAtZeroReenablesAllHidden() {
        Map<String, Float> nutrients = Map.of(
                "proteins", 0.50f,
                "grains", 0.85f,
                "fruits", 0.90f
        );

        List<String> visible = HudVisibilityRules.filter(nutrients, KEYS, false, 0.8f, 0.0f, Set.of());

        assertEquals(KEYS, visible);
    }

    @Test
    @DisplayName("flashing key bypasses hideAbove threshold")
    void flashingKeyBypassesHideAbove() {
        Map<String, Float> nutrients = Map.of(
                "proteins", 0.50f,
                "grains", 0.85f,
                "fruits", 0.90f
        );

        List<String> visible = HudVisibilityRules.filter(
                nutrients,
                KEYS,
                false,
                0.8f,
                1.0f,
                Set.of("grains", "fruits")
        );

        assertEquals(KEYS, visible);
    }

    @Test
    @DisplayName("flashing zero bar appears even when showZero is false")
    void flashingZeroBarAppearsWithoutShowZero() {
        Map<String, Float> nutrients = Map.of(
                "proteins", 0.0f,
                "grains", 0.50f,
                "fruits", 0.0f
        );

        List<String> visible = HudVisibilityRules.filter(
                nutrients,
                KEYS,
                false,
                1.0f,
                1.0f,
                Set.of("fruits")
        );

        assertEquals(List.of("grains", "fruits"), visible);
    }

    @Test
    @DisplayName("empty flashingKeys preserves existing hideAbove behavior")
    void emptyFlashingKeysPreservesHideAbove() {
        Map<String, Float> nutrients = Map.of(
                "proteins", 0.50f,
                "grains", 0.85f,
                "fruits", 0.90f
        );

        List<String> visible = HudVisibilityRules.filter(nutrients, KEYS, false, 0.8f, 1.0f, Set.of());

        assertEquals(List.of("proteins"), visible);
    }
}
