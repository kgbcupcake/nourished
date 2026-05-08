package dev.maire.nourished.api.registry;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.NutrientMilestoneDefinition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal storage for nutrient milestone definitions registered via the public API.
 */
@ApiStatus.Internal
public final class MilestoneRegistry {

    private static final Map<String, NutrientMilestoneDefinition> MILESTONES = new LinkedHashMap<>();

    private MilestoneRegistry() {}

    /**
     * Registers a nutrient milestone definition.
     *
     * @param definition the milestone to register
     * @throws IllegalArgumentException if a milestone with the same id already exists
     */
    public static void register(NutrientMilestoneDefinition definition) {
        if (MILESTONES.containsKey(definition.getId())) {
            throw new IllegalArgumentException("Milestone already registered: " + definition.getId());
        }
        MILESTONES.put(definition.getId(), definition);
    }

    /**
     * Returns a registered milestone by id, or {@code null} if not found.
     *
     * @param id the milestone identifier
     * @return the milestone definition, or {@code null}
     */
    @Nullable
    public static NutrientMilestoneDefinition get(String id) {
        return MILESTONES.get(id);
    }

    /**
     * Returns all registered milestones.
     *
     * @return an unmodifiable list of all milestone definitions
     */
    public static List<NutrientMilestoneDefinition> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(MILESTONES.values()));
    }

    /**
     * Returns all milestones that track a specific nutrient.
     *
     * @param nutrientKey the nutrient key to filter by
     * @return an unmodifiable list of matching milestones
     */
    public static List<NutrientMilestoneDefinition> getForNutrient(String nutrientKey) {
        return MILESTONES.values().stream()
                .filter(m -> nutrientKey.equals(m.getNutrientKey()))
                .toList();
    }
}
