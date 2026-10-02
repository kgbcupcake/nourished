package dev.maire.nourished.datagen.book.foodsafety;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Items;

public class GutHealthEntry extends EntryProvider {

    public static final String ID = "gut_health";

    // Severity tier colors
    private static final String MILD = "[#](E6C65C)";
    private static final String MEDIUM = "[#](E85D75)";
    private static final String SEVERE = "[#](E85D75)";

    // Food group colors
    private static final String VEGETABLES = "[#](7BC96F)";
    private static final String FRUITS = "[#](F5A623)";
    private static final String PROTEINS = "[#](E85D75)";
    private static final String GRAINS = "[#](E6C65C)";
    private static final String DAIRY = "[#](6FB8E8)";

    // Reset
    private static final String RESET = "[#]()";

    public GutHealthEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {

        this.page("gut_health", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Gut Health");

        this.pageText("""
                Your gut flora is a separate hidden stat that tracks how well your digestive system is holding up.

                It starts healthy and decays when you eat raw food. Let it get low enough and it starts amplifying the penalties you take — a bad gut makes raw food even worse.
                """);

        this.page("how_it_decays", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("How It Decays");

        this.pageText("""
                Every raw food event damages your gut flora by an amount based on the tier. %sSevere%s foods do the most damage, %sMild%s foods do the least.

                Gut damage doesn't happen instantly from one bad meal — it accumulates. Eating raw occasionally is survivable. Making it a habit is not.
                """.formatted(
                SEVERE, RESET,
                MILD, RESET
        ));

        this.page("how_it_recovers", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("How It Recovers");

        this.pageText("""
                Gut flora recovers two ways. Eating **cooked** food gives an immediate boost proportional to how well-cooked it is — a fully cooked meal helps more than a barely-seared one.

                It also regenerates passively over time, and that passive rate gets a **diversity bonus** the more balanced your overall diet is. A well-rounded diet across all six groups isn't just good for your bars — it heals your gut faster too.
                """);

        this.page("building_resistance", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("Building Resistance");

        this.pageText("""
                Beyond gut health, keeping specific nutrient bars topped up builds direct **resistance** to raw food penalties — reducing how hard each event hits before it even happens. Different tiers respond to different groups:

                %sMild%s resistance comes from %sVegetables%s and %sFruits%s.
                %sMedium%s resistance comes from %sProteins%s and %sGrains%s.
                %sSevere%s resistance comes from %sProteins%s and %sDairy%s.
                """.formatted(
                MILD, RESET, VEGETABLES, RESET, FRUITS, RESET,
                MEDIUM, RESET, PROTEINS, RESET, GRAINS, RESET,
                SEVERE, RESET, PROTEINS, RESET, DAIRY, RESET
        ));

        this.page("the_takeaway", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));

        this.pageTitle("The Takeaway");

        this.pageText("""
                Cook your food whenever possible. Keep your overall diet balanced to speed up gut recovery, and pay attention to Proteins in particular — it contributes to resistance against both Medium and Severe raw food.

                If your gut is already low, prioritize cooked meals until it recovers before worrying about the rest of your bars.
                """);
    }

    @Override
    protected String entryName() {
        return "Gut Health";
    }

    @Override
    protected String entryDescription() {
        return "The hidden stat that makes repeated raw eating worse over time.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.FERMENTED_SPIDER_EYE);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
