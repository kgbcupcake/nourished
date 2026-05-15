package dev.maire.nourished.core.nutrition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TagRuntimeBlend} covering tag precedence, blend protection,
 * and empty fallback protection.
 */
class TagRuntimeBlendTest {

    @Nested
    @DisplayName("Tag Precedence Tests")
    class TagPrecedenceTests {

        @Test
        @DisplayName("Tag-derived nutrients always override resolver nutrients for same key")
        void tagOverridesResolverForSameKey() {
            Map<String, Float> tags = Map.of("proteins", 1.0f);
            Map<String, Float> resolver = Map.of("proteins", 0.8f, "grains", 0.5f);

            TagRuntimeBlend.BlendOutcome outcome = TagRuntimeBlend.blend(tags, resolver);

            assertEquals(TagRuntimeBlend.Precedence.TAG, outcome.perKeyPrecedence().get("proteins"),
                    "proteins should have TAG precedence");
            assertTrue(outcome.discardedResolver().containsKey("proteins"),
                    "resolver proteins value should be discarded");
            assertEquals(0.8f, outcome.discardedResolver().get("proteins"), 0.001f,
                    "discarded value should match resolver input");
        }

        @Test
        @DisplayName("Resolver-only nutrients are marked as RUNTIME_SUPPLEMENT")
        void resolverOnlyMarkedAsSupplement() {
            Map<String, Float> tags = Map.of("proteins", 1.0f);
            Map<String, Float> resolver = Map.of("grains", 0.6f);

            TagRuntimeBlend.BlendOutcome outcome = TagRuntimeBlend.blend(tags, resolver);

            assertEquals(TagRuntimeBlend.Precedence.RUNTIME_SUPPLEMENT, outcome.perKeyPrecedence().get("grains"),
                    "grains should have RUNTIME_SUPPLEMENT precedence");
            assertFalse(outcome.discardedResolver().containsKey("grains"),
                    "grains should not be discarded");
        }

        @Test
        @DisplayName("Multiple tag keys all have TAG precedence")
        void multipleTagKeysHaveTagPrecedence() {
            Map<String, Float> tags = Map.of("proteins", 1.0f, "vegetables", 1.0f);
            Map<String, Float> resolver = Map.of("proteins", 0.5f, "vegetables", 0.5f, "grains", 0.5f);

            TagRuntimeBlend.BlendOutcome outcome = TagRuntimeBlend.blend(tags, resolver);

            assertEquals(TagRuntimeBlend.Precedence.TAG, outcome.perKeyPrecedence().get("proteins"));
            assertEquals(TagRuntimeBlend.Precedence.TAG, outcome.perKeyPrecedence().get("vegetables"));
            assertEquals(TagRuntimeBlend.Precedence.RUNTIME_SUPPLEMENT, outcome.perKeyPrecedence().get("grains"));
        }
    }

    @Nested
    @DisplayName("Blend Protection Tests")
    class BlendProtectionTests {

        @Test
        @DisplayName("Resolver never overwrites tag keys in result")
        void resolverNeverOverwritesTagKeys() {
            Map<String, Float> tags = Map.of("proteins", 1.0f);
            Map<String, Float> resolver = Map.of("proteins", 99.0f);

            TagRuntimeBlend.BlendOutcome outcome = TagRuntimeBlend.blend(tags, resolver);

            assertTrue(outcome.result().containsKey("proteins"),
                    "proteins should be in result");
            assertTrue(outcome.discardedResolver().containsKey("proteins"),
                    "resolver proteins should be discarded, not merged");
        }

        @Test
        @DisplayName("Result contains all tag keys after blend")
        void resultContainsAllTagKeys() {
            Map<String, Float> tags = Map.of("proteins", 0.5f, "grains", 0.3f, "vegetables", 0.2f);
            Map<String, Float> resolver = Map.of("fruits", 1.0f);

            TagRuntimeBlend.BlendOutcome outcome = TagRuntimeBlend.blend(tags, resolver);

            assertTrue(outcome.result().containsKey("proteins"));
            assertTrue(outcome.result().containsKey("grains"));
            assertTrue(outcome.result().containsKey("vegetables"));
            assertTrue(outcome.result().containsKey("fruits"));
        }

