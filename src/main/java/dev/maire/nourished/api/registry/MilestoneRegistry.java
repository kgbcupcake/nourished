package dev.maire.nourished.api.registry;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.NutrientMilestoneDefinition;
import dev.maire.nourished.registry.AbstractRegistry;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Internal storage for nutrient milestone definitions registered via the public API.
 */
@ApiStatus.Internal
public final class MilestoneRegistry {

    private static final class Core extends AbstractRegistry<String, NutrientMilestoneDefinition> {
        Core() {
            super("MilestoneRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private MilestoneRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        INSTANCE.freeze();
    }

    @ApiStatus.Internal
    public static void resetInternal() {
        INSTANCE.reset();
    }

    /**
     * Registers a nutrient milestone definition.
     *
     * @param definition the milestone to register
     * @throws IllegalStateException    if the registry is frozen or a milestone with the same id already exists
     * @throws IllegalArgumentException if {@code definition} is null
     */
    public static void register(NutrientMilestoneDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        INSTANCE.register(definition.getId(), definition);
    }

    /**
     * Returns a registered milestone by id, or {@code null} if not found.
     *
     * @param id the milestone identifier
     * @return the milestone definition, or {@code null}
     */
    @Nullable
    public static NutrientMilestoneDefinition get(String id) {
        return INSTANCE.get(id);
    }

    /**
     * Returns all registered milestones.
     *
     * @return an unmodifiable list of all milestone definitions
     */
    public static List<NutrientMilestoneDefinition> getAll() {
        return INSTANCE.values();
    }

    /**
     * Returns all milestones that track a specific nutrient.
     *
     * @param nutrientKey the nutrient key to filter by
     * @return an unmodifiable list of matching milestones
     */
    public static List<NutrientMilestoneDefinition> getForNutrient(String nutrientKey) {
        return INSTANCE.values().stream()
                .filter(m -> nutrientKey.equals(m.getNutrientKey()))
                .toList();
    }
}
