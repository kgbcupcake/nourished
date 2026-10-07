package dev.maire.nourished.client.screen.itemeditor;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.itemeditor.ItemEditorPage;
import dev.marie.framework.ui.itemeditor.ItemEditorScreenProvider;
import dev.marie.framework.ui.scaleconfig.colorpicker.PickerWindow;
import dev.marie.framework.ui.toolbox.CycleOption;
import dev.marie.framework.ui.toolbox.OptionLayout;
import dev.marie.framework.ui.toolbox.TextFieldOption;
import dev.marie.framework.ui.toolbox.ToggleOption;
import dev.marie.framework.ui.toolbox.colorpicker.ColorSlot;
import dev.maire.nourished.core.nutrition.ExcludedTooltipOverrides;
import dev.maire.nourished.core.nutrition.ExcludedTooltipPulseRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * File &gt; Exclude Tooltip: per-item styling for the "Excluded from nutrition tracking" tooltip
 * line, for the item currently loaded in MariesLib's generic item editor. Message and color write
 * straight into MariesLib's own tooltip override files via {@link ExcludedTooltipOverrides} (the
 * same files {@code config.nourished.tooltips} config options already expose for hand-editing);
 * Pulse is Nourished-only state (see {@link ExcludedTooltipPulseRegistry}), applied in {@code
 * ClientEvents#onItemTooltip}. Glow is left as a disabled placeholder row: MariesLib's glow effect
 * only draws through its own {@code RenderContext} (HUD module boxes), and vanilla item tooltips
 * render as plain {@code Component}/{@code Style} text with no equivalent hook, so there's nothing
 * for a Glow toggle to drive yet.
 *
 * <p>The Tooltip Message row is a real inline {@link TextFieldOption} (requires MariesLib's
 * {@code charTyped}/{@code keyPressed} forwarding through {@code ItemEditorPanel}/{@code
 * ItemEditorWindow}/{@code ItemEditorScreen}/{@code ItemEditorOverlay} — see those classes'
 * changelog entries); the color swatch opens a real floating {@link PickerWindow} beside the item
 * editor's own box via {@link ItemEditorPage#renderOverlay}/{@code overlayMouseClicked} and friends
 * (also new — see {@code ItemEditorPage}'s changelog entry), the same pattern {@code
 * ScaleConfigPanel} uses for a HUD module's own Colors tab.
 *
 * <p>Registered with {@code ItemEditorScreenProviderRegistry.register(Nourished.MODID, new
 * ExcludeTooltipScreenProvider())} from {@code ClientEventRegistrar}, alongside {@link
 * OptionsScreenProvider}.
 */
@ApiStatus.Internal
public final class ExcludeTooltipScreenProvider implements ItemEditorScreenProvider {

    @Override
    public String menuLabel() {
        return "Exclude Tooltip";
    }

    @Override
    public ItemEditorPage buildScreen(String modId, @Nullable String sourceId, ItemStack stack) {
        return new ExcludeTooltipScreen(sourceId);
    }

    private static final class ExcludeTooltipScreen implements ItemEditorPage {

        private static final int DEFAULT_COLOR = ChatFormatting.DARK_GRAY.getColor() & 0xFFFFFF;
        private static final String PICKER_OWNER_ID = "nourished-exclude-tooltip-color";

        @Nullable
        private final String sourceId;

        private String message = "";
        private boolean colorOverridden;
        private int color = DEFAULT_COLOR;
        private boolean pulseEnabled;
        private ExcludedTooltipPulseRegistry.Speed pulseSpeed = ExcludedTooltipPulseRegistry.Speed.NORMAL;

        private final OptionLayout layout;
        private final PickerWindow picker = new PickerWindow();
        private Bounds lastBounds = new Bounds(0, 0, 0, 0);
        private Bounds lastScreenBounds = new Bounds(0, 0, 0, 0);

        ExcludeTooltipScreen(@Nullable String sourceId) {
            this.sourceId = sourceId;
            loadFromDisk();
            this.layout = new OptionLayout("nourished-exclude-tooltip");
            layout.addTab("");
            layout.addRow(new TextFieldOption("Tooltip Message", () -> message, v -> message = v, () -> {})
                    .hint("Default: \"Excluded from nutrition tracking\""));
            layout.addRow(new ToggleOption("Custom Color", () -> colorOverridden, v -> colorOverridden = v, () -> {}));
            layout.addColorSlot(new ColorSlot("Tooltip Color", () -> color, rgb -> color = rgb, DEFAULT_COLOR, () -> {}));
            layout.enabledWhenLast(() -> colorOverridden);
            layout.setColorSlotListener(slot ->
                    picker.show(slot, PICKER_OWNER_ID, slot.label(), lastBounds, lastScreenBounds));
            layout.addRow(new ToggleOption("Pulse", () -> pulseEnabled, v -> pulseEnabled = v, () -> {}));
            layout.addRow(new CycleOption("Pulse Speed",
                    new String[]{"Slow", "Normal", "Fast"},
                    () -> pulseSpeed.ordinal(), i -> pulseSpeed = ExcludedTooltipPulseRegistry.Speed.values()[i], () -> {}));
            layout.enabledWhenLast(() -> pulseEnabled);
            layout.addRow(new ToggleOption("Glow (coming soon)", () -> false, v -> {}, () -> {}));
            layout.enabledWhenLast(() -> false);
        }

        private void loadFromDisk() {
            message = sourceId != null ? ExcludedTooltipOverrides.getMessageOverride(sourceId).orElse("") : "";
            colorOverridden = sourceId != null && ExcludedTooltipOverrides.getColorOverride(sourceId).isPresent();
            color = sourceId != null ? ExcludedTooltipOverrides.getColorOverride(sourceId).orElse(DEFAULT_COLOR) : DEFAULT_COLOR;
            pulseEnabled = sourceId != null && ExcludedTooltipPulseRegistry.isEnabled(sourceId);
            pulseSpeed = sourceId != null ? ExcludedTooltipPulseRegistry.getSpeed(sourceId) : ExcludedTooltipPulseRegistry.Speed.NORMAL;
        }

        @Override
        public void save() {
            if (sourceId == null) {
                return;
            }
            ExcludedTooltipOverrides.setMessageOverride(sourceId, message);
            ExcludedTooltipOverrides.setColorOverride(sourceId, colorOverridden ? color : null);
            ExcludedTooltipPulseRegistry.set(sourceId, pulseEnabled, pulseSpeed);
            ExcludedTooltipPulseRegistry.save();
        }

        @Override
        public void revert() {
            loadFromDisk();
        }

        @Override
        public String id() {
            return "nourished-exclude-tooltip-screen";
        }

        @Override
        public Constraint constraint() {
            return layout.constraint();
        }

        @Override
        public void render(RenderContext context, Bounds bounds) {
            lastBounds = bounds;
            lastScreenBounds = new Bounds(0, 0, context.screenWidth(), context.screenHeight());
            layout.render(context, bounds);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return layout.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return layout.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return layout.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            return layout.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            return layout.charTyped(codePoint, modifiers);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return layout.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void renderOverlay(RenderContext context, Bounds screenBounds) {
            picker.beginFrame(PICKER_OWNER_ID);
            picker.render(context, screenBounds);
        }

        @Override
        public boolean overlayMouseClicked(double mouseX, double mouseY, int button) {
            return picker.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean overlayMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return picker.mouseDragged(mouseX, mouseY, button);
        }

        @Override
        public boolean overlayMouseReleased(double mouseX, double mouseY, int button) {
            return picker.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean overlayMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            return picker.mouseScrolled(mouseX, mouseY);
        }
    }
}
