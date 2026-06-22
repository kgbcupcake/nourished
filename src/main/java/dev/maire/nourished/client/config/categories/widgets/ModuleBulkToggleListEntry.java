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

public final class ModuleBulkToggleListEntry extends TooltipListEntry<Object> {
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 6;
    private static final long CONFIRM_WINDOW_MS = 5000L;

    private final List<String> editableModuleKeys;
    private final Map<String, AtomicBoolean> modulePending;
    private final Button enableAllButton;
    private final Button disableAllButton;
    private boolean disableConfirmArmed;
    private long disableConfirmArmedAt;

public ModuleBulkToggleListEntry(List<String> editableModuleKeys, Map<String, AtomicBoolean> modulePending) {
        super(
                Component.translatable("config.nourished.modules.bulk"),
                () -> Optional.of(new Component[]{Component.translatable("config.nourished.modules.bulk.desc")}),
                false);
        this.editableModuleKeys = editableModuleKeys;
        this.modulePending = modulePending;
        this.enableAllButton = Button.builder(Component.translatable("config.nourished.modules.enableAll"), b -> setAll(true))
                .bounds(0, 0, 120, BUTTON_HEIGHT)
                .build();
        this.disableAllButton = Button.builder(Component.translatable("config.nourished.modules.disableAll"), b -> onDisableAllClick())
                .bounds(0, 0, 120, BUTTON_HEIGHT)
                .build();
    }

    private void setAll(boolean value) {
        for (String key : editableModuleKeys) {
            AtomicBoolean pending = modulePending.get(key);
            if (pending != null) {
                pending.set(value);
            }
        }
    }

    private void onDisableAllClick() {
        long now = System.currentTimeMillis();
        if (!disableConfirmArmed || now - disableConfirmArmedAt > CONFIRM_WINDOW_MS) {
            disableConfirmArmed = true;
            disableConfirmArmedAt = now;
            return;
        }
        setAll(false);
        disableConfirmArmed = false;
    }

    private void updateLabels() {
        long now = System.currentTimeMillis();
        if (disableConfirmArmed && now - disableConfirmArmedAt > CONFIRM_WINDOW_MS) {
            disableConfirmArmed = false;
        }
        disableAllButton.setMessage(disableConfirmArmed
                ? Component.translatable("config.nourished.confirm.disableAll")
                : Component.translatable("config.nourished.modules.disableAll"));
    }

    @Override
    public boolean isEdited() {
        return true;
    }

    @Override
    public void save() {
        // Module values are saved by each entry's save consumer.
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
    public int getItemHeight() {
        return 24;
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
        updateLabels();
        enableAllButton.active = isEditable();
        disableAllButton.active = isEditable();
        enableAllButton.setX(x);
        enableAllButton.setY(y);
        enableAllButton.setWidth(btnWidth);
        disableAllButton.setX(x + btnWidth + GAP);
        disableAllButton.setY(y);
        disableAllButton.setWidth(btnWidth);
        enableAllButton.render(graphics, mouseX, mouseY, delta);
        disableAllButton.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        return List.of(enableAllButton, disableAllButton);
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return List.of(enableAllButton, disableAllButton);
    }
}


