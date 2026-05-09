package dev.maire.nourished.registry;

import dev.maire.nourished.api.ApiStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Ordered list registry with optional comparator applied at {@link #freeze()} time.
 */
@ApiStatus.Internal
public class ListRegistry<V> {

    private final String name;
    private final Comparator<? super V> freezeOrder;
    private final ArrayList<V> mutable = new ArrayList<>();
    private int duplicateAttemptCount;

    private volatile boolean frozen;
    private List<V> frozenList = List.of();
    private Instant freezeTime = Instant.EPOCH;

    public ListRegistry(String name, Comparator<? super V> freezeOrder) {
        this.name = Objects.requireNonNull(name, "name");
        this.freezeOrder = freezeOrder;
    }

    public final String name() {
        return name;
    }

    /**
     * Appends a value in registration order (sorting, if any, happens in {@link #freeze()}).
     *
     * @throws IllegalArgumentException if {@code value} is null
     * @throws IllegalStateException    if the registry is frozen
     */
    public final void register(V value) {
        if (frozen) {
            throw new IllegalStateException(name + ": cannot register while frozen");
        }
        if (value == null) {
            throw new IllegalArgumentException(name + ": value cannot be null");
        }
        validateEntry(value);
        mutable.add(value);
    }

    /**
     * Same as {@link #values()} for list-shaped registries.
     */
    public final List<V> values() {
        if (frozen) {
            return frozenList;
        }
        return Collections.unmodifiableList(new ArrayList<>(mutable));
    }

    public final int size() {
        if (frozen) {
            return frozenList.size();
        }
        return mutable.size();
    }

    public final void freeze() {
        if (frozen) {
            return;
        }
        ArrayList<V> copy = new ArrayList<>(mutable);
        if (freezeOrder != null) {
            copy.sort(freezeOrder);
        }
        List<V> snap = List.copyOf(copy);
        Instant now = Instant.now();
        this.frozenList = snap;
        this.freezeTime = now;
        this.frozen = true;
        onFreeze();
    }

    public final void reset() {
        mutable.clear();
        duplicateAttemptCount = 0;
        frozen = false;
        frozenList = List.of();
        freezeTime = Instant.EPOCH;
        onReset();
    }

    public final boolean isFrozen() {
        return frozen;
    }

    public final RegistrySnapshot createSnapshot() {
        return new RegistrySnapshot(name, size(), freezeTime, duplicateAttemptCount);
    }

    protected void validateEntry(V value) {}

    protected void onFreeze() {}

    protected void onReset() {}
}
