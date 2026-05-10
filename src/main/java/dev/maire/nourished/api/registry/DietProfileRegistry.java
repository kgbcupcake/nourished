package dev.maire.nourished.api.registry;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.DietProfileDefinition;
import dev.maire.nourished.core.registry.AbstractRegistry;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Internal storage for diet profile definitions registered via the public API.
 */
@ApiStatus.Internal
public final class DietProfileRegistry {

    private static final class Core extends AbstractRegistry<String, DietProfileDefinition> {
        Core() {
            super("DietProfileRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private DietProfileRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        INSTANCE.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        INSTANCE.reset();
    }

    /**
     * Registers a diet profile definition.
     *
     * @param definition the diet profile to register
     * @throws IllegalStateException    if the registry is frozen or a profile with the same id already exists
     * @throws IllegalArgumentException if {@code definition} is null
     */
    public static void register(DietProfileDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        INSTANCE.register(definition.getId(), definition);
    }

    /**
     * Returns a registered diet profile by id, or {@code null} if not found.
     *
     * @param id the profile identifier
     * @return the profile definition, or {@code null}
     */
    @Nullable
    public static DietProfileDefinition get(String id) {
        return INSTANCE.get(id);
    }

    /**
     * Returns all registered diet profiles.
     *
     * @return an unmodifiable list of all profile definitions
     */
    public static List<DietProfileDefinition> getAll() {
        return INSTANCE.values();
    }
}
