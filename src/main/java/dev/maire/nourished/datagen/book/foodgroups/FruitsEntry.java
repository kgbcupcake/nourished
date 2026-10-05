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

public class FruitsEntry extends EntryProvider {

    public static final String ID = "fruits";

    private static final String PENALTY = "[#](e05252)";
    private static final String BONUS = "[#](52c252)";
    private static final String RESET = "[#]()";

    public FruitsEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("spotlight", () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(Items.APPLE))
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Fruits");

        this.pageText("""
                Fruits are sweet, vitamin-rich foods. %sPenalty:%s reduced max health when depleted. %sBonus:%s increased regeneration speed when maintained. Apples, melons, and berries all count.
                """.formatted(
                PENALTY, RESET,
                BONUS, RESET
        ));

        this.page("vanilla_sources", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Vanilla Fruit Sources");

        this.pageText("""
                - **Apple**: drops from oak and dark oak trees. Plant a small orchard early.
                - **Sweet Berries**: found wild in taiga biomes; bushes are renewable and fast.
                - **Melon Slice**: grown from seeds found in dungeon chests or jungle temples.
                - **Glow Berries**: found in lush caves; low nutrition but accessible early.
                """);

        this.page("golden_and_enchanted", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Golden & Enchanted");

        this.pageText("""
                **Golden Apple** and **Enchanted Golden Apple** both count as Fruits and give high nutrition, but they are expensive luxuries, not reliable staples.\s\s
                Save them for emergencies. A melon farm is far more cost-effective for keeping this group stable day to day.
                """);

        this.page("farming_tips", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Farming Tips");

        this.pageText("""
                Melon farms are among the most space-efficient for Fruits. A 3×3 patch of melon stems produces enough slices to keep this group topped up indefinitely.\s\s
                Sweet berry bushes planted in rows are low-maintenance. They grow without irrigation and produce reliably each season without replanting.
                """);

        this.page("vanilla_secondary", () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(Items.SWEET_BERRIES))
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Fruits: Vanilla");

        this.pageText("""
                Sweet berries are one of your most accessible wild fruit sources alongside apples and melons. Keep a berry bush farm early on to maintain this group easily.\s\s
                Apple trees near your base make passive top-ups effortless: just grab drops when you pass by.
                """);

        this.page("croptopia", () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(Items.SWEET_BERRIES))
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText())
                .withCondition(BookModLoadedConditionModel.create().withModId("croptopia")));

        this.pageTitle("Fruits: Croptopia");

        this.pageText("""
                With Croptopia installed, the Fruits group expands significantly. Strawberries, peaches, mangoes, kiwis, and many more exotic fruits are available. See the **Compat Mods** chapter for the full Croptopia food list.
                """);

        this.page("rotation_strategy", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Rotation Strategy");

        this.pageText("""
                Fruits has several distinct food families: Apple, Berry, and Melon. Rotating across all three gives you better nutrition gain than eating only apples.\s\s
                If you have Croptopia or another fruit mod installed, you have even more families to rotate through; check tooltips to see which family each fruit belongs to.
                """);
    }

    @Override
    protected String entryName() {
        return "Fruits";
    }

    @Override
    protected String entryDescription() {
        return "Sweet, vitamin-rich foods that speed up regeneration.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.APPLE);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
