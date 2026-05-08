package dev.maire.nourished.client.config;

import dev.maire.nourished.effect.EffectRegistry;
import dev.maire.nourished.nutrition.Nourished;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

class EffectCard {
    private final EffectBuilderWidget host;
    int listIndex;

    boolean expanded;
    private final Button expandToggle;
    final EditBox ruleIdEdit;
    final EditBox effectEdit;
    private final Button nutrientButton;
    private final Button triggerButton;
    private final EffectBuilderWidget.ThresholdSlider thresholdSlider;
    private final EffectBuilderWidget.ThresholdSlider thresholdMaxSlider;
    private final EditBox amplifierEdit;
    private final EditBox durationEdit;
    private final Button enabledButton;
    private final Button ambientButton;
    private final Button showParticlesButton;
    private final Button previewButton;
    private final Button saveButton;
    private final Button deleteButton;

    private String nutrientValue;
    private String triggerValue;
    private boolean enabledValue;
    private boolean ambientValue;
    private boolean showParticlesValue;

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
        this.ambientValue = def.ambient();
        this.showParticlesValue = def.showParticles();
        if (!nutrientOptions().contains(this.nutrientValue)) {
            this.nutrientValue = "all";
        }
        Minecraft mc = Minecraft.getInstance();
        this.expandToggle = Button.builder(Component.empty(), b -> {
            expanded = !expanded;
            if (!expanded) {
                host.hideSuggestions();
            }
            host.requestRebuildFromCard();
        }).bounds(0, 0, 20, EffectBuilderWidget.BTN_H).build();

        this.ruleIdEdit = new EditBox(mc.font, 0, 0, 120, EffectBuilderWidget.EDIT_H, Component.translatable("config.nourished.effects.ruleId"));
        ruleIdEdit.setMaxLength(64);
        ruleIdEdit.setValue(def.id());
        ruleIdEdit.setHint(Component.translatable("config.nourished.effects.ruleId.hint"));

        this.effectEdit = new EditBox(mc.font, 0, 0, 160, EffectBuilderWidget.EDIT_H, Component.translatable("config.nourished.effects.mobEffect"));
        effectEdit.setMaxLength(128);
        effectEdit.setValue(editorNormalizedEffectId(def.effect()));
        effectEdit.setResponder(s -> host.refreshSuggestions(this));
        effectEdit.setHint(Component.translatable("config.nourished.effects.mobEffect.hint"));

        this.nutrientButton = Button.builder(Component.empty(), b -> cycleNutrient()).bounds(0, 0, 100, EffectBuilderWidget.BTN_H).build();
        this.triggerButton = Button.builder(Component.empty(), b -> cycleTrigger()).bounds(0, 0, 100, EffectBuilderWidget.BTN_H).build();
        updateNutrientLabel();
        updateTriggerLabel();

        this.thresholdSlider = new EffectBuilderWidget.ThresholdSlider(0, 0, 100, EffectBuilderWidget.BTN_H, def.threshold());
        this.thresholdMaxSlider = new EffectBuilderWidget.ThresholdSlider(0, 0, 100, EffectBuilderWidget.BTN_H, def.thresholdMax());

        this.amplifierEdit = new EditBox(mc.font, 0, 0, 48, EffectBuilderWidget.EDIT_H, Component.translatable("config.nourished.effects.amplifier"));
        amplifierEdit.setFilter(EffectCard::unsignedIntFilter);
        amplifierEdit.setValue(String.valueOf(def.amplifier()));

        this.durationEdit = new EditBox(mc.font, 0, 0, 72, EffectBuilderWidget.EDIT_H, Component.translatable("config.nourished.effects.duration"));
        durationEdit.setFilter(EffectCard::unsignedIntFilter);
        durationEdit.setValue(String.valueOf(def.durationTicks()));

