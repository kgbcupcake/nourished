package dev.maire.nourished.core.nutrition.stages;

import dev.marie.MariesLib.scan.ResolutionResult;
import dev.marie.MariesLib.scan.RuntimeCascadeStage;
import dev.marie.MariesLib.scan.ResolutionStageHandler;
import dev.marie.MariesLib.scan.StageContext;
import dev.marie.MariesLib.scan.StageMath;
import dev.marie.MariesLib.cache.RunningAverage;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Stage 4: falls back to the running average of previously-resolved items
 * in the same namespace, requiring at least 5 prior data points and a
 * minimum spread of 2.0 to produce a result.
 */
public final class NamespacePeerStage implements ResolutionStageHandler {

    private static final int MIN_PEER_COUNT = 5;
    private static final float MIN_SPREAD = 2.0f;

    @Override
    @Nullable
    public ResolutionResult resolve(ResourceLocation itemId, StageContext ctx) {
        String ns = itemId.getNamespace();
        RunningAverage peerAvg = ctx.namespacePeers().get(ns);
        if (peerAvg == null || peerAvg.count() < MIN_PEER_COUNT) return null;

        Map<String, Float> avg = peerAvg.average();
        float spread = StageMath.computeSpread(avg);
        if (spread < MIN_SPREAD) return null;

        StageMath.NormalizationOutcome outcome = StageMath.normalizeWithRejections(avg, ctx.validKeys());
        if (outcome.normalized().isEmpty()) return null;

        return new ResolutionResult(
                outcome.normalized(), Map.copyOf(avg),
                List.of(), Map.of(), outcome.rejectedSignals(),
                false, spread, RuntimeCascadeStage.NAMESPACE_PEER,
                "namespace peer average (" + ns + ", n=" + peerAvg.count() + ")");
    }
}
