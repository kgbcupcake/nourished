package dev.maire.nourished.client.screen.itemeditor;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieContext;
import dev.marie.framework.runtime.SourceClassificationRegistry;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.itemeditor.ItemEditorPage;
import dev.marie.framework.ui.itemeditor.ItemEditorScreenProvider;
import dev.marie.framework.ui.toolbox.OptionLayout;
import dev.marie.framework.ui.toolbox.ToggleOption;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * File &gt; Nutrient Groups: a checkbox front end for the same per-nutrient weights the generic
 * Values page's sliders edit ({@code SourceClassificationRegistry}, read/written the same way
 * {@code ItemEditorValuesModel} does, scoped via {@link MarieContext#runAs}) — for the common case
 * of just wanting an item to count toward (or not count toward) a nutrient bar at all, without
 * tuning an exact 0-10 weight. Checking a group that has no existing weight sets it to {@value
 * #DEFAULT_WEIGHT} (a plain "yes, this counts" baseline); unchecking a group zeroes it. A weight
 * already set above zero (including one fine-tuned on the Values page) shows as checked and is left
 * exactly as it was unless this page's own checkbox is toggled — switching between the two pages
 * never silently overwrites the other's edits, since both always reload fresh from the registry
 * when opened and only touch the field(s) actually changed on save. Calories/Enabled are read
 * from, and written back to, whatever the existing entry already has — this page never touches
 * them.
 */
@ApiStatus.Internal
public final class NutrientGroupsScreenProvider implements ItemEditorScreenProvider {

    private static final float DEFAULT_WEIGHT = 1.0f;

    @Override
    public String menuLabel() {
        return "Nutrient Groups";
    }

    @Override
    public ItemEditorPage buildScreen(String modId, @Nullable String sourceId, ItemStack stack) {
        return new NutrientGroupsScreen(modId, sourceId);
    }

    private static final class NutrientGroupsScreen implements ItemEditorPage {

        private final String modId;
        @Nullable
        private final String sourceId;
        private final Map<String, Boolean> checked = new LinkedHashMap<>();
        private final OptionLayout layout;

        NutrientGroupsScreen(String modId, @Nullable String sourceId) {
            this.modId = modId;
            this.sourceId = sourceId;
            loadChecked();
            this.layout = new OptionLayout("nourished-nutrient-groups");
            layout.addTab("");
            for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
                layout.addRow(new ToggleOption(def.displayName(),
                        () -> checked.getOrDefault(def.key(), false),
                        v -> checked.put(def.key(), v),
                        () -> {}));
            }
        }

        private void loadChecked() {
            checked.clear();
            if (sourceId == null) {
                return;
            }
            Set<String> active = new LinkedHashSet<>();
            MarieContext.runAs(MarieContext.forMod(modId), () -> {
                SourceClassificationRegistry.SourceClassification existing = SourceClassificationRegistry.get(sourceId);
                if (existing != null) {
                    for (Map.Entry<String, Float> e : existing.values().entrySet()) {
                        if (e.getValue() > 0f) {
                            active.add(e.getKey());
                        }
                    }
                }
            });
            for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
                checked.put(def.key(), active.contains(def.key()));
            }
        }

        @Override
        public void save() {
            if (sourceId == null) {
                return;
            }
            MarieContext.runAs(MarieContext.forMod(modId), () -> {
                SourceClassificationRegistry.SourceClassification existing = SourceClassificationRegistry.get(sourceId);
                Map<String, Float> values = existing != null
                        ? new LinkedHashMap<>(existing.values())
                        : new LinkedHashMap<>();
                int calories = existing != null ? existing.calories() : 0;
                boolean enabled = existing == null || existing.enabled();
                for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
                    boolean isChecked = checked.getOrDefault(def.key(), false);
                    float currentWeight = values.getOrDefault(def.key(), 0f);
                    if (isChecked && currentWeight <= 0f) {
                        values.put(def.key(), DEFAULT_WEIGHT);
                    } else if (!isChecked) {
                        values.remove(def.key());
                    }
                }
                SourceClassificationRegistry.setOverride(sourceId, values, calories, enabled);
                SourceClassificationRegistry.save();
            });
        }

        @Override
        public void revert() {
            loadChecked();
        }

        @Override
        public String id() {
            return "nourished-nutrient-groups-screen";
        }

        @Override
        public Constraint constraint() {
            return layout.constraint();
        }

        @Override
        public void render(RenderContext context, Bounds bounds) {
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
    }
}
