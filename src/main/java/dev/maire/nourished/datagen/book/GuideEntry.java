package dev.maire.nourished.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookImagePageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookSpotlightPageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public abstract class GuideEntry extends EntryProvider {

    private final String id;
    private final String name;
    private final String description;
    private final ItemLike icon;

    protected GuideEntry(CategoryProvider parent, String id, String name, String description, ItemLike icon) {
        super(parent);
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
    }

    protected void text(String pageId, String title, String... lines) {
        this.page(pageId, () -> BookTextPageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle(title);
        this.pageText(join(lines));
    }

    protected void spotlight(String pageId, ItemLike item, String title, String... lines) {
        this.page(pageId, () -> BookSpotlightPageModel.create()
                .withItem(Ingredient.of(item))
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText()));
        this.pageTitle(title);
        this.pageText(join(lines));
    }

    protected void image(String pageId, String texture, String title, String... lines) {
        this.page(pageId, () -> BookImagePageModel.create()
                .withTitle(this.context().pageTitle())
                .withText(this.context().pageText())
                .withImages(ResourceLocation.fromNamespaceAndPath("nourished", texture)));
        this.pageTitle(title);
        this.pageText(join(lines));
    }

    // Modonomicon glues blank-line paragraphs together, so every line ends in a hard break.
    protected static String join(String... lines) {
        return String.join("  \n", lines).replace("%", "%%") + "\n";
    }

    protected static String hl(String color, String text) {
        return "[#](" + color + ")**" + text + "**[#]()";
    }

    protected static String link(String text, String category, String entry) {
        return "[" + text + "](entry://" + category + "/" + entry + ")";
    }

    @Override
    protected String entryName() {
        return name;
    }

    @Override
    protected String entryDescription() {
        return description;
    }

    @Override
    protected Pair<Integer, Integer> entryBackground() {
        return EntryBackground.DEFAULT;
    }

    @Override
    protected BookIconModel entryIcon() {
        return BookIconModel.create(icon);
    }

    @Override
    protected String entryId() {
        return id;
    }
}
