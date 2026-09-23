package dev.maire.nourished.datagen.book.serverowners;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class FoodOverridesEntry extends EntryProvider {

    public static final String ID = "food_overrides";

    // Accent colors
    private static final String FILE_PATH = "[#](B88CFF)";
    private static final String FIELD = "[#](6FB8E8)";
    private static final String COMMAND = "[#](7BC96F)";

    // Reset
    private static final String RESET = "[#]()";

    public FoodOverridesEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("food_overrides", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Food Value Overrides");

        this.pageText("""
                Nourished supports per-item %svalue overrides%s for nutrients and calories. This lets server owners and modpack makers correct or reassign how any specific item is classified without touching the mod itself.

                Overrides live at %sconfig/nourished/food_overrides.json%s.
                """.formatted(
                FILE_PATH, RESET,
                FILE_PATH, RESET
        ));

        this.page("how_overrides_merge", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("How Overrides Merge");

        this.pageText("""
                An override entry has %sitem%s, %snutrients%s, %scalories%s, and %senabled%s fields. It is merged over normal classification, not a full replacement:

                Any nutrient key you list wins outright — including a value of %s0%s, which zeroes that nutrient out entirely.
                Any key you omit still falls back to whatever Nourished would normally classify for that item.
                %scalories%s is always a full override when present.
                """.formatted(
                FIELD, RESET, FIELD, RESET, FIELD, RESET, FIELD, RESET,
                FIELD, RESET,
                FIELD, RESET
        ));

        this.page("getting_starting_values", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Getting Starting Values");

        this.pageText("""
                Run %s/marieslib dump nourished_nutrients%s (or the "Export All Foods" button in the Scanner tab of the config screen) to write a reference folder — one file per nutrient, listing every item Nourished currently resolves into that category with its live values.

                These export files are read-only reference; editing them does nothing by itself.
                """.formatted(
                COMMAND, RESET
        ));

        this.page("writing_an_override", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Writing an Override");

        this.pageText("""
                Copy the entry you want to change out of an export file and into %sfood_overrides.json%s, then add %senabled: true%s:

                { "item": "minecraft:steak", "nutrients": { "proteins": 0.8 }, "calories": 60, "enabled": true }

                Only entries actually present in %sfood_overrides.json%s take effect — the export files themselves are ignored.
                """.formatted(
                FILE_PATH, RESET, FIELD, RESET,
                FILE_PATH, RESET
        ));

        this.page("reloading_changes", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Reloading Changes");

        this.pageText("""
                After editing %sfood_overrides.json%s, run %s/nourished reload%s to pick up the change without restarting the server.
                """.formatted(
                FILE_PATH, RESET, FIELD, RESET
        ));
    }

    @Override
    protected String entryName() {
        return "Food Value Overrides";
    }

    @Override
    protected String entryDescription() {
        return "Correcting or reassigning how a specific item is classified.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.CHEST);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
