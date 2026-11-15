package dev.maire.nourished.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.ModonomiconLanguageProvider;
import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookModel;
import dev.maire.nourished.datagen.book.activity.ActivityCategory;
import dev.maire.nourished.datagen.book.advanced.AdvancedCategory;
import dev.maire.nourished.datagen.book.food.FoodCategory;
import dev.maire.nourished.datagen.book.gettingstarted.GettingStartedCategory;
import dev.maire.nourished.datagen.book.guthealth.GutHealthCategory;
import dev.maire.nourished.datagen.book.nutrition.NutritionCategory;
import dev.maire.nourished.datagen.book.progression.ProgressionCategory;
import net.minecraft.resources.ResourceLocation;

public class NourishedGuideBook extends SingleBookSubProvider {

    public static final String ID = "nourished_guide";

    public NourishedGuideBook(String modId, ModonomiconLanguageProvider lang) {
        super(ID, modId, lang);
    }

    @Override
    protected BookModel additionalSetup(BookModel book) {
        // "node" display mode (Modonomicon's default) renders as a quest-map graph, Thaumonomicon-style.
        return book.withModel(
                ResourceLocation.parse("nourished:nourished_book")
        )
                // Our own item instead of the shared modonomicon:modonomicon one, so the tooltip's
                // mod-name attribution reads "Nourished" once instead of "Modonomicon" twice.
                .withCustomBookItem(ResourceLocation.parse("nourished:nourished_book"));
    }

    @Override
    protected void registerDefaultMacros() {
        // none yet
    }

    @Override
    protected void generateCategories() {
        this.add(new GettingStartedCategory(this).generate());
        this.add(new FoodCategory(this).generate());
        this.add(new NutritionCategory(this).generate());
        this.add(new ActivityCategory(this).generate());
        this.add(new GutHealthCategory(this).generate());
        this.add(new ProgressionCategory(this).generate());
        this.add(new AdvancedCategory(this).generate());
    }

    @Override
    protected String bookName() {
        return "Nourished Guide";
    }

    @Override
    protected String bookTooltip() {
        return "Everything you need to know about eating well.";
    }
}