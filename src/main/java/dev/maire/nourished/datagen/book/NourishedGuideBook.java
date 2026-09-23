package dev.maire.nourished.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.ModonomiconLanguageProvider;
import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookModel;
import dev.maire.nourished.datagen.book.gettingstarted.GettingStartedCategory;
import net.minecraft.resources.ResourceLocation;

public class NourishedGuideBook extends SingleBookSubProvider {

    public static final String ID = "nourished_guide";

    // Book colors
    private static final String BOOK_NAME = "[#](B88CFF)";
    private static final String BOOK_TOOLTIP = "[#](6FB8E8)";

    // Reset
    private static final String RESET = "[#]()";

    public NourishedGuideBook(String modId, ModonomiconLanguageProvider lang) {
        super(ID, modId, lang);
    }

    @Override
    protected BookModel additionalSetup(BookModel book) {
        // "node" display mode (Modonomicon's default) renders as a quest-map graph, Thaumonomicon-style.
        return book.withModel(
                ResourceLocation.parse("nourished:nourished_book")
        );
    }

    @Override
    protected void registerDefaultMacros() {
        // none yet
    }

    @Override
    protected void generateCategories() {
        this.add(new GettingStartedCategory(this).generate());
    }

    @Override
    protected String bookName() {
        return "%sNourished Guide%s".formatted(BOOK_NAME, RESET);
    }

    @Override
    protected String bookTooltip() {
        return "%sEverything you need to know about eating well.%s".formatted(BOOK_TOOLTIP, RESET);
    }
}