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

public class DairyEntry extends EntryProvider {

    public static final String ID = "dairy";

    private static final String PENALTY = "[#](e05252)";
    private static final String BONUS = "[#](52c252)";
    private static final String RESET = "[#]()";

    public DairyEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("spotlight", () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(Items.MILK_BUCKET))
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Dairy");

        this.pageText("""
                Dairy foods strengthen bones and body. %sPenalty:%s reduced knockback resistance when depleted. %sBonus:%s increased armor toughness when maintained. A single cow pen keeps this group topped up easily.
                """.formatted(
                PENALTY, RESET,
                BONUS, RESET
        ));

        this.page("vanilla_sources", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Vanilla Dairy Sources");

        this.pageText("""
                **Milk Bucket**: the primary vanilla dairy source. Bucket a cow whenever passing by.\s\s
                Vanilla has only one dairy item, which means this group has almost no rotation options without a food mod installed. The upside: there is nothing to overthink. Keep a cow, bucket regularly.
                """);

        this.page("armor_toughness", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Armor Toughness Bonus");

        this.pageText("""
                The Dairy bonus (increased armor toughness) makes incoming damage more predictable and reduces the effectiveness of high-damage hits.\s\s
                This makes Dairy especially valuable in combat-heavy playthroughs. Players who fight frequently or play on hard difficulty should treat this group as a high priority.
                """);

        this.page("knockback_penalty", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Knockback Resistance Penalty");

        this.pageText("""
                The Dairy penalty (reduced knockback resistance) means depleted Dairy makes you easier to push around in combat. This is especially dangerous around ledges, lava, or enemies that spam attacks.\s\s
                Keep this group above zero before entering any dungeon or boss fight.
                """);

        this.page("vanilla_secondary", () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(Items.MILK_BUCKET))
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Dairy: Vanilla");

        this.pageText("""
                Milk bucket is your only vanilla dairy source. Since there is no rotation possible, diminishing returns do not apply the same way: just bucket a cow every morning and evening and this group stays healthy with minimal effort.
                """);

        this.page("herbs_and_harvest", () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(Items.MILK_BUCKET))
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText())
                .withCondition(BookModLoadedConditionModel.create().withModId("herbsandharvest")));

        this.pageTitle("Dairy: Herbs & Harvest");

        this.pageText("""
                Herbs & Harvest adds aged cheeses including Swiss, cheddar, and brie. These provide significantly higher dairy nutrition than plain milk and introduce distinct cheese families for rotation. See the **Compat Mods** chapter for more.
                """);

        this.page("farming_tips", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Farming Tips");

        this.pageText("""
                Two cows is the minimum for a sustainable dairy setup: one to breed from, one to milk. Three or more ensures you always have milk available without waiting.\s\s
                Keep your cow pen close to your base. Dairy is the easiest group to maintain once you have any cows at all; the challenge is remembering to actually bucket them each day.
                """);
    }

    @Override
    protected String entryName() {
        return "Dairy";
    }

    @Override
    protected String entryDescription() {
        return "Bone-strengthening foods that increase armor toughness.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.MILK_BUCKET);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
