package dev.maire.nourished.datagen;

import com.klikli_dev.modonomicon.api.datagen.LanguageProviderCache;
import com.klikli_dev.modonomicon.api.datagen.NeoBookProvider;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.datagen.book.NourishedGuideBook;
import net.minecraft.data.DataGenerator;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class NourishedDataGenerators {

    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();

        var enUsCache = new LanguageProviderCache("en_us");

        generator.addProvider(
                event.includeServer(),
                NeoBookProvider.of(
                        event,
                        new NourishedGuideBook(Nourished.MODID, enUsCache)
                )
        );

        // Must run after the book provider so the cache it wrote to is fully populated.
        generator.addProvider(
                event.includeClient(),
                new NourishedLangProvider(
                        generator.getPackOutput(),
                        enUsCache
                )
        );
    }
}