        this.enabledButton = Button.builder(Component.empty(), b -> {
            enabledValue = !enabledValue;
            updateEnabledLabel();
        }).bounds(0, 0, 90, EffectBuilderWidget.BTN_H).build();
        updateEnabledLabel();
        this.ambientButton = Button.builder(Component.empty(), b -> {
            ambientValue = !ambientValue;
            updateAmbientLabel();
        }).bounds(0, 0, 90, EffectBuilderWidget.BTN_H).build();
        updateAmbientLabel();
        this.showParticlesButton = Button.builder(Component.empty(), b -> {
            showParticlesValue = !showParticlesValue;
            updateShowParticlesLabel();
        }).bounds(0, 0, 90, EffectBuilderWidget.BTN_H).build();
        updateShowParticlesLabel();

        this.previewButton = Button.builder(Component.translatable("config.nourished.effects.preview"), b -> preview())
                .bounds(0, 0, 72, EffectBuilderWidget.BTN_H).build();
        this.saveButton = Button.builder(Component.translatable("config.nourished.effects.save"), b -> saveCard())
                .bounds(0, 0, 72, EffectBuilderWidget.BTN_H).build();
        this.deleteButton = Button.builder(Component.translatable("config.nourished.effects.delete"), b -> deleteSelf())
                .bounds(0, 0, 72, EffectBuilderWidget.BTN_H).build();
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
        List<String> opts = List.of("below", "above", "all_above", "any_below", "between");
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

    private void updateAmbientLabel() {
        ambientButton.setMessage(Component.literal("Ambient: " + (ambientValue ? "On" : "Off")));
    }

    private void updateShowParticlesLabel() {
        showParticlesButton.setMessage(Component.literal("Show particles: " + (showParticlesValue ? "On" : "Off")));
    }

    int rowHeight() {
        int h = expanded ? expandedHeight() : EffectBuilderWidget.COLLAPSED_H;
        if (expanded && host.suggestionCardIndex == listIndex && !host.suggestionBuffer.isEmpty()) {
            h += host.suggestionBuffer.size() * 11 + 8;
        }
        return h;
    }

    private int expandedHeight() {
        int h = 0;
        h += EffectBuilderWidget.EDIT_H;
        h += 16;
        h += EffectBuilderWidget.EDIT_H;
        h += 28;
        h += EffectBuilderWidget.BTN_H;
        h += 12;
        if ("between".equals(triggerValue)) {
            h += EffectBuilderWidget.BTN_H + 12;
        }
        h += EffectBuilderWidget.BTN_H;
        h += 18;
        h += EffectBuilderWidget.EDIT_H;
        h += 10;
        h += EffectBuilderWidget.BTN_H;
        h += 6;
        h += 10;
        h += EffectBuilderWidget.BTN_H;
        h += 6;
        h += EffectBuilderWidget.BTN_H;
        h += 4;
        return h;
    }

