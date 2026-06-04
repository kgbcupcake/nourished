package dev.maire.nourished.tooling.classification;

/**
 * Identifies which pipeline produced a classification trace.
 */
public enum ClassificationPipeline {
    RUNTIME,
    SCANNER,
    HELD_DEBUG
}
