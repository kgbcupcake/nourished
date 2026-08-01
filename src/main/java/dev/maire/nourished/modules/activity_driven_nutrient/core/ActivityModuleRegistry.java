package dev.maire.nourished.modules.activity_driven_nutrient.core;

import dev.marie.framework.api.value.ValueSourceTrigger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flat, static, process-wide registry — matches the singleton-facade shape every other registry in
 * this codebase uses (e.g. {@code ModuleRegistry}, {@code ScannerSpecRegistry}, {@code
 * ColorRegistry}): private constructor, static backing store, reachable from anywhere without a
 * caller holding a reference.
 *
 * <p>Dispatch's main lookup is "all modules for this trigger type", so modules are keyed by {@link
 * ValueSourceTrigger.TriggerType} rather than kept in one flat list filtered per lookup. A separate
 * flat list tracks overall registration order for {@link #getAll()} (e.g. config-screen listing),
 * since insertion order into a {@code Map<TriggerType, List<...>>} doesn't by itself give a single
 * global order across types.
 */
public final class ActivityModuleRegistry {

    private static final Map<ValueSourceTrigger.TriggerType, List<ActivityEffectModule>> BY_TRIGGER_TYPE = new LinkedHashMap<>();
    private static final List<ActivityEffectModule> ALL_MODULES = new ArrayList<>();

    private ActivityModuleRegistry() {}

    public static synchronized void register(ActivityEffectModule module) {
        BY_TRIGGER_TYPE.computeIfAbsent(module.triggerType(), t -> new ArrayList<>()).add(module);
        ALL_MODULES.add(module);
    }

    /** Every module registered for {@code type}, in registration order. Empty if none. */
    public static synchronized List<ActivityEffectModule> get(ValueSourceTrigger.TriggerType type) {
        return Collections.unmodifiableList(BY_TRIGGER_TYPE.getOrDefault(type, List.of()));
    }

    /** Every registered module, in registration order, regardless of trigger type. */
    public static synchronized List<ActivityEffectModule> getAll() {
        return Collections.unmodifiableList(ALL_MODULES);
    }
}
