package dev.maire.nourished.api.registry;

import dev.maire.nourished.api.DietReportProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal storage for diet report providers registered via the public API.
 */
public final class ReportProviderRegistry {

    private static final List<DietReportProvider> PROVIDERS = new ArrayList<>();

    private ReportProviderRegistry() {}

    /**
     * Registers a diet report provider.
     *
     * @param provider the report provider to register
     */
    public static void register(DietReportProvider provider) {
        PROVIDERS.add(provider);
    }

    /**
     * Returns all registered report providers in registration order.
     *
     * @return an unmodifiable list of report providers
     */
    public static List<DietReportProvider> getAll() {
        return Collections.unmodifiableList(PROVIDERS);
    }
}
