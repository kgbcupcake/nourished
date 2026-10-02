package dev.maire.nourished.datagen.book.serverowners;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookCategoryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.book.BookCategoryBackgroundParallaxLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class ServerOwnersCategory extends CategoryProvider {

    public static final String ID = "server_owners";

    public ServerOwnersCategory(SingleBookSubProvider parent) {
        super(parent);
    }

    @Override
    protected String[] generateEntryMap() {
        // Two entries, a short side-by-side pair.
        return new String[]{
                "e___",
                "____",
                "__f_"
        };
    }

    @Override
    protected void generateEntries() {

        // Configuring Effects
        var configuringEffects = this.add(
                new ConfiguringEffectsEntry(this).generate('e')
        );

        // Food Value Overrides
        this.add(
                new FoodOverridesEntry(this).generate('f')
        )
                .withParent(configuringEffects)
                .withCondition(this.condition().entryRead(configuringEffects));
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
        return "Server Owners";
    }

    @Override
    protected String categoryDescription() {
        return "Configuration options and per-item overrides for server administrators.";
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(Items.WRITABLE_BOOK);
    }

    @Override
    public String categoryId() {
        return ID;
    }
}
