package dev.maire.nourished.client;

import dev.maire.nourished.client.config.EffectPreview;
import dev.maire.nourished.client.screen.diet.DietScreen;
import dev.maire.nourished.client.screen.diet.classic.ClassicDietScreen;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.book.NourishedBookItems;
import dev.marie.framework.tooltips.MarieTooltipHelper;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.ui.api.MarieCommandCenter;
import dev.marie.framework.ui.api.EditModeCoordinator;
import dev.marie.framework.ui.itemeditor.ItemEditorApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class ClientEvents {

    private static final ItemStack DIET_BUTTON_ICON = new ItemStack(Items.GOLDEN_APPLE);

    private ClientEvents() {}

    private static final class InventoryDietButton extends Button {
        InventoryDietButton(int x, int y) {
            super(x, y, 20, 20, Component.empty(),
                    b -> Minecraft.getInstance().setScreen(openDietScreen()),
                    b -> Component.empty());
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.renderItem(DIET_BUTTON_ICON, getX() + 2, getY() + 2);
            if (isHovered()) {
                graphics.renderTooltip(Minecraft.getInstance().font,
                        Component.translatable("nourished.screen.diet.tooltip.nourish"),
                        mouseX, mouseY);
            }
        }
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        if (!FeatureFlagCache.enableTrackingScreen()) return;
        // Only gates the button's own presence — deliberately does NOT touch
        // NourishedKeys.OPEN_DIET_SCREEN's handling in onClientTick below, so the keybind keeps
        // opening the Diet Screen regardless of this setting.
        if (!NourishedClientConfig.get().showDietScreenButton()) return;

        int x = screen.getGuiLeft() - 26;
        int y = screen.getGuiTop() + 60;

        event.addListener(new InventoryDietButton(x, y));
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!NourishedConfig.get().enableFoodTooltips()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        var nourishedLines = MarieTooltipHelper.getTooltipLines(stack);
        if (nourishedLines.isEmpty()) {
            return;
        }
        var lines = event.getToolTip();
        lines.add(Component.empty());
        if (NourishedKeys.SHOW_TOOLTIP_DETAILS.isDown()) {
            lines.addAll(nourishedLines);
        } else {
            lines.add(Component.translatable("nourished.tooltip.holdForDetails",
                    NourishedKeys.SHOW_TOOLTIP_DETAILS.getTranslatedKeyMessage()));
        }
    }

    /** Pulsing magenta glow on the guide book's tooltip border, to match its Epic rarity. */
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        if (!event.getItemStack().is(NourishedBookItems.NOURISHED_BOOK.get())) {
            return;
        }
        float pulse = (Mth.sin(System.currentTimeMillis() / 300f) + 1f) / 2f;
        int topAlpha = (int) Mth.lerp(pulse, 170, 255);
        int bottomAlpha = (int) Mth.lerp(pulse, 80, 150);
        event.setBorderStart((topAlpha << 24) | 0xE060FF);
        event.setBorderEnd((bottomAlpha << 24) | 0x8000C8);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        EffectPreview.tick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        while (NourishedKeys.OPEN_DIET_SCREEN.consumeClick()) {
            if (!FeatureFlagCache.enableTrackingScreen()) {
                continue;
            }
            mc.setScreen(openDietScreen());
        }
        while (NourishedKeys.OPEN_SCALE_CONFIG.consumeClick()) {
            if (!FeatureFlagCache.enableTrackingScreen()) {
                continue;
            }
            openOrToggleScaleConfig();
        }
        while (NourishedKeys.OPEN_COMMAND_CENTER.consumeClick()) {
            MarieCommandCenter.openScreen();
        }
        while (NourishedKeys.OPEN_ITEM_EDITOR.consumeClick()) {
            openItemEditor(mc);
        }
        while (NourishedKeys.EDIT_ALL_HUDS.consumeClick()) {
            EditModeCoordinator.toggleAll();
        }
    }

    /**
     * {@code onClientTick}'s {@code consumeClick()} polling only runs while no screen is open (it
     * bails out early whenever {@code mc.screen != null}), same as vanilla's own key-binding
     * handling — so pressing the item editor's key while the inventory (or any other screen) is
     * open never reached {@code openItemEditor} at all. This intercepts the raw key press at the
     * screen level instead, specifically so the editor can be opened while looking at JEI's list in
     * the inventory.
     *
     * <p>Since a screen (the inventory) is already open here, this calls {@code toggleOverlay}
     * instead of {@code open} — {@code open} would replace that screen with the editor via {@code
     * Minecraft#setScreen}, closing the inventory (and JEI's list with it) instead of leaving it up.
     * {@code toggleOverlay} draws the same editor window on top without touching {@code mc.screen}
     * at all, so the inventory and JEI both stay exactly as they were.
     */
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!NourishedKeys.OPEN_ITEM_EDITOR.matches(event.getKeyCode(), event.getScanCode())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        RecipeManager recipeManager = mc.level != null ? mc.level.getRecipeManager() : null;
        ItemEditorApi.toggleOverlay(Nourished.MODID, ItemStack.EMPTY, recipeManager);
        event.setCanceled(true);
    }

    /**
     * Opens the item value editor with no item selected, so a player drags any item straight out
     * of JEI's list onto it — the editor's slot is a JEI ghost-ingredient target regardless of
     * what's in it, not limited to a pre-scanned list.
     */
    private static void openItemEditor(Minecraft mc) {
        RecipeManager recipeManager = mc.level != null ? mc.level.getRecipeManager() : null;
        ItemEditorApi.open(mc.screen, Nourished.MODID, ItemStack.EMPTY, recipeManager);
    }

    private static Screen openDietScreen() {
        return NourishedClientConfig.get().dietScreenClassicMode() ? new ClassicDietScreen() : new DietScreen();
    }

    /**
     * Ensures the dynamic Diet Screen is open (opening a fresh one, regardless of the
     * classic-renderer preference, if it isn't already the active screen), toggles its
     * scale-config sliders' visibility, and enters edit mode — same as if the player had pressed
     * J themselves — so the panel is immediately draggable without a separate manual step.
     * {@link dev.marie.framework.ui.edit.EditModeController#enter()} is a no-op if edit mode is
     * already active (e.g. the player pressed J before opening the panel), so this never
     * double-enters or re-toggles it off. Shared by OPEN_SCALE_CONFIG's own keybind handler above
     * and the Command Center's "Text Scale & Padding" card — the two are the same action from
     * different entry points, so both call this instead of duplicating it.
     */
    public static void openOrToggleScaleConfig() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DietScreen dietScreen) {
            dietScreen.toggleScaleConfigVisible();
            dietScreen.marieEditModeController().enter();
            return;
        }
        DietScreen screen = new DietScreen();
        mc.setScreen(screen);
        screen.toggleScaleConfigVisible();
        screen.marieEditModeController().enter();
    }
}
