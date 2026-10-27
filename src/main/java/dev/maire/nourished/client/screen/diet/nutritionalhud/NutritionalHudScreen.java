package dev.maire.nourished.client.screen.diet.nutritionalhud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.marie.framework.color.MarieColors;
import dev.marie.framework.ui.Theme;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.api.EditModeToggle;
import dev.marie.framework.ui.api.MarieModuleSettings;
import dev.marie.framework.ui.geometry.Anchor;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.hub.HubEntry;
import dev.marie.framework.ui.hub.HubPanel;
import dev.marie.framework.ui.hub.HubSidebarEntry;
import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import dev.maire.nourished.client.NourishedKeys;
import dev.maire.nourished.client.UiStatePersistence;
import dev.maire.nourished.client.colors.NourishedColors;
import dev.maire.nourished.client.screen.diet.DietEditModeToggle;
import dev.maire.nourished.client.screen.diet.DietHomeSections;
import dev.maire.nourished.client.screen.diet.DietScreen;
import dev.maire.nourished.client.screen.diet.dynamic.layout.DietLayout;
import dev.maire.nourished.client.screen.diet.dynamic.modules.ActiveEffectsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.BalanceComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.CaloriesComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.EatMoreComponent;
import dev.maire.nourished.client.screen.diet.dynamic.modules.RecentMealsComponent;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietScreenPersistence;
import dev.maire.nourished.client.screen.diet.dynamic.persistence.DietSettings;
import dev.maire.nourished.config.NutritionalPanelConfig;
import dev.maire.nourished.client.screen.diet.nutritionalhud.content.SoloIntakeBreakdownPane;
import dev.maire.nourished.client.screen.diet.nutritionalhud.content.SoloModulePane;
import dev.maire.nourished.client.screen.diet.nutritionalhud.content.TabModuleHosts;
import dev.marie.framework.ui.api.EditHostRenderer;
import dev.maire.nourished.client.screen.diet.nutritionalhud.content.ToggleGutterPane;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * The "Hub" Diet Screen mode: a sidebar window where each tab shows one module's box. The switch in
 * the window's top-right corner, just under the header label, opens the same full Editor window the
 * dynamic screen's edit mode uses.
 */
public final class NutritionalHudScreen extends Screen {

    private static final float FADE_DURATION_SEC = 0.15f;
    // HubPanel's collapsed strip is 20px tall and its window can't shrink below 140px.
    private static final int COLLAPSED_MAX_HEIGHT = 40;

    private final HubPanel hub = new HubPanel(
            Component.translatable("nourished.screen.diet.nutritionalhud.title"), "nourished.diet.nutritionalhud",
            UiStatePersistence.get(), DietScreenPersistence.inNutritionalPanel(this::viewEntries),
            NutritionalHudScreen::windowBackground,
            NutritionalHudScreen::windowBorder,
            () -> MarieColors.resolveColor(NourishedColors.DIET_TITLE),
            () -> MarieColors.resolveColor(NourishedColors.HUB_ACCENT),
            () -> Component.translatable("nourished.screen.diet.nutritionalhud.editor_label").getString())
            // Title colors both header labels; the sidebar tabs have their own Tab color.
            .labelColor(() -> MarieColors.resolveColor(NourishedColors.DIET_TITLE))
            .tabTextColor(() -> MarieColors.resolveColor(NourishedColors.DIET_TABS));

    // The dynamic screen's editor, built in the Nutritional Panel's scope so its options change this panel's boxes.
    private final HubPanel editor = DietScreenPersistence.inNutritionalPanel(() -> new HubPanel(
            Component.translatable("nourished.screen.diet.options_label"), "nourished.diet.hub",
            DietScreenPersistence.get(), DietScreen.scaleConfigEntries(),
            () -> MarieColors.resolveColor(NourishedColors.HUB_BACKGROUND),
            () -> MarieColors.resolveColor(NourishedColors.HUB_BORDER),
            () -> MarieColors.resolveColor(NourishedColors.HUB_TITLE),
            () -> MarieColors.resolveColor(NourishedColors.HUB_ACCENT)));
    private boolean editorOpen;
    // Inside the window's top-right corner, just under its header (16px + divider) with a 4px gap.
    private final EditModeToggle editToggle = DietEditModeToggle.builder(() -> hub.panelBounds(), () -> editorOpen, () -> editorOpen = !editorOpen)
            .anchor(Anchor.TOP_RIGHT, 8, 21)
            .visibleWhen(() -> hub.panelBounds().height() > COLLAPSED_MAX_HEIGHT)
            .build();

    private float fadeAlpha;
    private long fadeLastFrameNanos;
    private boolean fadeClockStarted;

    public NutritionalHudScreen() {
        super(Component.translatable("nourished.screen.diet.nutritionalhud.title"));
        DietScreenPersistence.inNutritionalPanel(() -> {
            NutritionalPanelConfig.get().migrateStyleInto(DietScreenPersistence.get(), DietSettings.PANEL_ID);
            return null;
        });
    }

