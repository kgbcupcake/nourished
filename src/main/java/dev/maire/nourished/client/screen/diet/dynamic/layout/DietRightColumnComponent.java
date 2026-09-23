package dev.maire.nourished.client.screen.diet.dynamic.layout;

import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.Container;
import dev.marie.framework.ui.Layout;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.layout.VerticalLayout;
import dev.maire.nourished.client.screen.diet.dynamic.modules.DietScreenModules;
import dev.maire.nourished.client.screen.diet.dynamic.modules.IntakeBarComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.IntakeHeaderComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * The Intake Breakdown ("right") column, rewritten as a thin {@link Container} mirroring {@link
 * DietLeftColumnComponent} — every row and the header are now independent {@link MarieComponent}s
 * built via {@link DietScreenModules#build(String, DietLayout.Layout, int)} against {@link
 * DietScreenModules#RIGHT_COLUMN_KEY} rather than hand-drawn inline here. See {@link
 * dev.maire.nourished.client.screen.diet.dynamic.modules.IntakeHeaderComponent}/{@link
 * IntakeBarComponent} for what used to be this class's {@code render} body.
 */
public final class DietRightColumnComponent implements Container {

    /** Local Y the header starts at — matches the legacy hand-drawn header's fixed {@code y = 30}. */
    public static final int HEADER_START_LOCAL_Y = 30;

    private final DietLayout.Layout layout;
    private final TrackingData data;
    private final int width;
    private final int height;
    private final List<MarieComponent> children;
    private final Layout columnLayout;

    private Bounds headerRenderBounds;
    private final java.util.Map<String, Bounds> barRenderBounds = new java.util.HashMap<>();

    DietRightColumnComponent(TrackingData data, List<String> bars, java.util.Map<String, Float> display, DietLayout.Layout layout, int width, int height) {
        this.data = data;
        this.layout = layout;
        this.width = width;
        this.height = height;
        this.children = DietScreenModules.build(DietScreenModules.RIGHT_COLUMN_KEY, layout, HEADER_START_LOCAL_Y, DietLayout.rightColumnContentX(layout));
        this.columnLayout = new VerticalLayout(0);
    }

    public IntakeHeaderComponent headerComponent() {
        return DietScreenModules.find(children, IntakeHeaderComponent.class);
    }

    public List<IntakeBarComponent> barComponents() {
        List<IntakeBarComponent> result = new ArrayList<>();
        for (MarieComponent child : children) {
            if (child instanceof IntakeBarComponent bar) {
                result.add(bar);
            }
        }
        return result;
    }

    /**
     * Overrides the bounds {@link #render} passes to the header/rows instead of their own {@code
     * resolvedBounds()} — for edit mode's live drag/resize preview. A {@code null} entry (or a key
     * absent from {@code barBoundsById}) means "use that child's own resolvedBounds()."
     */
    public void setIntakeRenderBounds(Bounds headerBounds, java.util.Map<String, Bounds> barBoundsById) {
        this.headerRenderBounds = headerBounds;
        this.barRenderBounds.clear();
        if (barBoundsById != null) {
            this.barRenderBounds.putAll(barBoundsById);
        }
    }

    @Override
    public String id() {
        return "nourished.diet.panel.right";
    }

    @Override
    public List<MarieComponent> children() {
        return children;
    }

    @Override
    public void addChild(MarieComponent child) {
        children.add(child);
    }

    @Override
    public void removeChild(MarieComponent child) {
        children.remove(child);
    }

    @Override
    public Layout layout() {
        return columnLayout;
    }

    @Override
    public Constraint constraint() {
        return Constraint.fixed(width, height);
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        if (data == null) {
            return;
        }
        for (MarieComponent child : children) {
            if (!child.visibilityRule().isVisible()) {
                continue;
            }
            Bounds renderBounds;
            if (child instanceof IntakeHeaderComponent header) {
                renderBounds = headerRenderBounds != null ? headerRenderBounds : header.resolvedBounds();
                if (!header.isVisible()) {
                    continue;
                }
            } else if (child instanceof IntakeBarComponent bar) {
                if (!bar.isVisible()) {
                    continue;
                }
                Bounds override = barRenderBounds.get(bar.id());
                renderBounds = override != null ? override : bar.resolvedBounds();
            } else if (child instanceof dev.marie.framework.ui.component.SelfPositioningModule self) {
                renderBounds = self.resolvedBounds();
            } else {
                continue;
            }
            child.render(context, renderBounds);
        }
    }
}
