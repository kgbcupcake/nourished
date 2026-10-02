package dev.maire.nourished.api.impl;

import dev.maire.nourished.api.impl.RegistrationBatch.Entry;
import dev.maire.nourished.api.impl.RegistrationBatch.FoodClassification;
import dev.maire.nourished.api.impl.RegistrationBatch.Kind;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.effects.SynergyDefinition;
import dev.marie.framework.api.effects.ThresholdEffect;
import dev.marie.framework.api.progression.MilestoneDefinition;
import dev.marie.framework.api.progression.ProfileDefinition;
import dev.marie.framework.api.source.SourcePairSynergy;
import dev.marie.framework.api.value.ValueDefinition;
import dev.marie.framework.compat.CompatDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Checks a collected batch for duplicates and unknown nutrient references, without touching any registry. */
@ApiStatus.Internal
public final class RegistrationValidator {

    public record Result(List<Entry> accepted, List<String> errors) {}

    private RegistrationValidator() {}

    public static Result validate(List<Entry> entries, Collection<String> existingNutrients) {
        Set<String> known = new HashSet<>(existingNutrients);
        List<Entry> accepted = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            Set<String> seenIds = new HashSet<>();
            for (Entry entry : entries) {
                if (entry.kind() != kind) {
                    continue;
                }
                String problem = check(entry, known, seenIds);
                if (problem != null) {
                    errors.add("[" + entry.owner() + "] " + describe(entry) + ": " + problem);
                    continue;
                }
                accepted.add(entry);
                if (kind == Kind.NUTRIENT) {
                    known.add(((ValueDefinition) entry.definition()).getId());
                }
            }
        }
        return new Result(List.copyOf(accepted), List.copyOf(errors));
    }

    private static String check(Entry entry, Set<String> known, Set<String> seenIds) {
        return switch (entry.kind()) {
            case NUTRIENT -> {
                String id = ((ValueDefinition) entry.definition()).getId();
                if (id == null || id.isBlank()) yield "nutrient has no id";
                yield known.contains(id) ? "nutrient '" + id + "' is already registered" : null;
            }
            case FOOD -> {
                FoodClassification food = (FoodClassification) entry.definition();
                if (food.sourceId() == null || food.sourceId().isBlank()) yield "missing item id";
                if (!Float.isFinite(food.amount())) yield "amount must be finite";
                yield unknown(known, List.of(nullSafe(food.valueKey())));
            }
            case COMPAT -> unknown(known, ((CompatDefinition) entry.definition()).getSourceMappings().values());
            case EFFECT -> unknown(known, List.of(nullSafe(((ThresholdEffect) entry.definition()).getValueKey())));
            case NUTRIENT_SYNERGY -> {
                SynergyDefinition def = (SynergyDefinition) entry.definition();
                String dup = duplicate(seenIds, def.getId());
                yield dup != null ? dup : unknown(known, List.of(nullSafe(def.getValueKeyA()), nullSafe(def.getValueKeyB())));
            }
            case FOOD_SYNERGY -> {
                SourcePairSynergy def = (SourcePairSynergy) entry.definition();
                String dup = duplicate(seenIds, def.getId());
                if (dup != null) yield dup;
                yield def.getBonusValueKey() == null ? null : unknown(known, List.of(def.getBonusValueKey()));
            }
            case PROFILE -> {
                ProfileDefinition def = (ProfileDefinition) entry.definition();
                String dup = duplicate(seenIds, def.getId());
                if (dup != null) yield dup;
                Set<String> refs = new LinkedHashSet<>(def.getCustomThresholds().keySet());
                refs.addAll(def.getCustomDecayRates().keySet());
                yield unknown(known, refs);
            }
            case MILESTONE -> {
                MilestoneDefinition def = (MilestoneDefinition) entry.definition();
                String dup = duplicate(seenIds, def.getId());
                yield dup != null ? dup : unknown(known, List.of(nullSafe(def.getValueKey())));
            }
        };
    }

    private static String duplicate(Set<String> seenIds, String id) {
        return seenIds.add(id) ? null : "id '" + id + "' was registered more than once in this batch";
    }

    private static String unknown(Set<String> known, Collection<String> refs) {
        List<String> missing = refs.stream().filter(k -> !known.contains(k)).distinct().toList();
        if (missing.isEmpty()) {
            return null;
        }
        return "unknown nutrient" + (missing.size() > 1 ? "s " : " ")
                + missing.stream().map(k -> "'" + k + "'").collect(Collectors.joining(", "));
    }

    private static String nullSafe(String key) {
        return key == null ? "<null>" : key;
    }

    static String describe(Entry entry) {
        Object def = entry.definition();
        return switch (entry.kind()) {
            case NUTRIENT -> "nutrient " + ((ValueDefinition) def).getId();
            case FOOD -> "food " + ((FoodClassification) def).sourceId() + " -> " + ((FoodClassification) def).valueKey();
            case COMPAT -> "compat " + ((CompatDefinition) def).getModId();
            case EFFECT -> "effect " + ((ThresholdEffect) def).getEffectId() + " on " + ((ThresholdEffect) def).getValueKey();
            case NUTRIENT_SYNERGY -> "nutrient synergy " + ((SynergyDefinition) def).getId();
            case FOOD_SYNERGY -> "food synergy " + ((SourcePairSynergy) def).getId();
            case PROFILE -> "profile " + ((ProfileDefinition) def).getId();
            case MILESTONE -> "milestone " + ((MilestoneDefinition) def).getId();
        };
    }
}
