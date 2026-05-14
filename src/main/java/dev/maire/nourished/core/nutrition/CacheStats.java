package dev.maire.nourished.core.nutrition;

/**
 * Snapshot of the runtime food resolver's cache counters.
 */
public record CacheStats(int hits, int misses, int size) {}
