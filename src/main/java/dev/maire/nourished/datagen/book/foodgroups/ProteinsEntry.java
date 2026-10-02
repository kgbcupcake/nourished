package dev.maire.nourished.datagen.book.foodgroups;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookSpotlightPageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class ProteinsEntry extends EntryProvider {

    public static final String ID = "proteins";

    private static final String PENALTY = "[#](e05252)";
    private static final String BONUS = "[#](52c252)";
    private static final String RESET = "[#]()";

    public ProteinsEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("spotlight", () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(Items.COOKED_BEEF))
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Proteins");

        this.pageText("""
                Proteins are rich, sustaining foods. %sPenalty:%s reduced attack damage when depleted. %sBonus:%s increased max health when maintained. Cooked meats and fish are your primary sources.
                """.formatted(
                PENALTY, RESET,
                BONUS, RESET
        ));

        this.page("vanilla_sources", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Vanilla Meat Sources");

        this.pageText("""
                **Cooked Beef** — highest nutrition of vanilla meats; keep a cow farm.
                **Cooked Porkchop** — nearly equal to beef; pigs breed quickly.
                **Cooked Chicken** — lower nutrition but very common and fast to farm.
                **Cooked Mutton** — solid mid-tier protein from sheep.
                **Cooked Rabbit** — lower yield but easy to obtain in the wild early on.
                """);

        this.page("fish_sources", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Fish Sources");

        this.pageText("""
                **Cooked Salmon** and **Cooked Cod** are excellent early-game proteins available near any river or ocean before you have an animal farm established.

                Fishing also yields **Tropical Fish** which counts as Proteins, though it has low nutrition. Use it as a supplement rather than a primary source.
                """);

        this.page("fish_spotlight", () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(Items.COOKED_SALMON))
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Proteins — Fish");

        this.pageText("""
                Cooked salmon and cod are excellent protein sources and easy to obtain near any river or ocean. A fishing rod from early game makes this group trivial to maintain before any farm is built.

                Auto-fishing farms are very effective for this group if you want a fully passive protein supply.
                """);

        this.page("non_meat", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Non-Meat Proteins");

        this.pageText("""
                Not all proteins come from animals. Several plant-based and processed foods also count:

                **Cooked Chicken Egg** (if enabled) — high nutrition density.
                **Mushroom Stew** — counts as Proteins in many configurations.

                Check tooltips on any food item to confirm which group it belongs to on your server.
                """);

        this.page("farming_tips", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Farming Tips");

        this.pageText("""
                A cow, pig, and chicken pen near your base covers three distinct protein families at once. Breed them regularly — you want a surplus, not just enough to survive.

                If you are playing early-game without a farm, keep a fishing rod ready. Fishing covers Proteins reliably with no infrastructure required and also produces bonus loot.
                """);

        this.page("rotation_strategy", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Rotation Strategy");

        this.pageText("""
                Each animal type is its own protein family. Eating beef, then porkchop, then chicken gives better returns than eating three steaks in a row.

                Fish counts as a separate family from land meat — switching between a cooked salmon and cooked beef at the same meal is an easy way to avoid diminishing returns on both.
                """);
    }

    @Override
    protected String entryName() {
        return "Proteins";
    }

    @Override
    protected String entryDescription() {
        return "Rich, sustaining foods that increase max health.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.COOKED_BEEF);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
