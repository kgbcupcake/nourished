package dev.maire.nourished.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookCategoryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.book.BookCategoryBackgroundParallaxLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

public abstract class GuideCategory extends CategoryProvider {

    private final String id;
    private final String name;
    private final String description;
    private final ItemLike icon;

    protected GuideCategory(SingleBookSubProvider parent, String id, String name, String description, ItemLike icon) {
        super(parent);
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
    }

    // Reference categories: draw the relationship line, never lock the entry.
    protected BookEntryModel under(BookEntryModel parent, BookEntryModel child) {
        return child.withParent(parent);
    }

    protected BookEntryModel after(BookEntryModel parent, BookEntryModel child) {
        return child.withParent(parent).withCondition(this.condition().entryRead(parent));
    }

    @Override
    protected BookCategoryModel additionalSetup(BookCategoryModel category) {
        return category.withBackgroundParallaxLayers(
                layer("base", 0.7f),
                layer("1", 1.0f),
                layer("2", 1.4f)
        );
    }

    private static BookCategoryBackgroundParallaxLayer layer(String name, float speed) {
        return new BookCategoryBackgroundParallaxLayer(
                ResourceLocation.parse("modonomicon:textures/gui/parallax/flow/" + name + ".png"), speed, -1);
    }

    @Override
    protected String categoryName() {
        return name;
    }

    @Override
    protected String categoryDescription() {
        return description;
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(icon);
    }

    @Override
    public String categoryId() {
        return id;
    }
}
