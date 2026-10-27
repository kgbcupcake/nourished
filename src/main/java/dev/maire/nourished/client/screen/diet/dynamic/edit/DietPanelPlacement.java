package dev.maire.nourished.client.screen.diet.dynamic.edit;

import dev.marie.framework.ui.api.EditCommit;
import dev.marie.framework.ui.api.PlacementStrategy;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.geometry.Bounds;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietPanelLayoutResolver;

/**
 * Saves the Diet Screen panel's own move/resize in absolute screen pixels. Only a left-edge (or
 * bottom-left corner) resize changes its left margin: the width it gained or lost on that side becomes
 * dead space the left column's content stays clear of, while a right-side resize goes to the right column.
 */
final class DietPanelPlacement implements PlacementStrategy {

    @Override
    public ComponentState toState(Bounds committed, EditCommit commit, ComponentState existing) {
        int persistedMargin = DietPanelLayoutResolver.persistedLeftMargin();
        int leftMargin = commit.resized() && commit.leftEdge()
                ? Math.max(0, persistedMargin + (committed.width() - commit.start().width()))
                : persistedMargin;
        double contentScale = existing != null ? existing.contentScale() : ComponentState.DEFAULT_CONTENT_SCALE;
        double paddingScale = existing != null ? existing.paddingScale() : ComponentState.DEFAULT_PADDING_SCALE;
        return new ComponentState(committed.x(), committed.y(), committed.width(), committed.height(), false,
                commit.widthManual(existing), commit.heightManual(existing), leftMargin, contentScale, paddingScale);
    }

    @Override
    public Bounds toBounds(ComponentState state, Bounds natural) {
        return natural;
    }
}
