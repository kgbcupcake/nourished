package dev.maire.nourished.datagen.book.foodsafety;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookCategoryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.book.BookCategoryBackgroundParallaxLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class FoodSafetyCategory extends CategoryProvider {

    public static final String ID = "food_safety";

    public FoodSafetyCategory(SingleBookSubProvider parent) {
        super(parent);
    }

    @Override
    protected String[] generateEntryMap() {
        // Two entries, a short side-by-side pair.
        return new String[]{
                "r___",
                "____",
                "__g_"
        };
    }

    @Override
    protected void generateEntries() {

        // Raw Food
        var rawFood = this.add(
                new RawFoodEntry(this).generate('r')
        );

        // Gut Health
        this.add(
                new GutHealthEntry(this).generate('g')
        )
                .withParent(rawFood)
                .withCondition(this.condition().entryRead(rawFood));
    }

    @Override
    protected BookCategoryModel additionalSetup(BookCategoryModel category) {
        return category.withBackgroundParallaxLayers(
                new BookCategoryBackgroundParallaxLayer(
                        ResourceLocation.parse(
                                "modonomicon:textures/gui/parallax/flow/base.png"
                        ),
                        0.7f,
                        -1
                ),
                new BookCategoryBackgroundParallaxLayer(
                        ResourceLocation.parse(
                                "modonomicon:textures/gui/parallax/flow/1.png"
                        ),
                        1.0f,
                        -1
                ),
                new BookCategoryBackgroundParallaxLayer(
                        ResourceLocation.parse(
                                "modonomicon:textures/gui/parallax/flow/2.png"
                        ),
                        1.4f,
                        -1
                )
        );
    }

    @Override
    protected String categoryName() {
        return "Food Safety";
    }

    @Override
    protected String categoryDescription() {
        return "What happens when you eat raw food, and how your gut handles the damage.";
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(Items.BEEF);
    }

    @Override
    public String categoryId() {
        return ID;
    }
}
