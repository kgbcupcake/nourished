package dev.maire.nourished.tooling.classification;

import dev.maire.nourished.core.nutrition.ResolutionStage;
import dev.maire.nourished.core.nutrition.RuntimeCascadeStage;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable trace of classification decisions for a single food item.
 * Records the ordered decision steps and final outcome.
 */
public final class ClassificationTrace {

    private final String itemId;
    private final ClassificationPipeline pipeline;
    private final List<ClassificationTraceStep> steps;
    private final Map<String, Float> finalBars;
    @Nullable private final String dominant;
    @Nullable private final ResolutionStage resolutionStage;
    @Nullable private final RuntimeCascadeStage cascadeStage;
    private final boolean uncertain;
    private final boolean tagClassified;
    private final String summaryReason;

    private ClassificationTrace(Builder builder) {
        this.itemId = builder.itemId;
        this.pipeline = builder.pipeline;
        this.steps = List.copyOf(builder.steps);
        this.finalBars = builder.finalBars.isEmpty() ? Map.of() : Map.copyOf(builder.finalBars);
        this.dominant = builder.dominant;
        this.resolutionStage = builder.resolutionStage;
        this.cascadeStage = builder.cascadeStage;
        this.uncertain = builder.uncertain;
        this.tagClassified = builder.tagClassified;
        this.summaryReason = builder.summaryReason;
    }

    public String itemId() {
        return itemId;
    }

    public ClassificationPipeline pipeline() {
        return pipeline;
    }

    public List<ClassificationTraceStep> steps() {
        return steps;
    }

    public Map<String, Float> finalBars() {
        return finalBars;
    }

    @Nullable
    public String dominant() {
        return dominant;
    }

    @Nullable
    public ResolutionStage resolutionStage() {
        return resolutionStage;
    }

    @Nullable
    public RuntimeCascadeStage cascadeStage() {
        return cascadeStage;
    }

    public boolean uncertain() {
        return uncertain;
    }

    public boolean tagClassified() {
        return tagClassified;
    }

    public String summaryReason() {
        return summaryReason;
    }

    public String format(ItemStack stack) {
        return ClassificationTraceFormatter.format(this, stack);
    }

    public static Builder builder(String itemId, ClassificationPipeline pipeline) {
        return new Builder(itemId, pipeline);
    }

    public static final class Builder {
        private final String itemId;
        private final ClassificationPipeline pipeline;
        private final List<ClassificationTraceStep> steps = new ArrayList<>();
        private Map<String, Float> finalBars = Map.of();
        @Nullable private String dominant;
        @Nullable private ResolutionStage resolutionStage;
        @Nullable private RuntimeCascadeStage cascadeStage;
        private boolean uncertain;
        private boolean tagClassified;
        private String summaryReason = "";

        private Builder(String itemId, ClassificationPipeline pipeline) {
            this.itemId = itemId;
            this.pipeline = pipeline;
        }

        public Builder addStep(ClassificationTraceStep step) {
            this.steps.add(step);
            return this;
        }

        public Builder addStep(TraceStepId id, TraceStepStatus status, String message) {
            return addStep(new ClassificationTraceStep(id, status, message));
        }

        public Builder addStep(TraceStepId id, TraceStepStatus status, String message, @Nullable Map<String, Object> detail) {
            return addStep(new ClassificationTraceStep(id, status, message, detail));
        }

        public Builder addSteps(List<ClassificationTraceStep> steps) {
            this.steps.addAll(steps);
            return this;
        }

        public Builder finalBars(Map<String, Float> finalBars) {
            this.finalBars = new LinkedHashMap<>(finalBars);
            return this;
        }

        public Builder dominant(@Nullable String dominant) {
            this.dominant = dominant;
            return this;
        }

        public Builder resolutionStage(@Nullable ResolutionStage stage) {
            this.resolutionStage = stage;
            return this;
        }

        public Builder cascadeStage(@Nullable RuntimeCascadeStage stage) {
            this.cascadeStage = stage;
            return this;
        }

        public Builder uncertain(boolean uncertain) {
            this.uncertain = uncertain;
            return this;
        }

        public Builder tagClassified(boolean tagClassified) {
            this.tagClassified = tagClassified;
            return this;
        }

        public Builder summaryReason(String reason) {
            this.summaryReason = reason != null ? reason : "";
            return this;
        }

        public ClassificationTrace build() {
            return new ClassificationTrace(this);
        }
    }
}
