package dev.maire.nourished.client.config;

import dev.maire.nourished.core.effect.EffectRegistry;
import dev.marie.MariesLib.client.ClientScreenFactories;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Cloth Config entry: expandable cards for each {@link EffectRegistry.EffectDef}, with mob-effect
 * autocomplete, per-card save to {@code effects.json}, preview, and delete confirmation.
 */
public final class EffectBuilderWidget extends TooltipListEntry<Object> {

    static final int BTN_H = 20;
    static final int EDIT_H = 18;
    static final int PAD = 4;
    static final int COLLAPSED_H = 24;

    private static List<String> mobEffectIds;

    final List<EffectCard> cards = new ArrayList<>();
    private final Button newEffectButton;

    int suggestionCardIndex = -1;
    final List<String> suggestionBuffer = new ArrayList<>();

    public EffectBuilderWidget() {
        super(
                Component.empty(),
                Optional::<Component[]>empty,
                false);
        for (EffectRegistry.EffectDef def : EffectRegistry.getAll()) {
            cards.add(new EffectCard(this, def, cards.size()));
        }
        this.newEffectButton = Button.builder(Component.translatable("config.nourished.effects.new"), b -> addBlankCard())
                .bounds(0, 0, 160, BTN_H)
                .build();
    }

    private static List<String> mobEffectIdList() {
        if (mobEffectIds == null) {
            List<String> list = new ArrayList<>();
            for (MobEffect me : BuiltInRegistries.MOB_EFFECT) {
                ResourceLocation key = BuiltInRegistries.MOB_EFFECT.getKey(me);
                if (key != null) {
                    list.add(key.toString());
                }
            }
            list.sort(Comparator.naturalOrder());
            mobEffectIds = List.copyOf(list);
        }
        return mobEffectIds;
    }

    private void addBlankCard() {
        String base = "new_effect";
        String id = uniquifyRuleId(base);
        cards.add(new EffectCard(this, new EffectRegistry.EffectDef(
                id,
                "minecraft:slowness",
                "all",
                "below",
                0.25,
                0,
                140,
                true,
                1.0,
                true,
                false), cards.size()));
        requestReferenceRebuilding();
    }

    private String uniquifyRuleId(String base) {
        if (cards.stream().noneMatch(c -> c.ruleIdEdit.getValue().equals(base))) {
            return base;
        }
        int n = 2;
        while (true) {
            final String candidate = base + "_" + n;
            if (cards.stream().noneMatch(c -> c.ruleIdEdit.getValue().equals(candidate))) {
                return candidate;
            }
            n++;
        }
    }

    void requestRebuildFromCard() {
        requestReferenceRebuilding();
    }

    void openDeleteConfirm(int index, String ruleId) {
        Minecraft mc = Minecraft.getInstance();
        var returnTo = ClientScreenFactories.getConfigScreen();
        BooleanConsumer callback = yes -> {
            if (yes && index >= 0 && index < cards.size()) {
                cards.remove(index);
                reindexCards();
                requestReferenceRebuilding();
            }
            mc.setScreen(returnTo);
        };
        mc.setScreen(new ConfirmScreen(
                callback,
                Component.translatable("config.nourished.effects.delete.title"),
                Component.translatable("config.nourished.effects.delete.message", ruleId)));
    }

