package dev.maire.nourished.datagen.book.serverowners;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class ConfiguringEffectsEntry extends EntryProvider {

    public static final String ID = "configuring_effects";

    // Accent colors
    private static final String FILE_PATH = "[#](B88CFF)";
    private static final String FIELD = "[#](6FB8E8)";

    // Reset
    private static final String RESET = "[#]()";

    public ConfiguringEffectsEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("configuring_effects", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Configuring Effects");

        this.pageText("""
                Nourished exposes every bonus and penalty as a status effect entry in a config file. Server owners can change which potion effect is applied, its amplifier and duration, and the nutrient threshold that triggers it — no restart required.

                The config lives at %sconfig/nourished/effects.json%s.
                """.formatted(
                FILE_PATH, RESET
        ));

        this.page("effect_entry_fields", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Effect Entry Fields");

        this.pageText("""
                Each entry in the array is one effect tied to one nutrient:

                %seffect%s — the status effect id to apply, e.g. %sminecraft:weakness%s.
                %snutrient%s — the nutrient key this entry watches, or %sall%s.
                %strigger%s — %sbelow%s, %sabove%s, or %sall_above%s: when the effect fires relative to %sthreshold%s.
                %samplifier%s / %sduration_ticks%s — potion strength and how long it lasts.
                %senabled%s — set %sfalse%s to disable the entry without deleting it.
                """.formatted(
                FIELD, RESET, FIELD, RESET,
                FIELD, RESET, FIELD, RESET,
                FIELD, RESET, FIELD, RESET, FIELD, RESET, FIELD, RESET, FIELD, RESET,
                FIELD, RESET, FIELD, RESET,
                FIELD, RESET, FIELD, RESET
        ));

        this.page("disabling_effects", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Disabling Effects");

        this.pageText("""
                To turn off a specific bonus or penalty, set its %senabled%s field to %sfalse%s. The nutrient bar keeps tracking as normal — the entry is simply skipped when deciding which effects to apply.

                This is useful for lighter server experiences or modpacks with their own progression systems. Built-in effects are only regenerated into the file if it is missing or invalid, so a disabled entry stays disabled.
                """.formatted(
                FIELD, RESET, FIELD, RESET
        ));

        this.page("reloading_changes", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Reloading Changes");

        this.pageText("""
                After editing %seffects.json%s, run %s/nourished reload%s to pick up the change without restarting the server.

                Effects registered by other mods through the API or KubeJS are merged into this same file at runtime, so it always reflects everything currently active.
                """.formatted(
                FILE_PATH, RESET, FIELD, RESET
        ));
    }

    @Override
    protected String entryName() {
        return "Configuring Effects";
    }

    @Override
    protected String entryDescription() {
        return "Tuning the potion effects tied to each nutrient bar.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.COMMAND_BLOCK);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
