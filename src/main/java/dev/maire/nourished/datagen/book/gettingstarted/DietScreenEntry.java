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

public class DietScreenEntry extends EntryProvider {

    public static final String ID = "diet_screen";

    public DietScreenEntry(CategoryProvider parent) {
        super(parent);
    }

    @Override
    protected void generatePages() {
        this.page("diet_screen_image", () -> BookImagePageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText())
                .withImages(ResourceLocation.fromNamespaceAndPath("nourished", "textures/images/diet_screen.png")));
        this.pageTitle("Diet Screen");
        this.pageText("The full diet screen shows detailed breakdowns of every food group and your recent eating history.");

        this.page("reading_the_screen", () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle("Reading the Screen");
        this.pageText("""
                Each food group shows a **fill bar** representing your current level. Bars drain over time through nutrient decay.

                Hover over a bar to see the exact value, your active bonus or penalty, and which foods you have recently eaten in that group.
                """);
    }

    @Override
    protected String entryName() {
        return "Diet Screen";
    }

    @Override
    protected String entryDescription() {
        return "A detailed breakdown of every food group.";
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(Items.PAPER);
    }

    @Override
    protected String entryId() {
        return ID;
    }
}
