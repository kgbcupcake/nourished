package dev.maire.nourished.client.hud.dynamic.edit;

import dev.marie.framework.ui.api.EditCommit;
import dev.marie.framework.ui.api.PlacementStrategy;
import dev.marie.framework.ui.component.AutoGrowPanelContainer;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.geometry.Bounds;

/**
 * Saves the Nutrient HUD panel's move/resize in absolute screen pixels, keeping its text scale. A
 * left-edge (or bottom-left corner) resize grows its left margin so the content stays put on screen.
 */
final class HudPanelPlacement implements PlacementStrategy {

    @Override
    public ComponentState toState(Bounds committed, EditCommit commit, ComponentState existing) {
        int persistedMargin = existing != null ? existing.leftMargin() : 0;
        int leftMargin = AutoGrowPanelContainer.leftMarginAfterResize(persistedMargin,
                commit.start().width(), committed.width(), commit.resized() && commit.leftEdge());
        return new ComponentState(committed.x(), committed.y(), committed.width(), committed.height(), false,
                commit.widthManual(existing), commit.heightManual(existing), leftMargin,
                HudEditTarget.persistedContentScale(), HudEditTarget.persistedPaddingScale());
    }

    @Override
    public Bounds toBounds(ComponentState state, Bounds natural) {
        return natural;
    }
}