        @Test
        @DisplayName("Resolver supplements are added at 0.5x weight before normalization")
        void resolverSupplementsAtHalfWeight() {
            Map<String, Float> tags = Map.of("proteins", 1.0f);
            Map<String, Float> resolver = Map.of("grains", 1.0f);

            TagRuntimeBlend.BlendOutcome outcome = TagRuntimeBlend.blend(tags, resolver);

            float proteinsWeight = outcome.result().get("proteins");
            float grainsWeight = outcome.result().get("grains");
            assertTrue(proteinsWeight > grainsWeight,
                    "Tag-derived (full weight) should be greater than resolver (0.5x weight) after normalization");
        }
    }

    @Nested
    @DisplayName("Empty Fallback Protection Tests")
    class EmptyFallbackProtectionTests {

        @Test
        @DisplayName("Non-empty tags produce non-empty result")
        void nonEmptyTagsProduceNonEmptyResult() {
            Map<String, Float> tags = Map.of("proteins", 1.0f);
            Map<String, Float> resolver = Map.of();

            TagRuntimeBlend.BlendOutcome outcome = TagRuntimeBlend.blend(tags, resolver);

            assertFalse(outcome.result().isEmpty(),
                    "Result should not be empty when tags are non-empty");
        }

        @Test
        @DisplayName("Non-empty resolver produces non-empty result when tags empty")
        void nonEmptyResolverWithEmptyTagsProducesNonEmptyResult() {
            Map<String, Float> tags = Map.of();
            Map<String, Float> resolver = Map.of("grains", 1.0f);

            TagRuntimeBlend.BlendOutcome outcome = TagRuntimeBlend.blend(tags, resolver);

            assertFalse(outcome.result().isEmpty(),
                    "Result should not be empty when resolver is non-empty");
        }

        @Test
        @DisplayName("Both non-empty produce non-empty result")
        void bothNonEmptyProduceNonEmptyResult() {
            Map<String, Float> tags = Map.of("proteins", 0.5f);
            Map<String, Float> resolver = Map.of("grains", 0.5f);

            TagRuntimeBlend.BlendOutcome outcome = TagRuntimeBlend.blend(tags, resolver);

            assertFalse(outcome.result().isEmpty(),
                    "Result should not be empty when both inputs are non-empty");
        }

        @Test
        @DisplayName("Both empty produce empty result")
        void bothEmptyProduceEmptyResult() {
            Map<String, Float> tags = Map.of();
            Map<String, Float> resolver = Map.of();

            TagRuntimeBlend.BlendOutcome outcome = TagRuntimeBlend.blend(tags, resolver);

            assertTrue(outcome.result().isEmpty(),
                    "Result should be empty when both inputs are empty");
        }

        @Test
        @DisplayName("Zero-weight tags fallback to tags map")
        void zeroWeightTagsFallbackToTagsMap() {
            Map<String, Float> tags = Map.of("proteins", 0.0f);
            Map<String, Float> resolver = Map.of();

            TagRuntimeBlend.BlendOutcome outcome = TagRuntimeBlend.blend(tags, resolver);

            assertEquals(tags, outcome.result(),
                    "When total weight is zero, should return original tags map");
        }
    }

    @Nested
    @DisplayName("Normalization Tests")
    class NormalizationTests {

        @Test
        @DisplayName("Result weights sum to 1.0 after normalization")
        void resultWeightsSumToOne() {
            Map<String, Float> tags = Map.of("proteins", 0.5f, "vegetables", 0.3f);
            Map<String, Float> resolver = Map.of("grains", 0.4f);

            TagRuntimeBlend.BlendOutcome outcome = TagRuntimeBlend.blend(tags, resolver);

            float sum = outcome.result().values().stream().reduce(0f, Float::sum);
            assertEquals(1.0f, sum, 0.001f,
                    "Normalized weights should sum to 1.0");
        }

        @Test
        @DisplayName("Relative proportions preserved for tag-only inputs")
        void relativeProportionsPreservedForTagsOnly() {
            Map<String, Float> tags = Map.of("proteins", 2.0f, "vegetables", 1.0f);
            Map<String, Float> resolver = Map.of();

            TagRuntimeBlend.BlendOutcome outcome = TagRuntimeBlend.blend(tags, resolver);

            float proteinsWeight = outcome.result().get("proteins");
            float vegetablesWeight = outcome.result().get("vegetables");
            assertEquals(2.0f, proteinsWeight / vegetablesWeight, 0.001f,
                    "Relative proportions should be preserved: proteins should be 2x vegetables");
        }
    }
}
