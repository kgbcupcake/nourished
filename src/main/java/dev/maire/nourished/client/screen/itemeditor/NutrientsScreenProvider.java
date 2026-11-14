package dev.maire.nourished.client.screen.itemeditor;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.itemeditor.ItemEditorPage;
import dev.marie.framework.ui.itemeditor.ItemEditorScreenProvider;
import dev.marie.framework.ui.toolbox.OptionLayout;
import dev.marie.framework.ui.toolbox.SliderOption;
import dev.marie.framework.ui.toolbox.ToggleOption;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.micro.MicroConfigFile;
import dev.maire.nourished.core.micro.MicroDef;
import dev.maire.nourished.core.micro.MicroProfileResolver;
import dev.maire.nourished.core.micro.MicroRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** File &gt; Nutrients: per-item Omega-3/Fiber/... values, saved to micros.json. */
@ApiStatus.Internal
public final class NutrientsScreenProvider implements ItemEditorScreenProvider {

    @Override
    public String menuLabel() {
        return "Nutrients";
    }

    @Override
    public ItemEditorPage buildScreen(String modId, @Nullable String sourceId, ItemStack stack) {
        return new NutrientsScreen(sourceId, stack);
    }

    private static final class NutrientsScreen implements ItemEditorPage {

        private static final double MAX_VALUE = 3.0d;

        @Nullable
        private final ResourceLocation itemId;
        private final ItemStack stack;
        private final List<MicroDef> defs;
        private final Map<String, Float> values = new LinkedHashMap<>();
        private boolean custom;
        private final OptionLayout layout;

        NutrientsScreen(@Nullable String sourceId, ItemStack stack) {
            this.itemId = sourceId != null ? ResourceLocation.tryParse(sourceId) : null;
            this.stack = stack;
            this.defs = List.copyOf(MicroRegistry.defs(true));
            this.layout = new OptionLayout("nourished-nutrients");
            layout.addTab("");
            if (ItemEditorRemoteGuard.isRemoteServer()) {
                layout.addRow(ItemEditorRemoteGuard.noticeRow());
            }
            layout.addRow(new ToggleOption("Custom Nutrient Values", () -> custom, v -> custom = v, () -> {}));
            for (MicroDef def : defs) {
                layout.addRow(SliderOption.ofDecimal(def.displayName(),
                        () -> values.getOrDefault(def.key(), 0f),
                        v -> values.put(def.key(), (float) v),
                        0d, MAX_VALUE, 0.05d, 2, "", () -> {}));
                layout.enabledWhenLast(() -> custom);
            }
            ItemEditorRemoteGuard.disableIfRemote(layout);
            revert();
        }

        @Override
        public void save() {
            if (itemId == null || ItemEditorRemoteGuard.blockSave("Nutrients")) {
                return;
            }
            IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
            if (server == null) {
                return;
            }
            Map<String, Float> toWrite = custom ? new LinkedHashMap<>(values) : null;
            ResourceLocation id = itemId;
            server.execute(() -> {
                try {
                    MicroConfigFile.setItem(server, id, toWrite);
                } catch (IOException ex) {
                    Nourished.LOGGER.warn("[ItemEditor] Could not save nutrients for {}: {}", id, ex.getMessage());
                }
            });
        }

        @Override
        public void revert() {
            values.clear();
            Map<String, Float> saved = itemId != null ? MicroConfigFile.itemEntry(itemId) : null;
            custom = saved != null;
            if (saved != null) {
                values.putAll(saved);
            } else {
                Minecraft mc = Minecraft.getInstance();
                values.putAll(MicroProfileResolver.resolve(stack, mc.level));
            }
        }

        @Override
        public String id() {
            return "nourished-nutrients-screen";
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
