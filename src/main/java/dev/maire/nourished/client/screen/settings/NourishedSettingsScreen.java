package dev.maire.nourished.client.screen.settings;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.ui.Theme;
import dev.marie.framework.ui.api.MarieToolbox;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.hub.HubEntry;
import dev.marie.framework.ui.hub.HubPanel;
import dev.marie.framework.ui.hub.HubSidebarEntry;
import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import dev.maire.nourished.client.NourishedKeys;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.config.NourishedConfigScreen;
import dev.maire.nourished.client.config.NourishedConfigSharedWidgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

import static dev.maire.nourished.client.screen.settings.SettingsCommit.t;

/** The curated in-game settings window: the core feature switches and balance numbers. */
public final class NourishedSettingsScreen extends Screen {

    private static final float FADE_DURATION_SEC = 0.15f;
    private static final String P = "nourished.screen.settings.";

    private final HubPanel hub;
    private float fadeAlpha;
    private long fadeLastFrameNanos;
    private boolean fadeClockStarted;

    public NourishedSettingsScreen() {
        super(Component.translatable(P + "title"));
        hub = new HubPanel(
                Component.translatable(P + "title"), "nourished.settings",
                UiStatePersistence.get(), entries(),
                () -> MarieColors.resolveColor(NourishedColors.HUB_BACKGROUND),
                () -> MarieColors.resolveColor(NourishedColors.HUB_BORDER),
                () -> MarieColors.resolveColor(NourishedColors.HUB_TITLE),
                () -> MarieColors.resolveColor(NourishedColors.HUB_ACCENT),
                () -> NourishedConfigSharedWidgets.isMultiplayer() ? t(P + "serverLocked") : "")
                .tabTextColor(() -> MarieColors.resolveColor(NourishedColors.DIET_TABS));
    }

    private List<HubSidebarEntry> entries() {
        return List.of(
                new HubEntry("features", Component.translatable(P + "features.title"), FeaturesPage.build()),
                new HubEntry("balance", Component.translatable(P + "balance.title"), BalancePage.build()),
                new HubEntry("advanced", Component.translatable(P + "advanced.title"), advancedPage()));
    }

    private MarieComponent advancedPage() {
        return MarieToolbox.panel(t(P + "advanced.title"))
                .tab(t(P + "advanced.title"))
                .button(t(P + "advanced.fullConfig"), t(P + "advanced.open"),
                        () -> minecraft.setScreen(NourishedConfigScreen.create(this)), () -> {})
                .build();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // no vanilla blur, the hub draws its own backdrop
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.nanoTime();
        if (!fadeClockStarted) {
            fadeLastFrameNanos = now;
            fadeClockStarted = true;
        }
        float dt = (now - fadeLastFrameNanos) / 1_000_000_000f;
        fadeLastFrameNanos = now;
        fadeAlpha = Mth.clamp(fadeAlpha + dt / FADE_DURATION_SEC, 0f, 1f);

        RenderSystem.setShaderColor(1f, 1f, 1f, fadeAlpha);
        GuiGraphicsRenderContext context = new GuiGraphicsRenderContext(g, minecraft, Theme.DARK, pt);
        try {
            hub.render(context, new Bounds(0, 0, this.width, this.height));
        } finally {
            context.resetClip();
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return hub.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return hub.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return hub.mouseDragged(mouseX, mouseY, button) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return hub.mouseScrolled(mouseX, mouseY, scrollX, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NourishedKeys.OPEN_SETTINGS.matches(keyCode, scanCode)) {
            onClose();
            while (NourishedKeys.OPEN_SETTINGS.consumeClick()) {
                // drain so onClientTick doesn't reopen it next tick
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
