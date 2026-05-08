package dev.maire.nourished.example;

import dev.maire.nourished.api.CompatDefinition;
import dev.maire.nourished.api.NourishedAPI;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates compatibility entry registration so Nourished can identify addon category metadata.
 */
public final class ExampleCompatEntry {

    private ExampleCompatEntry() {}

    public static void register() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("modId", NourishedExampleAddon.MOD_ID);
        fields.put("category", CompatDefinition.CompatCategory.FOOD_MOD);
        fields.put("foodTagMappings", new HashMap<>());

        CompatDefinition compat = ExampleApiFactory.instantiate(
                CompatDefinition.class,
                "dev.maire.nourished.api.CompatDefinition$Builder",
                new Class<?>[]{String.class},
                new Object[]{NourishedExampleAddon.MOD_ID},
                fields
        );

        NourishedAPI.registerCompatEntry(compat);
    }
}
