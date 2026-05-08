package dev.maire.nourished.api.registry;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.DietProfileDefinition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal storage for diet profile definitions registered via the public API.
 */
@ApiStatus.Internal
public final class DietProfileRegistry {

    private static final Map<String, DietProfileDefinition> PROFILES = new LinkedHashMap<>();

    private DietProfileRegistry() {}

    /**
     * Registers a diet profile definition.
     *
     * @param definition the diet profile to register
     * @throws IllegalArgumentException if a profile with the same id already exists
     */
    public static void register(DietProfileDefinition definition) {
        if (PROFILES.containsKey(definition.getId())) {
            throw new IllegalArgumentException("Diet profile already registered: " + definition.getId());
        }
        PROFILES.put(definition.getId(), definition);
    }

    /**
     * Returns a registered diet profile by id, or {@code null} if not found.
     *
     * @param id the profile identifier
     * @return the profile definition, or {@code null}
     */
    @Nullable
    public static DietProfileDefinition get(String id) {
        return PROFILES.get(id);
    }

    /**
     * Returns all registered diet profiles.
     *
     * @return an unmodifiable list of all profile definitions
     */
    public static List<DietProfileDefinition> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(PROFILES.values()));
    }
}
