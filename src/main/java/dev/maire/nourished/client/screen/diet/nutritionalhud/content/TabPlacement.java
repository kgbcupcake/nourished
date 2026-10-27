package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

import dev.marie.framework.ui.api.EditCommit;
import dev.marie.framework.ui.api.PlacementStrategy;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.geometry.Bounds;

/**
 * Where a box sits inside a Nutritional Panel tab: saved as an offset from its own default spot, plus
 * its size once resized. Never shrunk to fit the tab (a small window clips it instead), and never
 * clamped by the dynamic screen's column rules, so a resize sticks at the size it was dragged to.
 */
final class TabPlacement implements PlacementStrategy {

    private static final ComponentState BLANK = new ComponentState(0, 0, 0, 0, false, false, false, 0);

    private final Bounds home;
    private final Bounds region;

    TabPlacement(Bounds home, Bounds region) {
        this.home = home;
        this.region = region;
    }

    @Override
    public ComponentState toState(Bounds committed, EditCommit commit, ComponentState existing) {
        ComponentState base = existing != null ? existing : BLANK;
        return new ComponentState(committed.x() - home.x(), committed.y() - home.y(), committed.width(), committed.height(),
                base.collapsed(), commit.widthManual(existing), commit.heightManual(existing),
                base.leftMargin(), base.contentScale(), base.paddingScale());
    }

    @Override
    public Bounds toBounds(ComponentState state, Bounds natural) {
        if (state == null) {
            return natural;
        }
        return new Bounds(natural.x() + state.x(), natural.y() + state.y(),
                state.widthManual() ? state.width() : natural.width(),
                state.heightManual() ? state.height() : natural.height());
    }

    @Override
    public Bounds clampRegion() {
        return region;
    }
}
