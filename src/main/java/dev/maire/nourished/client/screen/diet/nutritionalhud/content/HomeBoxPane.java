package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

import dev.marie.framework.color.MarieColors;
import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.ui.api.MarieStoredStyle;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.geometry.Size;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.screen.diet.DietHomeSections;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;

/**
 * Draws {@link DietHomeSections#homePage()} inside its own styled box — background, border, shadow,
 * glow and pulse, all driven by the "Home" editor entry through {@link MarieModuleSettings}, the same
 * system every other Diet box uses. {@link dev.marie.framework.ui.hub.HubInfoPage} itself has none of
 * this (it's a plain shared layout with no per-caller styling hook), so this wraps it rather than
 * editing it.
 *
 * <p>Hosted exactly like {@link dev.maire.nourished.client.screen.diet.dynamic.modules.CaloriesComponent}
 * and the other sub-boxes (see {@link HomeTabRenderer}): the box itself is drawn at whatever bounds the
 * tab's edit host gives it, so it drags and resizes the same way, floored at {@link
 * dev.marie.framework.ui.api.ResizeLimits#DEFAULT}'s 60% of its natural size — comfortably above the
 * ~70x119 minimum {@code EntityPreviewBox} itself always keeps the player preview at — so shrinking it
 * never actually hides the preview.
 *
 * <p>The page's content is rendered through the real {@code context} passed in, never a wrapper — its
 * 3D player preview only draws when {@code context instanceof GuiGraphicsRenderContext} (see {@code
 * EntityPreviewBox}), so any decorator here (e.g. to drive Text size/offset the way other boxes do)
 * would silently fall back to the "join a world" placeholder even with a player loaded. That's why the
 * Home editor entry has no Text size/Move Text/Hide Text of its own — only what this class applies
 * outside {@link MarieComponent#render}, on {@code context} itself, is safe to expose.
 */
public final class HomeBoxPane implements MarieComponent {

    private static final int PAD = 6;
    /** Matches {@link dev.maire.nourished.client.screen.diet.dynamic.options.DietOptionsPanel}'s default background opacity (204/255). */
    private static final double DEFAULT_OPACITY = 204.0d / 255.0d;

    private final MarieComponent content = DietHomeSections.homePage();

    @Override
    public String id() {
        return DietHomeSections.ID;
    }

    @Override
    public Constraint constraint() {
        return Constraint.preferred(naturalWidth(), naturalHeight());
    }

    /** This box's untouched width at scale 1.0 — the {@code natural} size {@link TabModuleHosts#track} resizes from. */
    public int naturalWidth() {
        return content.constraint().preferredSize().width() + PAD * 2;
    }

    public int naturalHeight() {
        Size preferred = content.constraint().preferredSize();
        return preferred.height() + PAD * 2;
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        PersistenceProvider store = DietScreenPersistence.get();
        String id = DietHomeSections.ID;
        if (MarieModuleSettings.isWindowHidden(store, id)) {
            return;
        }

        int baseFill = MarieStoredStyle.applyOpacity(NourishedColors.surfaceRgb(), store, id, DEFAULT_OPACITY);
        int fill = MarieModuleSettings.styledBackground(baseFill, store, id);
        int border = MarieModuleSettings.styledBorder(MarieColors.resolveColor(NourishedColors.HOME_BORDER), store, id);
        MarieModuleSettings.drawBoxGlow(context, store, id, bounds.x(), bounds.y(), bounds.width(), bounds.height());
        context.drawRoundedRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 1, fill, border);

        Bounds inner = new Bounds(bounds.x() + PAD, bounds.y() + PAD,
                Math.max(0, bounds.width() - PAD * 2), Math.max(0, bounds.height() - PAD * 2));
        context.pushClip(inner.x(), inner.y(), inner.width(), inner.height());
        try {
            content.render(context, inner);
        } finally {
            context.popClip();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return content.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
