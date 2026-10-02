package dev.maire.nourished.api.impl;

import dev.maire.nourished.api.impl.RegistrationBatch.Entry;
import dev.maire.nourished.api.impl.RegistrationBatch.FoodClassification;
import dev.maire.nourished.api.impl.RegistrationBatch.Kind;
import dev.marie.framework.api.progression.MilestoneDefinition;
import dev.marie.framework.api.value.ValueDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationValidatorTest {

    private static final Set<String> BUILT_IN = Set.of("proteins", "grains");

    private static Entry nutrient(String owner, String id) {
        return new Entry(owner, Kind.NUTRIENT, ValueDefinition.builder(id).displayName(id).build());
    }

    private static Entry food(String owner, String item, String key, float amount) {
        return new Entry(owner, Kind.FOOD,
                new FoodClassification(owner + ":" + item, key, amount));
    }

    @Test
    void foodCanReferenceNutrientRegisteredLaterByAnotherAddon() {
        List<Entry> entries = List.of(
                food("addon_a", "berry", "vitamin_x", 1f),
                nutrient("addon_b", "vitamin_x"));

        RegistrationValidator.Result result = RegistrationValidator.validate(entries, BUILT_IN);

        assertTrue(result.errors().isEmpty(), result.errors().toString());
        assertEquals(List.of(Kind.NUTRIENT, Kind.FOOD), result.accepted().stream().map(Entry::kind).toList());
    }

    @Test
    void appliesInDependencyOrderRegardlessOfSubmissionOrder() {
        List<Entry> entries = List.of(
                new Entry("a", Kind.MILESTONE, MilestoneDefinition.builder("m").valueKey("vitamin_x").build()),
                food("a", "berry", "vitamin_x", 1f),
                nutrient("b", "vitamin_x"));

        List<Kind> order = RegistrationValidator.validate(entries, BUILT_IN).accepted().stream().map(Entry::kind).toList();

        assertEquals(List.of(Kind.NUTRIENT, Kind.FOOD, Kind.MILESTONE), order);
    }

    @Test
    void builtInNutrientReferencesAreAccepted() {
        RegistrationValidator.Result result = RegistrationValidator.validate(
                List.of(food("a", "bread", "grains", 0.5f)), BUILT_IN);

        assertEquals(1, result.accepted().size());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void unknownNutrientReferenceIsRejectedWithOwner() {
        RegistrationValidator.Result result = RegistrationValidator.validate(
                List.of(food("addon_a", "berry", "missing", 1f)), BUILT_IN);

        assertTrue(result.accepted().isEmpty());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).startsWith("[addon_a]"));
        assertTrue(result.errors().get(0).contains("missing"));
    }

    @Test
    void duplicateOfBuiltInNutrientIsRejected() {
        RegistrationValidator.Result result = RegistrationValidator.validate(
                List.of(nutrient("a", "proteins")), BUILT_IN);

        assertTrue(result.accepted().isEmpty());
        assertEquals(1, result.errors().size());
    }

    @Test
    void duplicateNutrientAcrossAddonsKeepsFirst() {
        RegistrationValidator.Result result = RegistrationValidator.validate(
                List.of(nutrient("first", "vitamin_x"), nutrient("second", "vitamin_x")), BUILT_IN);

        assertEquals(1, result.accepted().size());
        assertEquals("first", result.accepted().get(0).owner());
        assertTrue(result.errors().get(0).startsWith("[second]"));
    }

    @Test
    void duplicateMilestoneIdInBatchIsRejected() {
        MilestoneDefinition m = MilestoneDefinition.builder("m").valueKey("grains").build();
        RegistrationValidator.Result result = RegistrationValidator.validate(
                List.of(new Entry("a", Kind.MILESTONE, m), new Entry("b", Kind.MILESTONE, m)), BUILT_IN);

        assertEquals(1, result.accepted().size());
        assertEquals(1, result.errors().size());
    }

    @Test
    void nonFiniteFoodAmountIsRejected() {
        RegistrationValidator.Result result = RegistrationValidator.validate(
                List.of(food("a", "berry", "grains", Float.NaN)), BUILT_IN);

        assertTrue(result.accepted().isEmpty());
    }
}
