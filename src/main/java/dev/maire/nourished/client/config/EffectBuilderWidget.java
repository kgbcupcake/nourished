package dev.maire.nourished.client.config;

import dev.maire.nourished.Nourished;
import dev.maire.nourished.effect.EffectRegistry;
import dev.maire.nourished.nutrition.NutrientRegistry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;

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

    private static final int BTN_H = 20;
    private static final int EDIT_H = 18;
    private static final int PAD = 4;
    private static final int COLLAPSED_H = 24;

    private static List<String> mobEffectIds;

    private final List<EffectCard> cards = new ArrayList<>();
    private final Button newEffectButton;

    private int suggestionCardIndex = -1;
    private final List<String> suggestionBuffer = new ArrayList<>();

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
                true), cards.size()));
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

    void openDeleteConfirm(int index, String ruleId) {
        Minecraft mc = Minecraft.getInstance();
        var returnTo = getConfigScreen();
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

    final class EffectCard {
        private final EffectBuilderWidget host;
        int listIndex;

        boolean expanded;
        private final Button expandToggle;
        private final EditBox ruleIdEdit;
        private final EditBox effectEdit;
        private final Button nutrientButton;
        private final Button triggerButton;
        private final ThresholdSlider thresholdSlider;
        private final EditBox amplifierEdit;
        private final EditBox durationEdit;
        private final Button enabledButton;
        private final Button previewButton;
        private final Button saveButton;
        private final Button deleteButton;

        private String nutrientValue;
        private String triggerValue;
        private boolean enabledValue;

        private int lastEffectEditX;
        private int lastEffectEditY;
        private int lastEffectEditW;

        EffectCard(EffectBuilderWidget host, EffectRegistry.EffectDef def, int listIndex) {
            this.host = host;
            this.listIndex = listIndex;
            this.expanded = false;
            this.nutrientValue = def.nutrient();
            this.triggerValue = def.trigger();
            this.enabledValue = def.enabled();
            if (!nutrientOptions().contains(this.nutrientValue)) {
                this.nutrientValue = "all";
            }
            if (!this.triggerValue.equals("below") && !this.triggerValue.equals("all_above")) {
                this.triggerValue = "below";
            }

            Minecraft mc = Minecraft.getInstance();
            this.expandToggle = Button.builder(Component.empty(), b -> {
                expanded = !expanded;
                if (!expanded) {
                    host.hideSuggestions();
                }
                requestReferenceRebuilding();
            }).bounds(0, 0, 20, BTN_H).build();

            this.ruleIdEdit = new EditBox(mc.font, 0, 0, 120, EDIT_H, Component.translatable("config.nourished.effects.ruleId"));
            ruleIdEdit.setMaxLength(64);
            ruleIdEdit.setValue(def.id());
            ruleIdEdit.setHint(Component.translatable("config.nourished.effects.ruleId.hint"));

            this.effectEdit = new EditBox(mc.font, 0, 0, 160, EDIT_H, Component.translatable("config.nourished.effects.mobEffect"));
            effectEdit.setMaxLength(128);
            effectEdit.setValue(editorNormalizedEffectId(def.effect()));
            effectEdit.setResponder(s -> host.refreshSuggestions(this));
            effectEdit.setHint(Component.translatable("config.nourished.effects.mobEffect.hint"));

            this.nutrientButton = Button.builder(Component.empty(), b -> cycleNutrient()).bounds(0, 0, 100, BTN_H).build();
            this.triggerButton = Button.builder(Component.empty(), b -> cycleTrigger()).bounds(0, 0, 100, BTN_H).build();
            updateNutrientLabel();
            updateTriggerLabel();

            this.thresholdSlider = new ThresholdSlider(0, 0, 100, BTN_H, def.threshold());

            this.amplifierEdit = new EditBox(mc.font, 0, 0, 48, EDIT_H, Component.translatable("config.nourished.effects.amplifier"));
            amplifierEdit.setFilter(EffectCard::unsignedIntFilter);
            amplifierEdit.setValue(String.valueOf(def.amplifier()));

            this.durationEdit = new EditBox(mc.font, 0, 0, 72, EDIT_H, Component.translatable("config.nourished.effects.duration"));
            durationEdit.setFilter(EffectCard::unsignedIntFilter);
            durationEdit.setValue(String.valueOf(def.durationTicks()));

            this.enabledButton = Button.builder(Component.empty(), b -> {
                enabledValue = !enabledValue;
                updateEnabledLabel();
            }).bounds(0, 0, 90, BTN_H).build();
            updateEnabledLabel();

            this.previewButton = Button.builder(Component.translatable("config.nourished.effects.preview"), b -> preview())
                    .bounds(0, 0, 72, BTN_H).build();
            this.saveButton = Button.builder(Component.translatable("config.nourished.effects.save"), b -> saveCard())
                    .bounds(0, 0, 72, BTN_H).build();
            this.deleteButton = Button.builder(Component.translatable("config.nourished.effects.delete"), b -> deleteSelf())
                    .bounds(0, 0, 72, BTN_H).build();
        }

        private static boolean unsignedIntFilter(String s) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c < '0' || c > '9') {
                    return false;
                }
            }
            return true;
        }

        private void cycleNutrient() {
            List<String> opts = nutrientOptions();
            int i = Math.max(0, opts.indexOf(nutrientValue));
            nutrientValue = opts.get((i + 1) % opts.size());
            updateNutrientLabel();
        }

        private void cycleTrigger() {
            List<String> opts = List.of("below", "all_above");
            int i = Math.max(0, opts.indexOf(triggerValue));
            triggerValue = opts.get((i + 1) % opts.size());
            updateTriggerLabel();
        }

        private List<String> nutrientOptions() {
            List<String> opts = new ArrayList<>();
            opts.add("all");
            opts.addAll(NutrientRegistry.getKeys());
            return opts;
        }

        private void updateNutrientLabel() {
            nutrientButton.setMessage(Component.translatable("config.nourished.effects.nutrient", nutrientValue));
        }

        private void updateTriggerLabel() {
            triggerButton.setMessage(Component.translatable("config.nourished.effects.trigger", triggerValue));
        }

        private void updateEnabledLabel() {
            enabledButton.setMessage(Component.translatable(enabledValue ? "config.nourished.effects.enabled.on" : "config.nourished.effects.enabled.off"));
        }

        int rowHeight() {
            int h = expanded ? expandedHeight() : COLLAPSED_H;
            if (expanded && suggestionCardIndex == listIndex && !suggestionBuffer.isEmpty()) {
                h += suggestionBuffer.size() * 11 + 8;
            }
            return h;
        }

        private int expandedHeight() {
            // Keep this in sync with render() vertical layout so rows never overlap.
            int h = 0;
            h += EDIT_H;      // rule id
            h += 16;          // gap to effect row
            h += EDIT_H;      // effect id
            h += 28;          // gap to nutrient/trigger row (+ validation line)
            h += BTN_H;       // nutrient + trigger buttons
            h += 12;          // gap to threshold row
            h += BTN_H;       // threshold slider
            h += 18;          // gap to amplifier/duration row
            h += EDIT_H;      // amplifier + duration edits
            h += 10;          // gap to enabled row
            h += BTN_H;       // enabled button
            h += 6;           // gap to action row
            h += BTN_H;       // preview/save/delete row
            h += 4;           // bottom breathing room
            return h;
        }

        Optional<int[]> suggestionRect() {
            if (suggestionCardIndex != listIndex || !expanded) {
                return Optional.empty();
            }
            return Optional.of(new int[]{lastEffectEditX, lastEffectEditY + EDIT_H + 2, lastEffectEditW});
        }

        void render(GuiGraphics g, int x, int y, int w, int mouseX, int mouseY, float delta, boolean editable) {
            Minecraft mc = Minecraft.getInstance();
            if (!expanded) {
                expandToggle.setX(x);
                expandToggle.setY(y);
                String summary = "▶ " + summaryLine();
                String clipped = mc.font.plainSubstrByWidth(summary, Math.max(30, w - 14));
                expandToggle.setMessage(Component.literal(clipped));
                expandToggle.setWidth(w);
                expandToggle.active = editable;
                expandToggle.render(g, mouseX, mouseY, delta);
                return;
            }

            expandToggle.setX(x);
            expandToggle.setY(y);
            expandToggle.setMessage(Component.literal("▼"));
            expandToggle.setWidth(20);
            expandToggle.active = editable;
            expandToggle.render(g, mouseX, mouseY, delta);

            ruleIdEdit.setX(x + 24);
            ruleIdEdit.setY(y);
            ruleIdEdit.setWidth(w - 24);
            ruleIdEdit.setHeight(EDIT_H);
            ruleIdEdit.setEditable(editable);
            ruleIdEdit.render(g, mouseX, mouseY, delta);

            int lineY = y + EDIT_H + 16;
            g.drawString(mc.font, Component.translatable("config.nourished.effects.mobEffect").getString(), x, lineY - 10, 0xA0A0A0, false);
            effectEdit.setX(x);
            effectEdit.setY(lineY);
            effectEdit.setWidth(w);
            effectEdit.setHeight(EDIT_H);
            effectEdit.setEditable(editable);
            boolean effectIdValid = isEffectIdValid();
            effectEdit.setTextColor(effectIdValid ? 0xE0E0E0 : 0xFF8080);
            lastEffectEditX = x;
            lastEffectEditY = lineY;
            lastEffectEditW = w;
            effectEdit.render(g, mouseX, mouseY, delta);

            if (!effectIdValid) {
                g.drawString(mc.font, Component.translatable("config.nourished.effects.mobEffect.invalid"), x, lineY + EDIT_H + 2, 0xFF8080, false);
            } else {
                g.drawString(mc.font, Component.translatable("config.nourished.effects.mobEffect.valid"), x, lineY + EDIT_H + 2, 0x80FF80, false);
            }

            lineY += EDIT_H + 28;
            int half = (w - PAD) / 2;
            nutrientButton.setX(x);
            nutrientButton.setY(lineY);
            nutrientButton.setWidth(half);
            nutrientButton.active = editable;
            nutrientButton.render(g, mouseX, mouseY, delta);

            triggerButton.setX(x + half + PAD);
            triggerButton.setY(lineY);
            triggerButton.setWidth(half);
            triggerButton.active = editable;
            triggerButton.render(g, mouseX, mouseY, delta);

            lineY += BTN_H + 12;
            g.drawString(mc.font, Component.translatable("config.nourished.effects.threshold").getString(), x, lineY - 10, 0xA0A0A0, false);
            thresholdSlider.setX(x);
            thresholdSlider.setY(lineY);
            thresholdSlider.setWidth(w);
            thresholdSlider.active = editable;
            thresholdSlider.render(g, mouseX, mouseY, delta);

            lineY += BTN_H + 16;
            amplifierEdit.setX(x);
            amplifierEdit.setY(lineY);
            amplifierEdit.setWidth(52);
            amplifierEdit.render(g, mouseX, mouseY, delta);

            durationEdit.setX(x + 60);
            durationEdit.setY(lineY);
            durationEdit.setWidth(80);
            durationEdit.render(g, mouseX, mouseY, delta);

            int dur = parseDurationTicks();
            float sec = dur / 20.0f;
            String secLabel = String.format(Locale.ROOT, " (%.2f s)", sec);
            g.drawString(mc.font, secLabel, x + 150, lineY + 5, 0xC8C8C8, false);

            lineY += EDIT_H + 10;
            enabledButton.setX(x);
            enabledButton.setY(lineY);
            enabledButton.setWidth(w);
            enabledButton.active = editable;
            enabledButton.render(g, mouseX, mouseY, delta);

            lineY += BTN_H + 6;
            int actionGap = 6;
            int actionW = Math.max(60, (w - actionGap * 2) / 3);
            previewButton.setX(x);
            previewButton.setY(lineY);
            previewButton.setWidth(actionW);
            Level level = mc.level;
            previewButton.active = editable && mc.player != null && level != null;
            previewButton.render(g, mouseX, mouseY, delta);

            saveButton.setX(x + actionW + actionGap);
            saveButton.setY(lineY);
            saveButton.setWidth(actionW);
            saveButton.active = editable;
            saveButton.render(g, mouseX, mouseY, delta);

            deleteButton.setX(x + (actionW + actionGap) * 2);
            deleteButton.setY(lineY);
            deleteButton.setWidth(actionW);
            deleteButton.active = editable;
            deleteButton.render(g, mouseX, mouseY, delta);

            g.fill(x, y - 2, x + w, y - 1, 0xFF3A3A50);
        }

        private String summaryLine() {
            String effectShort = effectEdit.getValue().trim();
            int colon = effectShort.indexOf(':');
            if (colon >= 0 && colon + 1 < effectShort.length()) {
                effectShort = effectShort.substring(colon + 1);
            }
            return ruleIdEdit.getValue() + " - " + effectShort + " (" + nutrientValue + ", " + triggerValue + ")";
        }

        private static String editorNormalizedEffectId(String raw) {
            String id = raw == null ? "" : raw.trim();
            while (id.startsWith("/")) {
                id = id.substring(1);
            }
            if (id.isEmpty()) {
                return "minecraft:slowness";
            }
            if (id.indexOf(':') < 0) {
                return "minecraft:" + id;
            }
            return id;
        }

        private static boolean isValidEffectId(String raw) {
            String id = raw == null ? "" : raw.trim();
            if (id.isEmpty() || id.startsWith("/") || id.indexOf(':') < 0) {
                return false;
            }
            try {
                ResourceLocation.parse(id);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }

        private boolean isEffectIdValid() {
            return isValidEffectId(effectEdit.getValue());
        }

        List<net.minecraft.client.gui.components.events.GuiEventListener> children() {
            if (!expanded) {
                return List.of(expandToggle);
            }
            return List.of(
                    expandToggle,
                    ruleIdEdit,
                    effectEdit,
                    nutrientButton,
                    triggerButton,
                    thresholdSlider,
                    amplifierEdit,
                    durationEdit,
                    enabledButton,
                    previewButton,
                    saveButton,
                    deleteButton);
        }

        @SuppressWarnings("unchecked")
        List<net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return (List<net.minecraft.client.gui.narration.NarratableEntry>) (List<?>) children();
        }

        private int parseAmplifier() {
            try {
                return Mth.clamp(Integer.parseInt(amplifierEdit.getValue().isEmpty() ? "0" : amplifierEdit.getValue()), 0, 4);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        private int parseDurationTicks() {
            try {
                int v = Integer.parseInt(durationEdit.getValue().isEmpty() ? "20" : durationEdit.getValue());
                return Mth.clamp(v, 20, 72000);
            } catch (NumberFormatException e) {
                return 140;
            }
        }

        private EffectRegistry.EffectDef buildDef() {
            String effectId = effectEdit.getValue().trim();
            return new EffectRegistry.EffectDef(
                    ruleIdEdit.getValue().trim().isEmpty() ? "unnamed" : ruleIdEdit.getValue().trim(),
                    effectId,
                    nutrientValue,
                    triggerValue,
                    thresholdSlider.thresholdValue(),
                    parseAmplifier(),
                    parseDurationTicks(),
                    enabledValue);
        }

        private void preview() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                return;
            }
            if (!isEffectIdValid()) {
                return;
            }
            ResourceLocation rl;
            try {
                rl = ResourceLocation.parse(effectEdit.getValue().trim());
            } catch (Exception e) {
                return;
            }
            BuiltInRegistries.MOB_EFFECT.getHolder(rl).ifPresent(holder ->
                    mc.player.addEffect(new MobEffectInstance(holder, 5 * 20, parseAmplifier(), false, true, true)));
        }

        private void saveCard() {
            if (!isEffectIdValid()) {
                return;
            }
            EffectRegistry.EffectDef updated = buildDef();
            boolean wasExpanded = expanded;
            host.cards.set(listIndex, new EffectCard(host, updated, listIndex));
            host.cards.get(listIndex).expanded = wasExpanded;
            List<EffectRegistry.EffectDef> all = new ArrayList<>();
            for (EffectCard c : host.cards) {
                all.add(c.buildDef());
            }
            try {
                EffectRegistry.saveAll(all);
            } catch (Exception e) {
                Nourished.LOGGER.error("Failed to save effects.json", e);
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            mc.getToasts().addToast(new EffectSavedToast(updated.id()));
            host.hideSuggestions();
            requestReferenceRebuilding();
        }

        private void deleteSelf() {
            host.openDeleteConfirm(listIndex, ruleIdEdit.getValue());
        }
    }

    private static final class ThresholdSlider extends AbstractSliderButton {

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
