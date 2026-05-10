package dev.maire.nourished.core.registry;

import dev.maire.nourished.api.ApiStatus;

import java.time.Instant;

/**
 * Point-in-time diagnostic view of a registry after {@link AbstractRegistry#freeze()}
 * or {@link ListRegistry#freeze()}.
 */
@ApiStatus.Internal
public record RegistrySnapshot(
        String name,
        int size,
        Instant freezeTime,
        int duplicateAttemptCount
) {}
