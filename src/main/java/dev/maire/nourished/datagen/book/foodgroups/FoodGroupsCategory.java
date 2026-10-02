package dev.maire.nourished.datagen.book.foodgroups;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookCategoryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.book.BookCategoryBackgroundParallaxLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class FoodGroupsCategory extends CategoryProvider {

    public static final String ID = "food_groups";

    public FoodGroupsCategory(SingleBookSubProvider parent) {
        super(parent);
    }

    @Override
    protected String[] generateEntryMap() {
        /*
         * A simple linear progression through the five food groups.
         */
        return new String[]{
                "v________",
                "_________",
                "__f______",
                "_________",
                "____g____",
                "_________",
                "______p__",
                "_________",
                "________d"
        };
    }

    @Override
    protected void generateEntries() {

        // Vegetables
        var vegetables = this.add(
                new VegetablesEntry(this).generate('v')
        );

        // Fruits
        var fruits = this.add(
                new FruitsEntry(this).generate('f')
        )
                .withParent(vegetables)
                .withCondition(this.condition().entryRead(vegetables));

        // Grains
        var grains = this.add(
                new GrainsEntry(this).generate('g')
        )
                .withParent(fruits)
                .withCondition(this.condition().entryRead(fruits));

        // Proteins
        var proteins = this.add(
                new ProteinsEntry(this).generate('p')
        )
                .withParent(grains)
                .withCondition(this.condition().entryRead(grains));

        // Dairy
        this.add(
                new DairyEntry(this).generate('d')
        )
                .withParent(proteins)
                .withCondition(this.condition().entryRead(proteins));
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
        return "Food Groups";
    }

    @Override
    protected String categoryDescription() {
        return "Explore each food group, their bonuses, and the foods that belong to them.";
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(Items.APPLE);
    }

    @Override
    public String categoryId() {
        return ID;
    }
}
