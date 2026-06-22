package dev.maire.nourished.client.config.categories.widgets;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ModuleProfilesListEntry extends TooltipListEntry<Object> {
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 6;

    private final Map<String, AtomicBoolean> modulePending;
    private final List<String> editableModuleKeys;
    private final Button balancedButton;
    private final Button minimalistButton;
    private final Button immersiveButton;
    private final Button gameplayButton;

public ModuleProfilesListEntry(Map<String, AtomicBoolean> modulePending, List<String> editableModuleKeys) {
        super(
                Component.translatable("config.nourished.modules.profiles"),
                () -> Optional.of(new Component[]{Component.translatable("config.nourished.modules.profiles.desc")}),
                false);
        this.modulePending = modulePending;
        this.editableModuleKeys = editableModuleKeys;
        this.balancedButton = Button.builder(Component.translatable("config.nourished.modules.profile.balanced"), b -> applyProfile("balanced"))
                .bounds(0, 0, 110, BUTTON_HEIGHT)
                .build();
        this.minimalistButton = Button.builder(Component.translatable("config.nourished.modules.profile.minimalist"), b -> applyProfile("minimalist"))
                .bounds(0, 0, 110, BUTTON_HEIGHT)
                .build();
        this.immersiveButton = Button.builder(Component.translatable("config.nourished.modules.profile.immersive"), b -> applyProfile("immersive"))
                .bounds(0, 0, 110, BUTTON_HEIGHT)
                .build();
        this.gameplayButton = Button.builder(Component.translatable("config.nourished.modules.profile.gameplay"), b -> applyProfile("gameplay"))
                .bounds(0, 0, 110, BUTTON_HEIGHT)
                .build();
    }

    private void applyProfile(String profile) {
        // Start with all OFF then enable profile-specific modules.
        for (String key : editableModuleKeys) {
            AtomicBoolean pending = modulePending.get(key);
            if (pending != null) pending.set(false);
        }
        switch (profile) {
            case "minimalist" -> setModules(true, "enableDecay", "enableSourceApplication", "enableEffects", "enableCalorieTracking");
            case "immersive" -> setModules(true, editableModuleKeys.toArray(new String[0]));
            case "gameplay" -> setModules(true,
                    "enableDecay",
                    "enableSourceApplication",
                    "enableEffects",
                    "enableCalorieTracking",
                    "enableSleepBonus");
            default -> setModules(true,
                    "enableDecay",
                    "enableSourceApplication",
                    "enableEffects",
                    "enableHUD",
                    "enableToasts",
                    "enableFoodTooltips",
                    "enableCalorieTracking",
                    "enableDietScreen",
                    "enableCriticalToasts",
                    "enableSleepBonus",
                    "enableSynergies",
                    "enableMilestones",
                    "enableSeasonHooks",
                    "enableAbsorptionModifiers");
        }
        // Dependency guards.
        AtomicBoolean toasts = modulePending.get("enableToasts");
        AtomicBoolean crit = modulePending.get("enableCriticalToasts");
        if (toasts != null && crit != null && !toasts.get()) {
            crit.set(false);
        }
    }

    private void setModules(boolean value, String... keys) {
        for (String key : keys) {
            AtomicBoolean pending = modulePending.get(key);
            if (pending != null) pending.set(value);
        }
    }

    @Override
    public boolean isEdited() {
        return true;
    }

    @Override
    public void save() {}

    @Override
    public Object getValue() {
        return Boolean.FALSE;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public int getItemHeight() {
        return 50;
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
        int btnWidth = (entryWidth - GAP) / 2;
        balancedButton.active = isEditable();
        minimalistButton.active = isEditable();
        immersiveButton.active = isEditable();
        gameplayButton.active = isEditable();
        balancedButton.setX(x);
        balancedButton.setY(y);
        balancedButton.setWidth(btnWidth);
        minimalistButton.setX(x + btnWidth + GAP);
        minimalistButton.setY(y);
        minimalistButton.setWidth(btnWidth);
        immersiveButton.setX(x);
        immersiveButton.setY(y + BUTTON_HEIGHT + GAP);
        immersiveButton.setWidth(btnWidth);
        gameplayButton.setX(x + btnWidth + GAP);
        gameplayButton.setY(y + BUTTON_HEIGHT + GAP);
        gameplayButton.setWidth(btnWidth);
        balancedButton.render(graphics, mouseX, mouseY, delta);
        minimalistButton.render(graphics, mouseX, mouseY, delta);
        immersiveButton.render(graphics, mouseX, mouseY, delta);
        gameplayButton.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        return List.of(balancedButton, minimalistButton, immersiveButton, gameplayButton);
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return List.of(balancedButton, minimalistButton, immersiveButton, gameplayButton);
    }
}


