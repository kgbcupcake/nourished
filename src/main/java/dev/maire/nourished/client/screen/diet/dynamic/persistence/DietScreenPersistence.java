package dev.maire.nourished.client.screen.diet.dynamic.persistence;

import dev.marie.framework.ui.api.ComponentPersistence;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.api.ModuleProfile;
import dev.marie.framework.color.ColorScope;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.modules.EatMoreComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.IntakeHeaderComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.RecentMealsComponent;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Facade over the shared {@link UiStatePersistence} store for the Diet Screen's panel-relative sub-boxes. */
public final class DietScreenPersistence {

    private DietScreenPersistence() {}

    /**
     * A sub-box's live drag/resize preview {@link Bounds}, keyed by component ID, for whichever box
     * edit mode is actively dragging this frame — set by {@link
     * dev.maire.nourished.client.screen.diet.dynamic.edit.DietScreenEditTarget} just before it
     * rebuilds the module chain each frame, and cleared again immediately after. Without this,
     * {@link #resolveRelativeToPanel} (and therefore {@link DietLayout#nextSiblingStartLocalY
     * DietLeftColumnComponent#nextSiblingStartLocalY}'s sibling-stacking math) only ever sees a
     * dragged box's last *committed* size, so every module stacked after it keeps the pre-drag
     * start position for the whole gesture — a later sub-box being grown live overlaps whatever
     * follows it, and a later sub-box's own fit check (e.g. {@code ActiveEffectsComponent}'s
     * header-fits-in-panel gate) evaluates against a start-Y that hasn't caught up with the box
     * actually being resized above it, so it can appear to vanish or misplace mid-drag despite the
     * panel visually having room.
     */
    private static final Map<String, Bounds> liveOverrides = new HashMap<>();

    private static final ModuleProfile DYNAMIC = ModuleProfile.of(UiStatePersistence.get());
    // The store and color prefixes the Nutritional Panel has saved under since it got its own settings.
    private static final ModuleProfile NUTRITIONAL_PANEL = ModuleProfile.of(
            UiStatePersistence.get().scoped("nourished.nutritionalhud/"), ColorScope.prefixed("nutritionalhud/"));
    private static boolean nutritionalPanelScope;

    /** The profile the Diet Screen boxes are drawn and edited in right now. */
    public static ModuleProfile profile() {
        return nutritionalPanelScope ? NUTRITIONAL_PANEL : DYNAMIC;
    }

    /** The Diet Screen boxes' store — the Nutritional Panel's own copy while {@link #inNutritionalPanel} is running, the dynamic screen's otherwise. */
    public static PersistenceProvider get() {
        return profile().store();
    }

    public static boolean isNutritionalPanel() {
        return nutritionalPanelScope;
    }

    /** Runs {@code action} with every box and options panel reading/writing the Nutritional Panel's own settings and colors. */
    public static <T> T inNutritionalPanel(Supplier<T> action) {
        boolean previous = nutritionalPanelScope;
        nutritionalPanelScope = true;
        try {
            return NUTRITIONAL_PANEL.run(action);
        } finally {
            nutritionalPanelScope = previous;
        }
    }

    /** Registers {@code bounds} as the live preview for {@code componentId} for the remainder of this frame's module build. */
    public static void setLiveOverride(String componentId, Bounds bounds) {
        liveOverrides.put(componentId, bounds);
    }

    /** Clears every live preview override — call once per frame after the module chain that needed them has been built. */
    public static void clearLiveOverrides() {
        liveOverrides.clear();
    }

    /** A box's persisted content zoom multiplier — see {@link ComponentState#contentScale()}. Defaults to {@code 1.0} (no zoom) if never set. */
    public static double contentScale(String componentId) {
        return get().load(componentId).map(ComponentState::contentScale).orElse(ComponentState.DEFAULT_CONTENT_SCALE);
    }

    /** Always {@link ComponentState#DEFAULT_PADDING_SCALE} — the Diet Screen and its sub-boxes have no Padding slider (every panel here is built with {@code withoutPadding()}), so a value set before that slider was removed is no longer read back. */
    public static double paddingScale(String componentId) {
        return ComponentState.DEFAULT_PADDING_SCALE;
    }

