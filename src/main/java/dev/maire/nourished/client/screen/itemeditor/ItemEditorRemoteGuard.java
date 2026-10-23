package dev.maire.nourished.client.screen.itemeditor;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.toolbox.OptionRow;
import dev.marie.framework.ui.toolbox.OptionStyle;
import dev.maire.nourished.core.Nourished;
import net.minecraft.client.Minecraft;

import java.util.function.BooleanSupplier;

/**
 * Item editor pages write their per-item registries to this process's own config directory, which
 * only the integrated server (singleplayer / LAN host) also reads. Connected to a remote server,
 * those writes would silently never reach the server that actually classifies food, so the
 * gameplay-affecting pages stay read-only there and show {@link #noticeRow()} instead.
 */
@ApiStatus.Internal
final class ItemEditorRemoteGuard {

    private ItemEditorRemoteGuard() {}

    static boolean isRemoteServer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getConnection() != null && !mc.hasSingleplayerServer();
    }

    /** True (and logs why) when a page's save must be skipped because the edit couldn't take effect. */
    static boolean blockSave(String page) {
        if (!isRemoteServer()) {
            return false;
        }
        Nourished.LOGGER.warn("[ItemEditor] {} edits are local-only and were not saved: connected to a remote server", page);
        return true;
    }

    static OptionRow noticeRow() {
        return new NoticeRow();
    }

    private static final class NoticeRow implements OptionRow {

        private static final int HEIGHT = 12;
        private static final String TEXT = "Read-only on a remote server — edit in singleplayer or the server's config";

        @Override
        public int height() {
            return HEIGHT;
        }

        @Override
        public void enabledWhen(BooleanSupplier enabled) {}

        @Override
        public void render(RenderContext context, Bounds bounds) {
            context.drawText(TEXT, bounds.x(), bounds.y() + 1, OptionStyle.labelColor(context, false), OptionStyle.TEXT_SCALE);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY) {
            return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY) {
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY) {
            return false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
            return false;
        }
    }
}
