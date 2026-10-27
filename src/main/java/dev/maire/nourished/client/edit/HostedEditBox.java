package dev.maire.nourished.client.edit;

import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.api.EditCapabilities;
import dev.marie.framework.ui.api.EditableModule;
import dev.marie.framework.ui.api.PlacementStrategy;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.geometry.Size;

/**
 * A box as a {@code ModuleEditHost} sees it, for screens that draw their real boxes themselves: its id,
 * resize clamp, what it may do, which siblings it snaps against and where it saves. It never draws.
 */
public final class HostedEditBox implements EditableModule {

    private final String id;
    private final Bounds bounds;
    private final Constraint resize;
    private final EditCapabilities capabilities;
    private final String snapGroup;
    private final String sizeSnapGroup;
    private final PlacementStrategy placement;
    private final boolean editable;

    public HostedEditBox(String id, Bounds bounds, Constraint resize, EditCapabilities capabilities,
                         String snapGroup, String sizeSnapGroup, PlacementStrategy placement) {
        this(id, bounds, resize, capabilities, snapGroup, sizeSnapGroup, placement, true);
    }

    /** {@code editable} false skips the box this frame: no handles, no gestures. */
    public HostedEditBox(String id, Bounds bounds, Constraint resize, EditCapabilities capabilities,
                         String snapGroup, String sizeSnapGroup, PlacementStrategy placement, boolean editable) {
        this.id = id;
        this.bounds = bounds;
        this.resize = resize;
        this.capabilities = capabilities;
        this.snapGroup = snapGroup;
        this.sizeSnapGroup = sizeSnapGroup;
        this.placement = placement;
        this.editable = editable;
    }

    @Override
    public boolean isEditable() {
        return editable;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Size naturalSize() {
        return resize.preferredSize();
    }

    @Override
    public Constraint resizeConstraint() {
        return resize;
    }

    @Override
    public EditCapabilities editCapabilities() {
        return capabilities;
    }

    @Override
    public PlacementStrategy placementStrategy() {
        return placement;
    }

    @Override
    public String snapGroup() {
        return snapGroup;
    }

    @Override
    public String sizeSnapGroup() {
        return sizeSnapGroup;
    }

    @Override
    public Bounds resolvedBounds() {
        return bounds;
    }

    @Override
    public int localHeight() {
        return bounds.height();
    }

    @Override
    public Constraint constraint() {
        return resize;
    }

    @Override
    public void render(RenderContext context, Bounds drawAt) {
    }
}
