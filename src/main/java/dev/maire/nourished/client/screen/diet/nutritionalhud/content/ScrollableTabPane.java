package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.geometry.Bounds;

/**
 * Adds mouse-wheel scrolling over {@code inner}, driven by a shared {@link ScrollState} — a tab built
 * on {@link dev.marie.framework.ui.api.ModuleEditHost#asComponent} has no scroll handling of its own
 * ({@code HostTabContent} forwards clicks/drags into the host but not wheel events), so when its rows
 * overflow a small window there was no way to reach the ones past the bottom edge. Everything else
 * passes straight through to {@code inner}.
 */
public final class ScrollableTabPane implements MarieComponent {

    /** Matches {@code HubPanel}/{@code HubInfoPage}'s own scroll step. */
    private static final int SCROLL_STEP = 20;

    private final MarieComponent inner;
    private final ScrollState scroll;
    private Bounds lastBounds = new Bounds(0, 0, 0, 0);

    public ScrollableTabPane(MarieComponent inner, ScrollState scroll) {
        this.inner = inner;
        this.scroll = scroll;
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
        lastBounds = bounds;
        inner.render(context, bounds);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return inner.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return inner.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0 || !lastBounds.contains((int) mouseX, (int) mouseY)) {
            return inner.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scroll.scrollBy((int) (-Math.signum(scrollY) * SCROLL_STEP));
        return true;
    }
}