    private void reindexCards() {
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).listIndex = i;
        }
    }

    void refreshSuggestions(EffectCard card) {
        int idx = cards.indexOf(card);
        if (idx < 0) {
            return;
        }
        suggestionBuffer.clear();
        String prefix = card.effectEdit.getValue().trim().toLowerCase(Locale.ROOT);
        if (!card.effectEdit.isFocused() || prefix.isEmpty()) {
            suggestionCardIndex = -1;
            requestReferenceRebuilding();
            return;
        }
        for (String id : mobEffectIdList()) {
            if (id.toLowerCase(Locale.ROOT).startsWith(prefix) && !suggestionBuffer.contains(id)) {
                suggestionBuffer.add(id);
                if (suggestionBuffer.size() >= 8) {
                    break;
                }
            }
        }
        suggestionCardIndex = suggestionBuffer.isEmpty() ? -1 : idx;
        requestReferenceRebuilding();
    }

    void hideSuggestions() {
        suggestionCardIndex = -1;
        suggestionBuffer.clear();
        requestReferenceRebuilding();
    }

    private void applySuggestion(String id) {
        if (suggestionCardIndex >= 0 && suggestionCardIndex < cards.size()) {
            cards.get(suggestionCardIndex).effectEdit.setValue(id);
            cards.get(suggestionCardIndex).effectEdit.moveCursorToEnd(false);
        }
        hideSuggestions();
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
        // Per-card Save writes effects.json; global Done does not duplicate-save here.
    }

    @Override
    public int getItemHeight() {
        int h = 6;
        for (EffectCard c : cards) {
            h += c.rowHeight() + PAD;
        }
        h += BTN_H + PAD;
        return Math.max(h, 40);
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        List<net.minecraft.client.gui.components.events.GuiEventListener> out = new ArrayList<>();
        for (EffectCard c : cards) {
            out.addAll(c.children());
        }
        out.add(newEffectButton);
        return out;
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        List<net.minecraft.client.gui.narration.NarratableEntry> out = new ArrayList<>();
        for (EffectCard c : cards) {
            out.addAll(c.narratables());
        }
        out.add(newEffectButton);
        return out;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && suggestionCardIndex >= 0 && !suggestionBuffer.isEmpty()) {
            Optional<int[]> rect = suggestionPopupRect();
            if (rect.isPresent()) {
                int[] r = rect.get();
                int sx = r[0];
                int sy = r[1];
                int lineH = 11;
                for (int i = 0; i < suggestionBuffer.size(); i++) {
                    int ly = sy + i * lineH;
                    if (mouseX >= sx && mouseX < sx + r[2] && mouseY >= ly && mouseY < ly + lineH) {
                        applySuggestion(suggestionBuffer.get(i));
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** [x, y, w] of suggestion dropdown under the active card's mob effect field, if any. */
    private Optional<int[]> suggestionPopupRect() {
        if (suggestionCardIndex < 0 || suggestionCardIndex >= cards.size()) {
            return Optional.empty();
        }
        return cards.get(suggestionCardIndex).suggestionRect();
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
        if (!isRowVisible(y, getItemHeight())) {
            hideButtons();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int cy = y + 4;
        int innerW = entryWidth - 8;
        int sx = x + 4;

        for (EffectCard c : cards) {
            c.render(graphics, sx, cy, innerW, mouseX, mouseY, delta, isEditable());
            cy += c.rowHeight() + PAD;
        }

        newEffectButton.setX(sx);
        newEffectButton.setY(cy);
        newEffectButton.setWidth(Math.min(200, innerW));
        newEffectButton.active = isEditable();
        newEffectButton.render(graphics, mouseX, mouseY, delta);

        if (suggestionCardIndex >= 0 && !suggestionBuffer.isEmpty()) {
            Optional<int[]> opt = suggestionPopupRect();
            if (opt.isPresent()) {
                int[] r = opt.get();
                int lineH = 11;
                int boxH = suggestionBuffer.size() * lineH + 4;
                graphics.fill(r[0] - 2, r[1] - 2, r[0] + r[2] + 2, r[1] + boxH, 0xE0101010);
                graphics.renderOutline(r[0] - 2, r[1] - 2, r[2] + 4, boxH, 0xFF505050);
                for (int i = 0; i < suggestionBuffer.size(); i++) {
                    graphics.drawString(mc.font, suggestionBuffer.get(i), r[0], r[1] + i * lineH + 2, 0xFFFFFF, false);
                }
            }
        }
    }

    private void hideButtons() {
        for (EffectCard card : cards) {
            card.hideButtons();
        }
        newEffectButton.setY(-2000);
    }

    private boolean isRowVisible(int y, int h) {
        if (!(Minecraft.getInstance().screen instanceof ClothConfigScreen cloth) || cloth.listWidget == null) {
            return true;
        }
        return y < cloth.listWidget.bottom && y + h > cloth.listWidget.top;
    }

    static final class EffectSavedToast implements Toast {

        private static final long DISPLAY_MS = 3200L;
        private final Component line;

        EffectSavedToast(String ruleId) {
            this.line = Component.literal("Effect '" + ruleId + "' saved to effects.json");
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

    static final class ThresholdSlider extends AbstractSliderButton {

        ThresholdSlider(int x, int y, int w, int h, double initial) {
            super(x, y, w, h, Component.empty(), Mth.clamp(initial, 0d, 1d));
            updateMessage();
        }

        double thresholdValue() {
            return value;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("config.nourished.effects.threshold.value", String.format(Locale.ROOT, "%.3f", value)));
        }

        @Override
        protected void applyValue() {
            this.value = Mth.clamp(this.value, 0d, 1d);
            updateMessage();
        }
    }
}
