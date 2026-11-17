package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.api.EditHostRenderer;
import dev.marie.framework.ui.api.ModuleEditHost;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.geometry.Bounds;

import java.util.function.BooleanSupplier;

/**
 * Hosts a {@link ScrollingBoxPane} in its tab like {@link HomeTabRenderer} hosts the Home box (drag,
 * resize, snap in edit mode). Outside edit mode the host ignores input, so {@link #wrap} passes
 * clicks, scrollbar drags, the wheel and keys to the box's content instead.
 */
public final class BoxTabRenderer implements EditHostRenderer {

    private final ScrollingBoxPane box;

    public BoxTabRenderer(ScrollingBoxPane box) {
        this.box = box;
    }

    @Override
    public void render(RenderContext context, Bounds area, ModuleEditHost host) {
        TabModuleHosts.addTabSnap(host, area);
        int top = area.y() + TabModuleHosts.GLOW_ROOM;
        int bottom = area.y() + area.height() - TabModuleHosts.GLOW_ROOM;
        box.setMaxHeight(bottom - top);
        Bounds at = TabModuleHosts.track(host, area, box, area.x() + TabModuleHosts.GLOW_ROOM, top);
        // a box dragged or resized past the tab bottom is drawn cut to it, so its content scrolls
        int visibleH = Math.max(1, Math.min(at.height(), bottom - at.y()));
        box.render(context, new Bounds(at.x(), at.y(), at.width(), visibleH));
    }

    /** The hosted tab plus input for the box's pages while not editing. */
    public MarieComponent wrap(MarieComponent hosted, BooleanSupplier editing) {
        return new MarieComponent() {
            @Override
            public String id() {
                return hosted.id();
            }

            @Override
            public Constraint constraint() {
                return hosted.constraint();
            }

            @Override
            public void render(RenderContext context, Bounds bounds) {
                hosted.render(context, bounds);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (editing.getAsBoolean()) return hosted.mouseClicked(mouseX, mouseY, button);
                return button == 0 && box.clickContent(mouseX, mouseY);
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                if (box.releaseContent()) return true;
                return hosted.mouseReleased(mouseX, mouseY, button);
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                if (!editing.getAsBoolean() && box.dragContent(mouseY)) return true;
                return hosted.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            }

            @Override
            public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
                return !editing.getAsBoolean() && box.scrollContent(mouseX, mouseY, scrollY);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                return !editing.getAsBoolean() && box.keyContent(keyCode);
            }
        };
    }
}
