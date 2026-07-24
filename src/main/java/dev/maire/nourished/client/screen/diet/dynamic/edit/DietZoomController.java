package dev.maire.nourished.client.screen.diet.dynamic.edit;

import dev.marie.framework.ui.edit.DoubleClickRecognizer;
import dev.marie.framework.ui.geometry.Bounds;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.modules.ActiveEffectsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.BalanceComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.CaloriesComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.EatMoreComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.RecentMealsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-sub-box double-click zoom mode for {@link DietScreenEditTarget}: left-double-clicking a box
 * enters zoom mode for it (subsequent scroll-wheel input adjusts that box's persisted contentScale
 * multiplier — see {@code DietScreenModules#zoomedTextIconScale}), right-double-click exits it.
 * Exactly one box can be zoomed at a time; entering one implicitly exits whichever other box was
 * active. Kept as its own small class rather than folded into {@link DietScreenEditTarget} so that
 * already-large class doesn't grow further for what is otherwise self-contained input logic.
 */
final class DietZoomController {

    enum Box { CALORIES, BALANCE, RECENT_MEALS, EAT_MORE, ACTIVE_EFFECTS }

    /** ContentScale change applied per scroll notch. */
    private static final double SCROLL_STEP = 0.05d;

    private final Map<Box, DoubleClickRecognizer> recognizers = new EnumMap<>(Box.class);
    /**
     * Per-box hit-test bounds snapshot from that box's own most recent click, reused for the *next*
     * click's containment check instead of re-reading that box's live {@code resolvedBounds()} —
     * a box whose default (never-manually-positioned) Y is chained after a sibling's content-driven
     * height (e.g. {@code EatMoreComponent} chained after {@code RecentMealsComponent}, whose height
     * depends on how many recent meals are currently tracked) can shift vertically between the two
     * clicks of a double-click; testing the second click against bounds that already moved makes the
     * pair silently fail to complete. Expires after {@link DoubleClickRecognizer#DEFAULT_WINDOW_MS} so
     * a stale snapshot from a long-past click can't gate a much-later, unrelated click.
     */
    private final Map<Box, Bounds> pendingClickBounds = new EnumMap<>(Box.class);
    private final Map<Box, Long> pendingClickTimeMs = new EnumMap<>(Box.class);
    private Box zoomedBox;

    DietZoomController() {
        for (Box box : Box.values()) {
            DoubleClickRecognizer recognizer = new DoubleClickRecognizer();
            recognizer.onLeftDoubleClick(() -> zoomedBox = box);
            recognizer.onRightDoubleClick(() -> {
                if (zoomedBox == box) {
                    zoomedBox = null;
                }
            });
            recognizers.put(box, recognizer);
        }
    }

    /**
     * Feeds a click at ({@code mouseX}, {@code mouseY}) to {@code box}'s recognizer, hit-tested
     * against {@code liveBounds} for the first click of a potential pair or against that same click's
     * snapshotted bounds for a second click following within the window — see {@link
     * #pendingClickBounds}. Returns true if this click completed a double-click (entering/exiting zoom
     * mode) — callers should treat that as consumed and skip the box's normal drag/resize click
     * handling for this event, same contract as {@link DoubleClickRecognizer#onClick}.
     */
    boolean onClick(Box box, int button, double mouseX, double mouseY, Bounds liveBounds) {
        long now = System.currentTimeMillis();
        Long pendingTime = pendingClickTimeMs.get(box);
        boolean pendingFresh = pendingTime != null && now - pendingTime <= DoubleClickRecognizer.DEFAULT_WINDOW_MS;
        Bounds testBounds = pendingFresh ? pendingClickBounds.get(box) : liveBounds;
        if (!testBounds.contains((int) mouseX, (int) mouseY)) {
            return false;
        }
        boolean completed = recognizers.get(box).onClick(button, mouseX, mouseY);
        if (completed) {
            pendingClickBounds.remove(box);
            pendingClickTimeMs.remove(box);
        } else {
            pendingClickBounds.put(box, liveBounds);
            pendingClickTimeMs.put(box, now);
        }
        return completed;
    }

    boolean isZoomed(Box box) {
        return zoomedBox == box;
    }

    /**
     * Adjusts the currently-zoomed box's persisted contentScale (via its {@code ComponentState}, see
     * {@link DietScreenPersistence#adjustContentScale}) by one scroll notch. Returns true if a zoomed
     * box consumed the scroll, false if nothing is zoomed or {@code currentBounds} doesn't have an
     * entry for the zoomed box yet (no render has happened this session).
     */
    boolean handleScroll(double scrollY, DietLayout.Layout panelLayout, Map<Box, Bounds> currentBounds) {
        if (zoomedBox == null || scrollY == 0) {
            return false;
        }
        Bounds bounds = currentBounds.get(zoomedBox);
        if (bounds == null) {
            return false;
        }
        double delta = scrollY > 0 ? SCROLL_STEP : -SCROLL_STEP;
        DietScreenPersistence.adjustContentScale(componentId(zoomedBox), panelLayout, bounds, delta);
        return true;
    }

    /** The persisted-component ID backing {@code box}'s content transform — see the individual module classes' own {@code ID} constants. */
    static String componentId(Box box) {
        return switch (box) {
            case CALORIES -> CaloriesComponent.ID;
            case BALANCE -> BalanceComponent.ID;
            case RECENT_MEALS -> RecentMealsComponent.ID;
            case EAT_MORE -> EatMoreComponent.ID;
            case ACTIVE_EFFECTS -> ActiveEffectsComponent.ID;
        };
    }
}
