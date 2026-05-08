package dev.maire.nourished.client.config;

import dev.maire.nourished.config.NourishedConfigScreen;
import dev.maire.nourished.config.PresetRegistry;
import dev.maire.nourished.config.PresetRegistry.ParsedPreset;
import dev.maire.nourished.nutrition.Nourished;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cloth Config entry: preset cards with load/delete, save-current dialog, and built-in/lock rules.
 */
public final class PresetsWidget extends TooltipListEntry<Object> {

    private static final int BTN_H = 20;
    private static final int PAD = 4;
    private static final int CARD_H = 56;
    private static final int LABEL_TOP = 14;

    private final Screen reopenParent;
    private final List<ParsedPreset> presets;
    private final List<PresetCard> cards = new ArrayList<>();
    private final Button saveCurrentButton;

    public PresetsWidget(Screen reopenParent) {
        super(
                Component.translatable("config.nourished.presets.listLabel"),
                () -> Optional.of(new Component[]{Component.translatable("config.nourished.presets.listLabel.desc")}),
                false);
        this.reopenParent = reopenParent;
        this.presets = new ArrayList<>(PresetRegistry.listPresets());
        for (ParsedPreset p : presets) {
            cards.add(new PresetCard(this, p));
        }
        this.saveCurrentButton = Button.builder(Component.translatable("config.nourished.presets.saveCurrent"), b -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.setScreen(new SavePresetScreen(mc.screen, reopenParent));
                })
                .bounds(0, 0, 220, BTN_H)
                .build();
    }

    void openDeleteConfirm(ParsedPreset preset) {
        Minecraft mc = Minecraft.getInstance();
        Screen returnTo = getConfigScreen();
        BooleanConsumer callback = yes -> {
            if (yes) {
                try {
                    PresetRegistry.deletePreset(preset);
                } catch (Exception e) {
                    Nourished.LOGGER.warn("[PresetsWidget] Failed to delete preset {}", preset.path(), e);
                }
                mc.setScreen(NourishedConfigScreen.create(reopenParent));
            } else {
                mc.setScreen(returnTo);
            }
        };
        mc.setScreen(new ConfirmScreen(
                callback,
                Component.translatable("config.nourished.presets.delete.title"),
                Component.translatable("config.nourished.presets.delete.message", preset.name())));
    }

    void loadPreset(ParsedPreset preset) {
        PresetRegistry.applyPreset(preset);
        Minecraft mc = Minecraft.getInstance();
        mc.getToasts().addToast(new PresetLoadedToast(preset.name()));
        mc.setScreen(NourishedConfigScreen.create(reopenParent));
    }

    @Override
    public Object getValue() {
        return Boolean.FALSE;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public boolean isEdited() {
        return false;
    }

    @Override
    public void save() {
    }

    @Override
    public int getItemHeight() {
        int h = LABEL_TOP + BTN_H + PAD + (CARD_H + PAD) * cards.size();
        return Math.max(h, 48);
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        List<net.minecraft.client.gui.components.events.GuiEventListener> out = new ArrayList<>();
        out.add(saveCurrentButton);
        for (PresetCard c : cards) {
            out.addAll(c.children());
        }
        return out;
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        List<net.minecraft.client.gui.narration.NarratableEntry> out = new ArrayList<>();
        out.add(saveCurrentButton);
        for (PresetCard c : cards) {
            out.addAll(c.narratables());
        }
        return out;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int index,
            int y,
            int x,
            int entryWidth,
            int entryHeight,
            int mouseX,
            int mouseY,
            boolean isHovered,
            float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        int innerW = entryWidth - 8;
        int sx = x + 4;
        int cy = y + LABEL_TOP;

        saveCurrentButton.setX(sx);
        saveCurrentButton.setY(cy);
        saveCurrentButton.setWidth(Math.min(240, innerW));
        saveCurrentButton.active = isEditable();
        saveCurrentButton.render(graphics, mouseX, mouseY, delta);
        cy += BTN_H + PAD;

        for (PresetCard c : cards) {
            c.render(graphics, sx, cy, innerW, mouseX, mouseY, delta, isEditable());
            cy += CARD_H + PAD;
        }
    }

    private static final class PresetCard {
        private final ParsedPreset preset;
        private final Button loadButton;
        private final Button deleteButton;

        private PresetCard(PresetsWidget parent, ParsedPreset preset) {
            this.preset = preset;
            this.loadButton = Button.builder(Component.translatable("config.nourished.presets.load"), b -> parent.loadPreset(preset))
                    .bounds(0, 0, 56, BTN_H)
                    .build();
            this.deleteButton = Button.builder(Component.translatable("config.nourished.presets.delete"), b -> parent.openDeleteConfirm(preset))
                    .bounds(0, 0, 56, BTN_H)
                    .build();
        }

        List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            return List.of(loadButton, deleteButton);
        }

        List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return List.of(loadButton, deleteButton);
        }

        void render(GuiGraphics graphics, int sx, int cy, int innerW, int mouseX, int mouseY, float delta, boolean editable) {
            Minecraft mc = Minecraft.getInstance();
            int lockSlot = preset.showLockIcon() ? 18 : 0;
            int textLeft = sx + lockSlot;
            int textMaxW = innerW - lockSlot - 128;
            int btnTop = cy + 4;
            loadButton.setX(sx + innerW - 120);
            loadButton.setY(btnTop);
            loadButton.active = editable;
            deleteButton.setX(sx + innerW - 60);
            deleteButton.setY(btnTop);
            deleteButton.active = editable && preset.canDelete();

            graphics.fill(sx, cy, sx + innerW, cy + CARD_H - PAD, 0x28000000);
            graphics.renderOutline(sx, cy, innerW, CARD_H - PAD, 0xFF404040);

            if (preset.showLockIcon()) {
                graphics.pose().pushPose();
                graphics.pose().translate(sx + 1, cy + 4, 100f);
                graphics.pose().scale(0.75f, 0.75f, 1f);
                graphics.renderItem(new ItemStack(Items.IRON_DOOR), 0, 0);
                graphics.pose().popPose();
            }

            String title = mc.font.plainSubstrByWidth(preset.name(), Math.max(20, textMaxW));
            graphics.drawString(mc.font, title, textLeft, cy + 6, 0xFFFFFF, false);

            String desc = preset.description() == null ? "" : preset.description();
            if (!desc.isEmpty()) {
                String descLine = mc.font.plainSubstrByWidth(desc, Math.max(20, innerW - lockSlot - 8));
                graphics.drawString(mc.font, descLine, textLeft, cy + 20, 0xA0A0A0, false);
            }

            String authorKey = preset.author() == null ? "" : preset.author();
            if (!authorKey.isEmpty()) {
                Component by = Component.translatable("config.nourished.presets.byAuthor", authorKey);
                String authorLine = mc.font.plainSubstrByWidth(by.getString(), Math.max(20, innerW - lockSlot - 8));
                graphics.drawString(mc.font, authorLine, textLeft, cy + 36, 0x808080, false);
            }

            loadButton.render(graphics, mouseX, mouseY, delta);
            deleteButton.render(graphics, mouseX, mouseY, delta);
        }
    }

    private static final class PresetLoadedToast implements Toast {

        private static final long DISPLAY_MS = 3200L;
        private final Component line;

        PresetLoadedToast(String presetName) {
            this.line = Component.translatable("config.nourished.presets.loaded", presetName);
        }

        @Override
        public int width() {
            return Math.min(320, Math.max(160, Minecraft.getInstance().font.width(line) + 16));
        }

        @Override
        public int height() {
            return 32;
        }

        @Override
        public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long timeSinceLastVisible) {
            var font = toastComponent.getMinecraft().font;
            guiGraphics.fill(0, 0, width(), height(), 0xF0100010);
            guiGraphics.renderOutline(0, 0, width(), height(), 0xFF505078);
            guiGraphics.drawString(font, line, 8, 12, 0xFFFFFF, false);
            return timeSinceLastVisible >= DISPLAY_MS ? Visibility.HIDE : Visibility.SHOW;
        }
    }
}
