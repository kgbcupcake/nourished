package dev.maire.nourished.client;

import dev.maire.nourished.client.config.EffectPreview;
import dev.maire.nourished.client.screen.diet.DietScreen;
import dev.maire.nourished.client.screen.diet.classic.ClassicDietScreen;
import dev.maire.nourished.config.NourishedClientConfig;
import dev.maire.nourished.config.NourishedConfig;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.book.NourishedBookItems;
import dev.maire.nourished.core.nutrition.ExcludedTooltipPulseRegistry;
import dev.maire.nourished.core.nutrition.NutrientClassificationLookup;
import dev.marie.framework.tooltips.MarieTooltipHelper;
import dev.marie.framework.tooltips.TooltipColorRegistry;
import dev.marie.framework.tooltips.TooltipMessageRegistry;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.ui.api.MarieCommandCenter;
import dev.marie.framework.ui.api.EditModeCoordinator;
import dev.marie.framework.ui.itemeditor.ItemEditorApi;
import dev.marie.framework.util.MarieRegistryUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        ResourceLocation itemId = MarieRegistryUtils.itemKey(stack.getItem());
        List<Component> nourishedLines = itemId != null && NutrientClassificationLookup.isExcluded(itemId.toString())
                ? excludedTooltipLines(itemId.toString())
                : MarieTooltipHelper.getTooltipLines(stack);
        if (nourishedLines.isEmpty()) {
            return;
        }
        var lines = event.getToolTip();
        lines.add(Component.empty());
        if (isKeyHeld(NourishedKeys.SHOW_TOOLTIP_DETAILS)) {
            lines.addAll(nourishedLines);
        } else {
            lines.add(Component.translatable("nourished.tooltip.holdForDetails",
                    NourishedKeys.SHOW_TOOLTIP_DETAILS.getTranslatedKeyMessage()));
        }
    }

    /**
     * {@link KeyMapping#isDown()} only reflects GLFW key callbacks, which can lag or get swallowed
     * while a screen has keyboard focus (e.g. inventory search boxes). Tooltip gating needs the
     * actual current hardware state every frame, so poll it directly the same way
     * {@link Screen#hasShiftDown()} does for its own modifier checks.
     */
    private static boolean isKeyHeld(KeyMapping mapping) {
        var key = mapping.getKey();
        if (key.getType() != InputConstants.Type.KEYSYM) {
            return mapping.isDown();
        }
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, key.getValue());
    }

    /**
     * Builds the "Excluded from nutrition tracking" line ourselves, instead of {@link
     * MarieTooltipHelper#getTooltipLines}, for any item Nourished considers excluded (editor
     * toggle, scanner exclusion, or MariesLib's own escape hatch — see {@link
     * NutrientClassificationLookup#isExcluded}). MariesLib's own helper only recognizes its own
     * {@code ExcludedItemsRegistry}/{@code ScannerSpecRegistry} exclusions for this branch, so an
     * item excluded purely via Nourished's item editor would otherwise fall through to the
     * generic "Unclassified" line instead. Message/color overrides come from the same {@code
     * TooltipMessageRegistry}/{@code TooltipColorRegistry} "excluded" lookups MariesLib's own
     * branch uses, so behavior for non-editor exclusions is unchanged; pulse (see {@link
     * ExcludedTooltipPulseRegistry}) has no MariesLib equivalent and is applied here only.
     */
    private static List<Component> excludedTooltipLines(String itemId) {
        List<Component> lines = new ArrayList<>();
        String modId = Nourished.MODID;
        lines.add(Component.literal("✦ " + modId).withStyle(ChatFormatting.GOLD));

        Optional<String> override = TooltipMessageRegistry.getForItem(modId, itemId, "excluded");
        Component excludedLine = override.isPresent()
                ? Component.literal(override.get())
                : Component.translatable(modId + ".tooltip.excluded");
        int baseColor = TooltipColorRegistry.getForItem(modId, itemId, "excluded")
                .orElse(ChatFormatting.DARK_GRAY.getColor());
        int color = ExcludedTooltipPulseRegistry.isEnabled(itemId)
                ? pulseColor(baseColor, ExcludedTooltipPulseRegistry.getSpeed(itemId))
                : baseColor;
        lines.add(excludedLine.copy().withStyle(Style.EMPTY.withColor(color)));
        return lines;
    }

    /** Oscillates {@code rgb}'s brightness over time; tooltip text color has no usable alpha channel (see TOOLTIP_COLORS_README.md), so pulse has to modulate the RGB channels themselves. */
    private static int pulseColor(int rgb, ExcludedTooltipPulseRegistry.Speed speed) {
        float periodMs = switch (speed) {
            case SLOW -> 1400f;
            case FAST -> 450f;
            default -> 800f;
        };
        float t = (Mth.sin(System.currentTimeMillis() / periodMs) + 1f) / 2f;
        float factor = 0.35f + 0.65f * t;
        int r = Math.round(((rgb >> 16) & 0xFF) * factor);
        int g = Math.round(((rgb >> 8) & 0xFF) * factor);
        int b = Math.round((rgb & 0xFF) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
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
