package dev.maire.nourished.datagen.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookImagePageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class FoodTooltipsEntry extends EntryProvider {

    public static final String ID = "food_tooltips";

    public FoodTooltipsEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("tooltip_image", () -> BookImagePageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText())
                .withImages(ResourceLocation.fromNamespaceAndPath("nourished", "textures/images/tooltip.png")));
        this.pageTitle("Food Tooltips");
        this.pageText("Hover over any food item to see which food group it belongs to and how much nutrition it provides.");

        this.page("reading_tooltips", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Reading Tooltips");
        this.pageText("""
                The tooltip shows the food's **group**, its **nutrition value**, and its **family**. Foods from the same family give diminishing returns — variety within a group matters as much as the group itself.
                """);
    }

    @Override
    protected String entryName() {
        return "Food Tooltips";
    }

    @Override
    protected String entryDescription() {
        return "Reading a food's group and nutrition value at a glance.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.CARROT);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
