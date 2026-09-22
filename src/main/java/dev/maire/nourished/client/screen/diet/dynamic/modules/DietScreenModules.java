package dev.maire.nourished.client.screen.diet.dynamic.modules;

import dev.marie.framework.ui.component.AutoGrowPanelContainer;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.component.ModuleFactory;
import dev.marie.framework.ui.component.ModuleRegistry;
import dev.marie.framework.ui.component.SelfPositioningModule;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLeftColumnComponent;
import dev.maire.nourished.core.Nourished;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers the Diet Screen's three self-positioning sub-box modules (RecentMeals/EatMore/
 * ActiveEffects) against the generic marie-ui {@link ModuleRegistry}, and builds them back from it —
 * replacing the hardcoded {@code new RecentMealsComponent(...)}/etc. construction that used to be
 * duplicated independently in both {@link DietLeftColumnComponent} and {@link DietScreenEditTarget}.
 * {@link #registerAll()} is called once from {@link dev.maire.nourished.client.ClientEventRegistrar
 * #register}, client-side only — these constructors touch {@code Minecraft.getInstance()}, so this
 * must never run on a dedicated server.
 *
 * <p>Intake Breakdown is NOT registered here — {@link DietRightColumnComponent} is positioned by
 * horizontal column split against {@link DietPanelContainer}'s layout, not by a single chained
 * local-Y within a vertical stack, so it doesn't fit {@link ModuleFactory}'s
 * {@code (layoutContext, startLocalY)} contract the way a left-column module does. Registering it
 * through this same list would insert it into the left column's vertical stack instead of the right
 * column. Making it registrable needs {@link ModuleRegistry} to grow a region/slot concept (or a
 * second registry instance per column) — a separate, not-yet-scoped follow-up. It still gets real
 * {@link HeaderCollapsibleComponent}-consistent collapse behavior; it's just still constructed
 * directly by {@link DietPanelContainer}, not via this registry.
 */
public final class DietScreenModules {

    /**
     * Single shared trailing gap, in local (pre-scale) units, every left-column module reserves
     * after its own content before the next module's stacked start-Y — consolidates what used to be
     * five independently-hardcoded values (5, 5, 4, 8, 8 across Calories/Balance/EatMore/RecentMeals/
     * ActiveEffects) into one constant, so no module sits visually closer to its neighbor than
     * another regardless of size.
     */
    public static final int MODULE_GAP_LOCAL = 8;

    /**
     * Smallest continuous-fade room (local units, see {@link dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout#roomInPanel})
     * a left-column sub-box still renders at before it drops out entirely — the tail end of "shrink
     * smoothly as room tightens" rather than an abrupt full-to-nothing flip. Small enough to squeeze
     * out most of a box's usable natural height range before disappearing, but not so small the box
     * renders as an unreadable sliver right before it goes.
     */
    public static final int MIN_VISIBLE_ROOM_LOCAL = 4;

    /**
     * Local-unit top padding between a module's own top border and its header text baseline, for
     * modules whose local (0, startLocalY) origin sits flush at the box's own top-left (EatMore/
     * RecentMeals/ActiveEffects — not Calories/Balance, whose origin is already inset 2 units inside
     * their own border). Matches ActiveEffectsComponent's prior inline value.
     */
    static final int HEADER_TOP_PADDING_LOCAL = 2;

    /**
     * Registry key for the Intake Breakdown (right) column's module chain — separate from {@link
     * Nourished#MODID} (the left column's key) so the two columns each get their own independent,
     * ordered {@code startLocalY} cursor out of the single shared {@link ModuleRegistry}. See {@link
     * #build(String, DietLayout.Layout, int)}.
     */
    public static final String RIGHT_COLUMN_KEY = "nourished.diet.right";

    private DietScreenModules() {}

    public static void registerAll() {
        // Calories/Balance registered before RecentMeals/EatMore/ActiveEffects to preserve the
        // original stacked visual order (Today header, then Calories, then Balance, then the three
        // sub-boxes) — build()'s chaining loop below positions each module strictly in registration
        // order.
        ModuleRegistry.register(Nourished.MODID, (ModuleFactory<DietLayout.Layout>) CaloriesComponent::new);
        ModuleRegistry.register(Nourished.MODID, (ModuleFactory<DietLayout.Layout>) BalanceComponent::new);
        ModuleRegistry.register(Nourished.MODID, (ModuleFactory<DietLayout.Layout>) RecentMealsComponent::new);
        ModuleRegistry.register(Nourished.MODID, (ModuleFactory<DietLayout.Layout>) EatMoreComponent::new);
        ModuleRegistry.register(Nourished.MODID, (ModuleFactory<DietLayout.Layout>) ActiveEffectsComponent::new);

        // Right column: header, one row per nutrient "slot", then the legend. Each row factory
        // resolves its actual nutrient key from NourishedClientConfig#effectiveDietBarOrder() at
        // construction time (every frame), not here at registration time, so live bar reordering
        // still works — see IntakeBarComponent's javadoc. The slot count is fixed at registration
        // time to the registered nutrient count, which does not change at runtime.
        ModuleRegistry.register(RIGHT_COLUMN_KEY, (ModuleFactory<DietLayout.Layout>) IntakeHeaderComponent::new);
        int slots = dev.maire.nourished.core.nutrition.NutrientRegistry.getKeys().size();
        for (int i = 0; i < slots; i++) {
            final int slot = i;
            ModuleRegistry.register(RIGHT_COLUMN_KEY, (ModuleFactory<DietLayout.Layout>) (layout, startY) -> IntakeBarComponent.create(slot, layout, startY));
        }
        ModuleRegistry.register(RIGHT_COLUMN_KEY, (ModuleFactory<DietLayout.Layout>) IntakeLegendComponent::new);
    }

    /**
     * Builds every module registered for {@link Nourished#MODID} against {@code layout}, in
     * registration order, chaining each {@link SelfPositioningModule}'s stacked start-Y off the
     * previous one via {@link DietLeftColumnComponent#nextSiblingStartLocalY} — the single shared
     * implementation of what {@link DietLeftColumnComponent}'s constructor and {@link
     * DietScreenEditTarget}'s constructor used to each hand-roll independently for the same three
     * components. A module that doesn't implement {@link SelfPositioningModule} (none do today, but
     * the registry itself doesn't require it) simply doesn't advance the cursor for whatever comes
     * after it.
     */
    public static List<MarieComponent> build(DietLayout.Layout layout, int startLocalY) {
        return build(Nourished.MODID, layout, startLocalY);
    }

    /**
     * Same as {@link #build(DietLayout.Layout, int)}, but against an arbitrary {@link ModuleRegistry}
     * key — lets a second, independently-chained module list (the right/"Intake Breakdown" column,
     * under {@link #RIGHT_COLUMN_KEY}) share the same build/chaining logic as the left column instead
     * of a second hand-rolled copy. Assumes the left column's content X for the out-of-flow check
     * (see the 4-arg overload below) — correct for {@link Nourished#MODID}, wrong for {@link
     * #RIGHT_COLUMN_KEY} callers, which must use that overload instead.
     */
    public static List<MarieComponent> build(String registryKey, DietLayout.Layout layout, int startLocalY) {
        return build(registryKey, layout, startLocalY, layout.panelX() + layout.leftMargin());
    }

    /**
     * Same as {@link #build(String, DietLayout.Layout, int)}, but against an arbitrary expected
     * content X for the sibling-chaining out-of-flow check — see {@link DietLeftColumnComponent
     * #nextSiblingStartLocalY(int, int, Bounds, DietLayout.Layout, int)}. The right column must pass
     * {@link DietLayout#rightColumnContentX} here; otherwise every one of its modules' resolved X
     * (past the divider) reads as "out of flow" relative to the left column's X, the cursor never
     * advances, and every module collapses onto the same start-Y.
     */
    @SuppressWarnings("unchecked")
    public static List<MarieComponent> build(String registryKey, DietLayout.Layout layout, int startLocalY, int expectedContentX) {
        List<MarieComponent> built = new ArrayList<>();
        int cursorY = startLocalY;
        for (ModuleFactory<?> factory : ModuleRegistry.get(registryKey)) {
            // Safe: every factory registered above is a ModuleFactory<DietLayout.Layout> — the
            // registry itself is type-erased per-entry (marie-ui doesn't know Nourished's layout
            // type), so this cast is the one place that mod-local knowledge is reasserted.
            ModuleFactory<DietLayout.Layout> typed = (ModuleFactory<DietLayout.Layout>) factory;
            MarieComponent module = typed.create(layout, cursorY);
            built.add(module);
            if (module instanceof SelfPositioningModule self) {
                cursorY = DietLeftColumnComponent.nextSiblingStartLocalY(cursorY, self.localHeight(), self.resolvedBounds(), layout, expectedContentX);
            }
        }
        return built;
    }

    /**
     * The single registered module of {@code type} from {@code modules} — replaces the old
     * {@code children.get(0)/(1)/(2)} array-index assumption (fragile against registration-order
     * changes) with a lookup by concrete type, which is stable regardless of where in the list a
     * given module ended up.
     *
     * @throws IllegalStateException if no module of {@code type} was registered
     */
    public static <T extends MarieComponent> T find(List<MarieComponent> modules, Class<T> type) {
        for (MarieComponent module : modules) {
            if (type.isInstance(module)) {
                return type.cast(module);
            }
        }
        throw new IllegalStateException("No module of type " + type.getSimpleName() + " registered for " + Nourished.MODID);
    }

    /** Reference panel height for {@link #naturalContentEndLocalY}'s measurement pass — tall enough that no module's own visibility gating can fall short of it. */
    private static final int NATURAL_HEIGHT_REFERENCE_LOCAL = DietLayout.HEIGHT * 8;

    /** Natural (content-driven) end-Y, in local units, of every currently-enabled left-column module stacked from {@code startLocalY} — used for panel auto-grow. */
    public static int naturalContentEndLocalY(DietLayout.Layout layout, int startLocalY) {
        int referencePanelHeight = DietLayout.toScreenDim(layout, NATURAL_HEIGHT_REFERENCE_LOCAL);
        DietLayout.Layout reference = new DietLayout.Layout(
                layout.panelX(), layout.panelY(), layout.panelW(), referencePanelHeight,
                layout.baseX(), layout.baseY(), layout.scale(), layout.recentMealsScale(), layout.eatMoreScale(),
                layout.leftMargin()
        );
        List<MarieComponent> modules = build(reference, startLocalY);
        List<SelfPositioningModule> selfPositioning = new ArrayList<>(modules.size());
        for (MarieComponent module : modules) {
            if (module instanceof SelfPositioningModule self) {
                selfPositioning.add(self);
            }
        }
        return AutoGrowPanelContainer.naturalContentHeight(selfPositioning, startLocalY, layout.scale());
    }
}
