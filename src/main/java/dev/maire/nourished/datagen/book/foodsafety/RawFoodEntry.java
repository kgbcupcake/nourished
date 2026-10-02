package dev.maire.nourished.datagen.book.foodsafety;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class RawFoodEntry extends EntryProvider {

    public static final String ID = "raw_food";

    // Severity tier colors
    private static final String MILD = "[#](E6C65C)";
    private static final String MEDIUM = "[#](E85D75)";
    private static final String SEVERE = "[#](E85D75)";

    // Reset
    private static final String RESET = "[#]()";

    public RawFoodEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("eating_raw_food", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Eating Raw Food");

        this.pageText("""
                Eating raw meat and other uncooked foods %sisn't free%s. Nourished tracks what you eat and penalizes you when you skip the campfire.

                The penalty scales with how raw the food is — there are three tiers: **Mild**, **Medium**, and **Severe**. Foods that are fine raw (bread, apples, carrots) skip all of this entirely.
                """.formatted(
                SEVERE, RESET
        ));

        this.page("mild", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Mild");

        this.pageText("""
                %sMild%s penalties apply to foods that are technically edible raw but clearly not ideal — lightly processed or borderline items.

                You take a small nutrient penalty and miss out on a portion of the nutrition the cooked version would have given. Not a disaster, but it adds up.
                """.formatted(
                MILD, RESET
        ));

        this.page("medium", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Medium");

        this.pageText("""
                %sMedium%s penalties apply to clearly raw foods — standard raw meats like beef, pork, and chicken.

                The nutrient penalty is larger and a significant portion of the nutrition is lost compared to eating the cooked version. You are also more likely to trigger gut flora damage at this tier.
                """.formatted(
                MEDIUM, RESET
        ));

        this.page("severe", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Severe");

        this.pageText("""
                %sSevere%s penalties apply to foods that should never be eaten raw under any circumstances.

                Nutrient bars take a meaningful hit, nearly all of the cooked nutrition is denied, and your gut flora takes the hardest damage. Just cook it.
                """.formatted(
                SEVERE, RESET
        ));

        this.page("missed_opportunity", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Missed Opportunity");

        this.pageText("""
                Every tier includes a **missed opportunity** multiplier. This is the fraction of nutrition the cooked version would have given that you simply don't get.

                Eating raw doesn't just hurt you — it wastes the food. A raw beef gives far less Protein than a cooked one, even before the penalty kicks in.
                """);

        this.page("eating_the_same_food_again", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Eating the Same Food Again");

        this.pageText("""
                Nourished remembers recent raw food events for a short window. If you eat the same raw item again inside that window, the system does not roll a fresh random debuff — it **extends the one you already have**, stacking its duration on top.

                Back-to-back raw meals of the same item make the debuff last longer, not shorter. Switching to a cooked meal (or a different food entirely) breaks the chain.
                """);
    }

    @Override
    protected String entryName() {
        return "Raw Food";
    }

    @Override
    protected String entryDescription() {
        return "What happens when you skip the campfire.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.BEEF);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
