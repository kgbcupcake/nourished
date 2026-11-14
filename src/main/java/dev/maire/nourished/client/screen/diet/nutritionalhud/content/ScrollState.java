package dev.maire.nourished.client.screen.diet.nutritionalhud.content;

/**
 * A shared vertical scroll offset between a tab's scroll-wheel input ({@link ScrollableTabPane}) and
 * its row-stacking content (e.g. {@link SoloIntakeBreakdownPane}), which alone knows its content and
 * viewport height each frame.
 */
public final class ScrollState {

    private int offset;
    private int maxOffset;

    public int offset() {
        return offset;
    }

    /** Called once per render by the content that knows how far there is to scroll this frame. */
    public void setMaxOffset(int max) {
        maxOffset = Math.max(0, max);
        offset = Math.max(0, Math.min(offset, maxOffset));
    }

    void scrollBy(int delta) {
        offset = Math.max(0, Math.min(maxOffset, offset + delta));
    }
}
