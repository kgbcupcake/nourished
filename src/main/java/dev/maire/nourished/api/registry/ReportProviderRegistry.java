package dev.maire.nourished.api.registry;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.DietReportProvider;
import dev.maire.nourished.registry.ListRegistry;

import java.util.List;

/**
 * Internal storage for diet report providers registered via the public API.
 */
@ApiStatus.Internal
public final class ReportProviderRegistry {

    private static final ListRegistry<DietReportProvider> REGISTRY = new ListRegistry<>("ReportProviderRegistry", null);

    private ReportProviderRegistry() {}

    @ApiStatus.Internal
    public static void freezeInternal() {
        REGISTRY.freeze();
    }

    /**
     * Registers a diet report provider.
     *
     * @param provider the report provider to register
     * @throws IllegalArgumentException if {@code provider} is null
     */
    public static void register(DietReportProvider provider) {
        REGISTRY.register(provider);
    }

    /**
     * Returns all registered report providers in registration order.
     *
     * @return an unmodifiable list of report providers
     */
    public static List<DietReportProvider> getAll() {
        return REGISTRY.values();
    }
}
