package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.api.ContentWrapper;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.geometry.Bounds;

/** A tab's content with its right edge pulled in, so the edit toggle in the window's corner never covers it. */
public final class ToggleGutterPane implements MarieComponent, ContentWrapper {

    public static final int TOGGLE_GUTTER = 16;

    private final MarieComponent inner;

    public ToggleGutterPane(MarieComponent inner) {
        this.inner = inner;
    }

    @Override
    public MarieComponent wrappedContent() {
        return inner;
    }

    @Override
    public String id() {
        return inner.id();
    }

    @Override
    public Constraint constraint() {
        return inner.constraint();
    }

    @Override
    public void render(RenderContext context, Bounds bounds) {
        inner.render(context, new Bounds(bounds.x(), bounds.y(), Math.max(0, bounds.width() - TOGGLE_GUTTER), bounds.height()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return inner.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return inner.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return inner.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return inner.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return inner.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return inner.charTyped(codePoint, modifiers);
    }
}