    /** Resolves a sub-box's screen bounds relative to the panel: a live drag/resize preview if one is active this frame, else persisted local-unit offset/size if manually moved/resized, otherwise the natural stacked position. Confined between the panel's left edge and the column divider. */
    public static Bounds resolveRelativeToPanel(String componentId, DietLayout.Layout panelLayout, int startLocalY, int localWidth, int localHeight) {
        ensureOffsetMigration();
        int dividerX = dividerX(panelLayout);
        return ComponentPersistence.resolveRelative(get(), liveOverrides, componentId,
                contentX(panelLayout), panelLayout.panelY(), panelLayout.scale(), startLocalY, localWidth, localHeight,
                panelLayout.panelX(), panelLayout.panelW(), panelLayout.panelH(),
                panelLayout.panelX(), dividerX);
    }

    /**
     * Same as {@link #resolveRelativeToPanel}, but for the right ("Intake Breakdown") column —
     * confined between the column divider and the panel's right edge instead. Local X/Y coordinates
     * are in the same panel-relative coordinate space as the left column (e.g. the header's local X
     * of {@code DietLayout.SPLIT + DietLayout.PAD}), so a component's default stacked position lands
     * exactly where the legacy hand-rolled {@code DietRightColumnComponent} used to draw it.
     */
    public static Bounds resolveRelativeToRightColumn(String componentId, DietLayout.Layout panelLayout, int startLocalY, int localWidth, int localHeight) {
        ensureOffsetMigration();
        int dividerX = dividerX(panelLayout);
        int rightEdge = panelLayout.panelX() + panelLayout.panelW();
        return ComponentPersistence.resolveRelative(get(), liveOverrides, componentId,
                DietLayout.rightColumnContentX(panelLayout), panelLayout.panelY(), panelLayout.scale(), startLocalY, localWidth, localHeight,
                panelLayout.panelX(), panelLayout.panelW(), panelLayout.panelH(),
                dividerX, rightEdge);
    }

    private static int contentX(DietLayout.Layout panelLayout) {
        return panelLayout.panelX() + panelLayout.leftMargin();
    }

    private static int dividerX(DietLayout.Layout panelLayout) {
        Bounds panelBounds = new Bounds(panelLayout.panelX(), panelLayout.panelY(), panelLayout.panelW(), panelLayout.panelH());
        return DietLayout.columnGeometry(panelLayout, panelBounds).dividerX();
    }

    /** One-time resets for past persisted-offset schema changes (absolute -> local-unit -> scale-normalized). Each flag guards a distinct meaning change so players on an intermediate scheme aren't misinterpreted. */
    private static void ensureOffsetMigration() {
        NourishedClientConfig cc = NourishedClientConfig.get();
        boolean didWork = false;
        if (!cc.recentMealsEatMoreOffsetMigrationDone()) {
            UiStatePersistence.get().remove(RecentMealsComponent.ID);
            UiStatePersistence.get().remove(EatMoreComponent.ID);
            cc.setRecentMealsEatMoreOffsetMigrationDone(true);
            didWork = true;
        }
        if (!cc.recentMealsEatMoreLocalOffsetMigrationDone()) {
            UiStatePersistence.get().remove(RecentMealsComponent.ID);
            UiStatePersistence.get().remove(EatMoreComponent.ID);
            cc.setRecentMealsEatMoreLocalOffsetMigrationDone(true);
            didWork = true;
        }
        if (!cc.recentMealsEatMoreLocalSizeMigrationDone()) {
            UiStatePersistence.get().remove(RecentMealsComponent.ID);
            UiStatePersistence.get().remove(EatMoreComponent.ID);
            cc.setRecentMealsEatMoreLocalSizeMigrationDone(true);
            didWork = true;
        }
        if (!cc.recentMealsRowHeightMigrationDone()) {
            UiStatePersistence.get().remove(RecentMealsComponent.ID);
            cc.setRecentMealsRowHeightMigrationDone(true);
            didWork = true;
        }
        if (!cc.intakeBreakdownOffsetMigrationDone()) {
            UiStatePersistence.get().remove(IntakeHeaderComponent.ID);
            // "nourished.diet.legend": the removed IntakeLegendComponent's old id, kept as a literal
            // now that the class is gone — still worth clearing out any stale persisted state for it.
            UiStatePersistence.get().remove("nourished.diet.legend");
            int slots = NutrientRegistry.getKeys().size();
            for (int i = 0; i < slots; i++) {
                UiStatePersistence.get().remove("nourished.diet.intake.slot" + i);
            }
            cc.setIntakeBreakdownOffsetMigrationDone(true);
            didWork = true;
        }
        if (didWork) {
            NourishedClientConfig.saveNow();
        }
    }
}
