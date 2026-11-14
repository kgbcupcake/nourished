package dev.maire.nourished.client.screen.diet.dynamic.modules;

import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.HeaderCollapsibleComponent;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.component.SelfPositioningModule;
import dev.marie.framework.ui.edit.ContentScaleController;
import dev.marie.framework.ui.geometry.Bounds;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.core.micro.MicroAttachment;
import dev.maire.nourished.core.micro.MicroDef;
import dev.maire.nourished.core.micro.MicroRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

import static dev.maire.nourished.client.screen.diet.dynamic.layout.DietSubBoxConstraints.SUMMARY_BOX_LOCAL_WIDTH;

/** Nutrient (Omega-3, Fiber, ...) mini-bars, shown under Balance in the Diet Screen's left column. */
public final class MicroNutrientsComponent implements MarieComponent, HeaderCollapsibleComponent, SelfPositioningModule {

    public static final String ID = "nourished.diet.micros";
    private static final int HEADER_LOCAL_HEIGHT = 22;
    private static final int ROW_LOCAL_HEIGHT = 11;
    private static final int LABEL_LOCAL_WIDTH = 56;
    private static final double BASE_PADDING_LOCAL = 2.0d;

    private final DietLayout.Layout layout;
    private final int startLocalY;
    private final List<MicroDef> defs;
    private final Map<String, Float> levels;
    private final boolean visible;
    private final int boxLocalHeight;
    private final int localHeight;
    private final Bounds resolvedBounds;
    private final SummaryBoxRenderSupport support;

    // last drawn screen geometry, so a click on the Diet Screen can open that nutrient's detail view
    private static volatile Bounds lastBounds;
    private static volatile int lastFirstRowY;
    private static volatile int lastRowH = 1;

    MicroNutrientsComponent(DietLayout.Layout layout, int startLocalY) {
        this.layout = layout;
        this.startLocalY = startLocalY;
        this.support = new SummaryBoxRenderSupport(startLocalY);
        Minecraft mc = Minecraft.getInstance();
        MicroRegistry.Snapshot snap = MicroRegistry.forSide(true);
        this.defs = snap.settings().enabled() ? List.copyOf(snap.defs().values()) : List.of();
        this.levels = mc.player != null ? MicroAttachment.get(mc.player) : Map.of();
        this.boxLocalHeight = HEADER_LOCAL_HEIGHT + defs.size() * ROW_LOCAL_HEIGHT + 4;

        boolean enabled = !defs.isEmpty() && mc.player != null
                && !MarieModuleSettings.isWindowHidden(DietScreenPersistence.get(), ID);
        int room = enabled ? DietLayout.roomInPanel(layout, startLocalY, boxLocalHeight) : 0;
        this.visible = room >= DietScreenModules.MIN_VISIBLE_ROOM_LOCAL;
        this.localHeight = visible ? room + DietScreenModules.MODULE_GAP_LOCAL : 0;
        this.resolvedBounds = DietScreenPersistence.resolveRelativeToPanel(ID, layout, startLocalY, SUMMARY_BOX_LOCAL_WIDTH, localHeight);
    }

    @Override
    public int localHeight() {
        return localHeight;
    }

    @Override
    public Bounds resolvedBounds() {
        return resolvedBounds;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public int headerLocalHeight() {
        return HEADER_LOCAL_HEIGHT;
    }

    @Override
    public Constraint constraint() {
        return Constraint.preferred(DietLayout.toScreenDim(layout, SUMMARY_BOX_LOCAL_WIDTH), DietLayout.toScreenDim(layout, localHeight));
    }

    @Override
    public void render(RenderContext baseContext, Bounds bounds) {
        RenderContext context = MarieModuleSettings.withDisplaySettings(baseContext, DietScreenPersistence.get(), ID, false);
        if (!visible) {
            lastBounds = null;
            return;
        }
        lastBounds = bounds;
        var store = DietScreenPersistence.get();
        double userPaddingLocal = BASE_PADDING_LOCAL * DietScreenPersistence.paddingScale(ID);
        support.begin(bounds, layout.scale(), ContentScaleController.resolvePadding(userPaddingLocal) - BASE_PADDING_LOCAL);
        support.drawOuterBox(context, bounds.width(), bounds.height(), NourishedClientConfig.get(),
                SummaryBoxRenderSupport.borderColor(), store, ID);

        float scale = ContentScaleController.resolveContentScale(DietScreenPersistence.contentScale(ID));
        float iconScale = ContentScaleController.resolveContentScale(MarieModuleSettings.iconScale(store, ID, false));
        float headerScale = ContentScaleController.resolveContentScale(MarieModuleSettings.headerScale(store, ID));
        context.pushClip(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        try {
            support.drawItem(context, "minecraft:cod", 2, 3, iconScale);
            if (!MarieModuleSettings.isHeaderHidden(store, ID)) {
                String header = Component.translatable("nourished.screen.diet.micros_label").getString();
                support.drawText(context, header, 24, 7, SummaryBoxRenderSupport.textColor(), headerScale);
            }
            lastFirstRowY = support.sy(startLocalY + HEADER_LOCAL_HEIGHT);
            lastRowH = Math.max(1, support.sd(ROW_LOCAL_HEIGHT));
            int barLeft = LABEL_LOCAL_WIDTH + 4;
            int barWidth = SUMMARY_BOX_LOCAL_WIDTH - barLeft - 6;
            for (int i = 0; i < defs.size(); i++) {
                MicroDef def = defs.get(i);
                int rowY = HEADER_LOCAL_HEIGHT + i * ROW_LOCAL_HEIGHT;
                int color = 0xFF000000 | def.color();
                support.drawText(context, def.displayName(), 4, rowY, color, scale * 0.85f);
                float level = Math.max(0f, Math.min(1f, levels.getOrDefault(def.key(), 0f)));
                int x = support.sx(barLeft);
                int y = support.sy(startLocalY + rowY + 1);
                int w = support.sd(barWidth);
                int h = Math.max(1, support.sd(5));
                context.fillRect(x, y, w, h, SummaryBoxRenderSupport.barTrackColor());
                int filled = Math.round(w * level);
                if (filled > 0) {
                    context.fillRect(x, y, filled, h, level < def.lowThreshold() ? dim(color) : color);
                }
            }
        } finally {
            context.popClip();
        }
    }

    /** -1 outside the box, 0 on the header (Overview), n on the n-th nutrient row. */
    public static int rowAt(double mouseX, double mouseY) {
        Bounds b = lastBounds;
        if (b == null || mouseX < b.x() || mouseX >= b.x() + b.width() || mouseY < b.y() || mouseY >= b.y() + b.height()) {
            return -1;
        }
        if (mouseY < lastFirstRowY) return 0;
        return (int) Math.floor((mouseY - lastFirstRowY) / lastRowH) + 1;
    }

    public static void forgetBounds() {
        lastBounds = null;
    }

    private static int dim(int argb) {
        int r = ((argb >> 16) & 0xFF) / 2;
        int g = ((argb >> 8) & 0xFF) / 2;
        int b = (argb & 0xFF) / 2;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
