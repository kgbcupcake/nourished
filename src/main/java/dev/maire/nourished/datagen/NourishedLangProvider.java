package dev.maire.nourished.datagen;

import com.klikli_dev.modonomicon.api.datagen.AbstractModonomiconLanguageProvider;
import com.klikli_dev.modonomicon.api.datagen.ModonomiconLanguageProvider;
import dev.maire.nourished.core.Nourished;
import net.minecraft.data.PackOutput;

/**
 * Flushes the guide book's lang cache (filled by the book/category/entry providers) to
 * {@code assets/nourished/lang/en_us.json}. Must be registered after the book provider in
 * {@link NourishedDataGenerators} so the cache is fully populated before this runs.
 */
public class NourishedLangProvider extends AbstractModonomiconLanguageProvider {

    public NourishedLangProvider(PackOutput output, ModonomiconLanguageProvider cache) {
        super(output, Nourished.MODID, "en_us", cache);
    }

    @Override
    protected void addTranslations() {
        // All book/category/entry text comes from the book providers via the cache.
        this.accept("item.nourished.nourished_book", "Nourished Guide");
    }
}
