package dev.maire.nourished.client.screen.diet.dynamic.modules;

import dev.marie.framework.client.config.state.MarieClientCache;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.animations.AnimatedFloat;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.component.SelfPositioningModule;
import dev.marie.framework.ui.component.widgets.BarRowComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.geometry.Bounds;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.hud.dynamic.HudDrawHelpers;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One row of the Intake Breakdown column: icon + label + fill bar + percent + trend arrow for a
 * single nutrient — a thin Nourished-specific adapter over the generic marie-ui {@link
 * BarRowComponent}, one instance built per registered "slot" (see {@link DietScreenModules
 * #registerAll}) rather than one bespoke class per nutrient key.
 *
 * <p>Which nutrient key a given slot actually shows is resolved fresh from {@link
 * NourishedClientConfig#effectiveDietBarOrder()} at construction time (every frame), not baked in at
 * registration time — so live bar reordering (drag-to-reorder in the options panel) keeps working
 * exactly like the legacy hand-rolled row loop, which walked that same live list every frame.
 */
public final class IntakeBarComponent implements MarieComponent, SelfPositioningModule {

    public static final int ROW_STEP = 26;
    public static final int LOCAL_WIDTH = (DietLayout.WIDTH - DietLayout.PAD) - (DietLayout.SPLIT + DietLayout.PAD - 2);

    /**
     * Matches {@code DietScreen#ANIM_DURATION_SEC} exactly — the ~300ms convergence the classic
     * per-frame {@code display} lerp map used, so the restored animation feels identical to the
     * pre-refactor renderer, not a newly-invented speed.
     */
    private static final float ANIM_DURATION_SECONDS = 0.3f;

    /**
     * One {@link AnimatedFloat} per nutrient key, living for the process' lifetime (not per-frame,
     * unlike this component itself — see the class javadoc) so the eased value actually carries
     * state across frames. Mirrors the shape of {@code DietScreen}'s/{@code NourishedHUD}'s own
     * per-screen {@code Map<String, Float> display} lerp field, just keyed by nutrient rather than
     * held on a single long-lived screen/HUD instance, since {@link IntakeBarComponent} itself has
     * no such instance to hold it on.
     */
    private static final Map<String, AnimatedFloat> ANIMATED_VALUES = new ConcurrentHashMap<>();

    private static int borderColor() {
        return MarieColors.resolveColor(NourishedColors.BORDER);
    }

    private static int textColor() {
        return MarieColors.resolveColor(NourishedColors.TEXT);
    }

    private static int barTrackColor() {
        return MarieColors.resolveColor(NourishedColors.DIET_BAR_TRACK);
    }

    private final BarRowComponent delegate;
    private final Bounds resolvedBounds;
    private final boolean visible;
    private final String id;

    /** {@code slotIndex}: this row's position in the live bar order — see the class javadoc. */
    static MarieComponent create(int slotIndex, DietLayout.Layout layout, int startLocalY) {
        return new IntakeBarComponent(slotIndex, layout, startLocalY);
    }

    private IntakeBarComponent(int slotIndex, DietLayout.Layout layout, int startLocalY) {
        var order = NourishedClientConfig.get().effectiveDietBarOrder();
        String key = slotIndex < order.size() ? order.get(slotIndex) : null;
        this.id = "nourished.diet.intake.slot" + slotIndex;

        Minecraft mc = Minecraft.getInstance();
        TrackingData data = mc.player != null ? MarieClientCache.get() : null;

        boolean enabled = key != null && data != null;
        int room = enabled ? DietLayout.roomInPanel(layout, startLocalY, ROW_STEP) : 0;
        this.visible = room >= DietScreenModules.MIN_VISIBLE_ROOM_LOCAL;

        this.resolvedBounds = DietScreenPersistence.resolveRelativeToRightColumn(id, layout, startLocalY, LOCAL_WIDTH, ROW_STEP);

        if (!visible || key == null) {
            this.delegate = null;
            return;
        }
        String nutrientKey = key;

        // Eases toward the real current value over ANIM_DURATION_SECONDS, exactly like the classic
        // renderer's per-frame `display` lerp map — advanced once per built instance (i.e. once per
        // frame, matching how often DietScreen's own render() advanced its lerp), read by the bar's
        // fill/percent supplier below. The trend arrow deliberately does NOT use this value (see
        // trendCurrentSupplier below) — it compares the real, un-eased values, unchanged.
        double real = currentValue(nutrientKey, data);
        AnimatedFloat animated = ANIMATED_VALUES.computeIfAbsent(nutrientKey, k -> new AnimatedFloat(ANIM_DURATION_SECONDS));
        double animatedDisp = animated.update((float) real, System.nanoTime());

        this.delegate = new BarRowComponent(
                id,
                DietScreenPersistence.get(),
                resolvedBounds,
                true,
                ROW_STEP,
                BarRowComponent.DEFAULT_PERCENT_DIM_ALPHA,
                () -> resolveIcon(nutrientKey),
                () -> NutrientRegistry.getLabelComponent(nutrientKey).getString(),
                IntakeBarComponent::textColor,
                () -> animatedDisp,
                () -> previousValue(nutrientKey, data),
                () -> HudDrawHelpers.nutrientColorArgb(nutrientKey),
                IntakeBarComponent::barTrackColor,
                () -> HudDrawHelpers.nutrientColorArgb(nutrientKey),
                () -> NourishedColors.nutrient(nutrientKey),
                () -> NourishedColors.nutrient(nutrientKey),
                () -> panelFillColor(),
                IntakeBarComponent::borderColor,
                () -> flashOverlayColor(nutrientKey),
                () -> currentValue(nutrientKey, data)
        );
    }

    /**
     * The intake row's brief "just changed" highlight over the bar — ported verbatim from the
     * classic {@code DietRightColumnComponent}'s inline flash draw (alpha {@code flashAlpha * 255},
     * clamped to {@code [1, 255]}, over the nutrient's own color), now exposed as an {@code
     * IntSupplier} for the generic {@link BarRowComponent}'s optional overlay parameter. Returns a
     * fully transparent (alpha 0) color while {@link MarieClientCache#flashAlpha} is 0 — {@link
     * BarRowComponent} treats alpha 0 as "draw nothing."
     */
    private static int flashOverlayColor(String key) {
        float flashAlpha = MarieClientCache.flashAlpha(key);
        if (flashAlpha <= 0f) {
            return 0;
        }
        int aByte = Mth.clamp(Mth.floor(flashAlpha * 255f), 1, 255);
        return (aByte << 24) | NourishedColors.nutrientRgb(key);
    }

    private static ItemStack resolveIcon(String key) {
        String iconId = NutrientRegistry.getIcon(key);
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(iconId)).orElse(Items.APPLE);
        return new ItemStack(item);
    }

    private static double currentValue(String key, TrackingData data) {
        var display = data.values;
        return display.getOrDefault(key, 0f);
    }

    private static double previousValue(String key, TrackingData data) {
        float real = data.values.getOrDefault(key, 0f);
        return data.lastValues.getOrDefault(key, real);
    }

    private static int panelFillColor() {
        int alpha = Math.max(0, Math.min(255, (int) Math.round(NourishedClientConfig.get().dietBackgroundOpacity() * 255.0d)));
        return (alpha << 24) | (NourishedColors.surfaceRgb() & 0x00FFFFFF);
    }

    @Override
    public int localHeight() {
        return visible ? ROW_STEP : 0;
    }

    public int naturalLocalHeight() {
        return ROW_STEP;
    }

    @Override
    public Bounds resolvedBounds() {
        return resolvedBounds;
    }

    public boolean isVisible() {
        return visible && delegate != null;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Constraint constraint() {
        return Constraint.preferred(resolvedBounds.width(), resolvedBounds.height());
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        if (delegate != null) {
            delegate.render(context, bounds);
        }
    }
}
