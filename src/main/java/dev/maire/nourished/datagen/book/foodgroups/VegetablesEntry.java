package dev.maire.nourished.datagen.book.foodgroups;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.condition.BookModLoadedConditionModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookSpotlightPageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class VegetablesEntry extends EntryProvider {

    public static final String ID = "vegetables";

    private static final String PENALTY = "[#](e05252)";
    private static final String BONUS = "[#](52c252)";
    private static final String RESET = "[#]()";

    public VegetablesEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("spotlight", () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(Items.CARROT))
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Vegetables");

        this.pageText("""
                Vegetables are hearty, grounding foods. %sPenalty:%s reduced movement speed when depleted. %sBonus:%s improved hunger saturation when maintained. Carrots, potatoes, and beetroot all qualify.
                """.formatted(
                PENALTY, RESET,
                BONUS, RESET
        ));

        this.page("vanilla_sources", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Vanilla Vegetable Sources");

        this.pageText("""
                - **Carrot**: found in village farms or zombie drops; very easy to obtain early.
                - **Potato**: also found in villages; bake for higher nutrition value.
                - **Baked Potato**: cook any potato for a significant nutrition boost.
                - **Beetroot**: grown from seeds; lower yield than carrots but still reliable.
                """);

        this.page("golden_carrot", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Golden Carrot");

        this.pageText("""
                **Golden Carrot** is the single highest-nutrition vanilla vegetable. It fills the bar noticeably more than a plain carrot and also provides excellent hunger saturation.\s\s
                If you have a gold surplus, a small stock of golden carrots makes a great emergency top-up for the Vegetables group.
                """);

        this.page("farming_tips", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Farming Tips");

        this.pageText("""
                Carrots and potatoes are the most efficient vanilla vegetables to farm: they replant themselves and grow quickly with irrigation.\s\s
                A mixed row of carrots, potatoes, and beetroot covers three distinct vegetable families. Plant them side by side in alternating rows to make rotating your diet effortless.
                """);

        this.page("vanilla_secondary", () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(Items.BEETROOT))
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Vegetables: Vanilla");

        this.pageText("""
                Beetroot is your best secondary vegetable alongside carrots and potatoes. A small mixed crop farm covers this group reliably from early game onward.\s\s
                Beetroot soup counts as Vegetables and provides more nutrition per craft than raw beetroot alone.
                """);

        this.page("farm_and_charm", () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(Items.CARROT))
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText())
                .withCondition(BookModLoadedConditionModel.create().withModId("farm_and_charm")));

        this.pageTitle("Vegetables: Farm & Charm");

        this.pageText("""
                Farm & Charm adds tomatoes, onions, peppers, and more. These introduce new vegetable families and significantly expand rotation options. See the **Compat Mods** chapter for the full Farm & Charm food list.
                """);

        this.page("rotation_strategy", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Rotation Strategy");

        this.pageText("""
                The main vanilla vegetable families are Carrot, Potato, and Beetroot. Rotating through all three maximizes your nutrition gain compared to eating only one type.\s\s
                **Baked potato** and **raw potato** share a family, so cooking improves nutrition value but does not count as variety. Use both baked and raw in a pinch, but branch out to carrots and beetroot for best results.
                """);
    }

    @Override
    protected String entryName() {
        return "Vegetables";
    }

    @Override
    protected String entryDescription() {
        return "Hearty, grounding foods that improve hunger saturation.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.CARROT);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
