package dev.maire.nourished.core.registry;

import dev.maire.nourished.api.ApiStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Keyed registry with a mutable registration phase and an immutable frozen runtime phase.
 *
 * <p>After {@link #freeze()}, read paths use only immutable snapshots and do not allocate.
 * Mutation attempts throw {@link IllegalStateException} including the registry {@link #name()}
 * in the message.</p>
 */
@ApiStatus.Internal
public abstract class AbstractRegistry<K, V> {

    private final String name;
    private final LinkedHashMap<K, V> mutable = new LinkedHashMap<>();
    private int duplicateAttemptCount;

    private volatile boolean frozen;
    private Map<K, V> frozenEntries = Map.of();
    private List<K> frozenKeys = List.of();
    private List<V> frozenValues = List.of();
    private Instant freezeTime = Instant.EPOCH;

    protected AbstractRegistry(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public final String name() {
        return name;
    }

    /**
     * Registers a key-value pair in insertion order. Not valid after {@link #freeze()}.
     *
     * @throws IllegalArgumentException if {@code key} or {@code value} is null
     * @throws IllegalStateException    if the registry is frozen or the key is already present
     */
    public final void register(K key, V value) {
        if (frozen) {
            throw new IllegalStateException(name + ": cannot register while frozen");
        }
        if (key == null) {
            throw new IllegalArgumentException(name + ": key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException(name + ": value cannot be null");
        }
        validateEntry(key, value);
        if (mutable.containsKey(key)) {
            noteDuplicateAttempt();
            throw new IllegalStateException(name + ": duplicate key: " + key);
        }
        mutable.put(key, value);
    }

    public final V get(K key) {
        if (key == null) {
            return null;
        }
        if (frozen) {
            return frozenEntries.get(key);
        }
        return mutable.get(key);
    }

    public final boolean contains(K key) {
        if (key == null) {
            return false;
        }
        if (frozen) {
            return frozenEntries.containsKey(key);
        }
        return mutable.containsKey(key);
    }

    public final List<V> values() {
        if (frozen) {
            return frozenValues;
        }
        return Collections.unmodifiableList(new ArrayList<>(mutable.values()));
    }

    public final List<K> keys() {
        if (frozen) {
            return frozenKeys;
        }
        return Collections.unmodifiableList(new ArrayList<>(mutable.keySet()));
    }

    public final Map<K, V> entries() {
        if (frozen) {
            return frozenEntries;
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(mutable));
    }

    public final int size() {
        if (frozen) {
            return frozenEntries.size();
        }
        return mutable.size();
    }

    /**
     * Builds immutable snapshots, locks the registry, and invokes {@link #onFreeze()}.
     * Idempotent if already frozen.
     */
    public final void freeze() {
        if (frozen) {
            return;
        }
        Map<K, V> snap = Collections.unmodifiableMap(new LinkedHashMap<>(mutable));
        List<K> keysSnap = List.copyOf(snap.keySet());
        List<V> valuesSnap = List.copyOf(snap.values());
        Instant now = Instant.now();
        this.frozenEntries = snap;
        this.frozenKeys = keysSnap;
        this.frozenValues = valuesSnap;
        this.freezeTime = now;
        this.frozen = true;
        onFreeze();
    }

    /**
     * Clears all state, unlocks the registry, and invokes {@link #onReset()}.
     */
    public final void reset() {
        mutable.clear();
        duplicateAttemptCount = 0;
        frozen = false;
        frozenEntries = Map.of();
        frozenKeys = List.of();
        frozenValues = List.of();
        freezeTime = Instant.EPOCH;
        onReset();
    }

    public final boolean isFrozen() {
        return frozen;
    }

    public final RegistrySnapshot createSnapshot() {
        return new RegistrySnapshot(name, size(), freezeTime, duplicateAttemptCount);
    }

    protected void validateEntry(K key, V value) {}

    protected void onFreeze() {}

    protected void onReset() {}

    protected final void noteDuplicateAttempt() {
        duplicateAttemptCount++;
    }
}