    private MarieComponent hosted(String id, EditHostRenderer renderer) {
        return TabModuleHosts.newHost(() -> editorOpen).asComponent(id, renderer);
    }

    private List<HubSidebarEntry> viewEntries() {
        return List.of(
                new HubEntry("nourished.diet.hub.home", Component.translatable("nourished.screen.diet.home_label"),
                        new ToggleGutterPane(DietHomeSections.homePage())),
                tab(CaloriesComponent.ID, "nourished.screen.diet.calories_label", hosted(CaloriesComponent.ID, new SoloModulePane(CaloriesComponent.ID))),
                tab(BalanceComponent.ID, "nourished.screen.diet.balance_label", hosted(BalanceComponent.ID, new SoloModulePane(BalanceComponent.ID))),
                tab(RecentMealsComponent.ID, "nourished.screen.diet.recent_label", hosted(RecentMealsComponent.ID, new SoloModulePane(RecentMealsComponent.ID))),
                tab(EatMoreComponent.ID, "nourished.screen.diet.suggestion_label", hosted(EatMoreComponent.ID, new SoloModulePane(EatMoreComponent.ID))),
                tab(ActiveEffectsComponent.ID, "nourished.screen.diet.effects_label", hosted(ActiveEffectsComponent.ID, new SoloModulePane(ActiveEffectsComponent.ID))),
                tab(SoloIntakeBreakdownPane.ID, "nourished.screen.diet.intake", hosted(SoloIntakeBreakdownPane.ID, new SoloIntakeBreakdownPane()))
        );
    }

    private static HubEntry tab(String id, String labelKey, MarieComponent content) {
        return new HubEntry(id, Component.translatable(labelKey), new ToggleGutterPane(content));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // intentionally empty — suppress vanilla menu blur; the hub draws its own backdrop
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
        renderBackground(g, mx, my, pt);

        GuiGraphicsRenderContext context = new GuiGraphicsRenderContext(g, minecraft, Theme.DARK, pt);
        Bounds screen = new Bounds(0, 0, this.width, this.height);
        try {
            DietScreenPersistence.inNutritionalPanel(() -> {
                renderWindows(context, screen, mx, my);
                return null;
            });
        } finally {
            context.resetClip();
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        super.render(g, mx, my, pt);
    }

    // The window is this screen's panel, so the editor's Nutritional Panel page (panel color, opacity,
    // background shade, border, shadow, glow, pulse) styles it the way it styles the dynamic panel.
    private static int windowBackground() {
        int alpha = Math.max(0, Math.min(255, (int) Math.round(DietSettings.backgroundOpacity() * 255.0d)));
        int fill = (alpha << 24) | NourishedColors.rgb(NourishedColors.DIET_PANEL);
        return MarieModuleSettings.styledBackground(fill, DietScreenPersistence.get(), DietSettings.PANEL_ID);
    }

    private static int windowBorder() {
        return MarieModuleSettings.styledBorder(MarieColors.resolveColor(NourishedColors.BORDER), DietScreenPersistence.get(), DietSettings.PANEL_ID);
    }

    private void renderWindows(GuiGraphicsRenderContext context, Bounds screen, int mx, int my) {
        Bounds window = hub.panelBounds();
        if (window.height() > COLLAPSED_MAX_HEIGHT) {
            MarieModuleSettings.drawBoxGlow(context, DietScreenPersistence.get(), DietSettings.PANEL_ID,
                    window.x(), window.y(), window.width(), window.height());
        }
        hub.render(context, screen);
        if (editorOpen) {
            editor.render(context, screen);
        }
        // Drawn last so the editor window can never cover the switch that closes it.
        editToggle.render(context, mx, my);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (DietScreenPersistence.inNutritionalPanel(() -> clickWindows(mouseX, mouseY, button))) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickWindows(double mouseX, double mouseY, int button) {
        if (editToggle.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (editorOpen && editor.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return hub.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (DietScreenPersistence.inNutritionalPanel(() ->
                (editorOpen && editor.mouseReleased(mouseX, mouseY, button)) || hub.mouseReleased(mouseX, mouseY, button))) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (DietScreenPersistence.inNutritionalPanel(() ->
                (editorOpen && editor.mouseDragged(mouseX, mouseY, button)) || hub.mouseDragged(mouseX, mouseY, button))) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (DietScreenPersistence.inNutritionalPanel(() -> (editorOpen && editor.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
                || hub.mouseScrolled(mouseX, mouseY, scrollX, scrollY))) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NourishedKeys.OPEN_DIET_SCREEN.matches(keyCode, scanCode)) {
            onClose();
            while (NourishedKeys.OPEN_DIET_SCREEN.consumeClick()) {
                // drain the queued click so ClientEvents.onClientTick doesn't reopen next tick
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