    Optional<int[]> suggestionRect() {
        if (host.suggestionCardIndex != listIndex || !expanded) {
            return Optional.empty();
        }
        return Optional.of(new int[]{lastEffectEditX, lastEffectEditY + EffectBuilderWidget.EDIT_H + 2, lastEffectEditW});
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
        ruleIdEdit.setHeight(EffectBuilderWidget.EDIT_H);
        ruleIdEdit.setEditable(editable);
        ruleIdEdit.render(g, mouseX, mouseY, delta);

        int lineY = y + EffectBuilderWidget.EDIT_H + 16;
        g.drawString(mc.font, Component.translatable("config.nourished.effects.mobEffect").getString(), x, lineY - 10, 0xA0A0A0, false);
        effectEdit.setX(x);
        effectEdit.setY(lineY);
        effectEdit.setWidth(w);
        effectEdit.setHeight(EffectBuilderWidget.EDIT_H);
        effectEdit.setEditable(editable);
        boolean effectIdValid = isEffectIdValid();
        effectEdit.setTextColor(effectIdValid ? 0xE0E0E0 : 0xFF8080);
        lastEffectEditX = x;
        lastEffectEditY = lineY;
        lastEffectEditW = w;
        effectEdit.render(g, mouseX, mouseY, delta);

        if (!effectIdValid) {
            g.drawString(mc.font, Component.translatable("config.nourished.effects.mobEffect.invalid"), x, lineY + EffectBuilderWidget.EDIT_H + 2, 0xFF8080, false);
        } else {
            g.drawString(mc.font, Component.translatable("config.nourished.effects.mobEffect.valid"), x, lineY + EffectBuilderWidget.EDIT_H + 2, 0x80FF80, false);
        }

        lineY += EffectBuilderWidget.EDIT_H + 28;
        int half = (w - EffectBuilderWidget.PAD) / 2;
        nutrientButton.setX(x);
        nutrientButton.setY(lineY);
        nutrientButton.setWidth(half);
        nutrientButton.active = editable;
        nutrientButton.render(g, mouseX, mouseY, delta);

        triggerButton.setX(x + half + EffectBuilderWidget.PAD);
        triggerButton.setY(lineY);
        triggerButton.setWidth(half);
        triggerButton.active = editable;
        triggerButton.render(g, mouseX, mouseY, delta);

        lineY += EffectBuilderWidget.BTN_H + 12;
        g.drawString(mc.font, Component.translatable("config.nourished.effects.threshold").getString(), x, lineY - 10, 0xA0A0A0, false);
        thresholdSlider.setX(x);
        thresholdSlider.setY(lineY);
        thresholdSlider.setWidth(w);
        thresholdSlider.active = editable;
        thresholdSlider.render(g, mouseX, mouseY, delta);

        if ("between".equals(triggerValue)) {
            lineY += EffectBuilderWidget.BTN_H + 12;
            g.drawString(mc.font, "Max threshold:", x, lineY - 10, 0xA0A0A0, false);
            thresholdMaxSlider.setX(x);
            thresholdMaxSlider.setY(lineY);
            thresholdMaxSlider.setWidth(w);
            thresholdMaxSlider.active = editable;
            thresholdMaxSlider.render(g, mouseX, mouseY, delta);
        }

        lineY += EffectBuilderWidget.BTN_H + 16;
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

        lineY += EffectBuilderWidget.EDIT_H + 10;
        enabledButton.setX(x);
        enabledButton.setY(lineY);
        enabledButton.setWidth(w);
        enabledButton.active = editable;
        enabledButton.render(g, mouseX, mouseY, delta);

        lineY += EffectBuilderWidget.BTN_H + 10;
        ambientButton.setX(x);
        ambientButton.setY(lineY);
        ambientButton.setWidth(half);
        ambientButton.active = editable;
        ambientButton.render(g, mouseX, mouseY, delta);

        showParticlesButton.setX(x + half + EffectBuilderWidget.PAD);
        showParticlesButton.setY(lineY);
        showParticlesButton.setWidth(half);
        showParticlesButton.active = editable;
        showParticlesButton.render(g, mouseX, mouseY, delta);

        lineY += EffectBuilderWidget.BTN_H + 6;
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
        return List.of(
                expandToggle,
                thresholdMaxSlider,
                ruleIdEdit,
                effectEdit,
                nutrientButton,
                triggerButton,
                thresholdSlider,
                amplifierEdit,
                durationEdit,
                enabledButton,
                ambientButton,
                showParticlesButton,
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
                enabledValue,
                thresholdMaxSlider.thresholdValue(),
                ambientValue,
                showParticlesValue);
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
        mc.getToasts().addToast(new EffectBuilderWidget.EffectSavedToast(updated.id()));
        host.hideSuggestions();
        host.requestRebuildFromCard();
    }

    private void deleteSelf() {
        host.openDeleteConfirm(listIndex, ruleIdEdit.getValue());
    }
}
