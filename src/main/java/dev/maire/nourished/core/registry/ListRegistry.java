package dev.maire.nourished.core.registry;

import dev.maire.nourished.api.ApiStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Ordered list registry with optional comparator applied at {@link #freeze()} time.
 */
@ApiStatus.Internal
public class ListRegistry<V> {

    private final String name;
    private final Comparator<? super V> freezeOrder;
    private final ArrayList<V> mutable = new ArrayList<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
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
     * Runs {@code action} while holding the write lock so reset → repopulate → freeze is atomic.
     */
    public final void runWrite(Runnable action) {
        lock.writeLock().lock();
        try {
            action.run();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Appends a value in registration order (sorting, if any, happens in {@link #freeze()}).
     *
     * @throws IllegalArgumentException if {@code value} is null
     * @throws IllegalStateException    if the registry is frozen
     */
    public final void register(V value) {
        lock.writeLock().lock();
        try {
            registerUnlocked(value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Same as {@link #values()} for list-shaped registries.
     */
    public final List<V> values() {
        lock.readLock().lock();
        try {
            return valuesUnlocked();
        } finally {
            lock.readLock().unlock();
        }
    }

    public final int size() {
        lock.readLock().lock();
        try {
            return sizeUnlocked();
        } finally {
            lock.readLock().unlock();
        }
    }

    public final void freeze() {
        lock.writeLock().lock();
        try {
            freezeUnlocked();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public final void reset() {
        lock.writeLock().lock();
        try {
            resetUnlocked();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public final boolean isFrozen() {
        lock.readLock().lock();
        try {
            return frozen;
        } finally {
            lock.readLock().unlock();
        }
    }

    public final RegistrySnapshot createSnapshot() {
        lock.readLock().lock();
        try {
            return new RegistrySnapshot(name, sizeUnlocked(), freezeTime, duplicateAttemptCount);
        } finally {
            lock.readLock().unlock();
        }
    }

    public final void registerUnlocked(V value) {
        if (frozen) {
            throw new IllegalStateException(name + ": cannot register while frozen");
        }
        if (value == null) {
            throw new IllegalArgumentException(name + ": value cannot be null");
        }
        validateEntry(value);
        mutable.add(value);
    }

    public final void resetUnlocked() {
        mutable.clear();
        duplicateAttemptCount = 0;
        frozen = false;
        frozenList = List.of();
        freezeTime = Instant.EPOCH;
        onReset();
    }

    public final void freezeUnlocked() {
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

    public final List<V> valuesUnlocked() {
        if (frozen) {
            return frozenList;
        }
        return Collections.unmodifiableList(new ArrayList<>(mutable));
    }

    private int sizeUnlocked() {
        if (frozen) {
            return frozenList.size();
        }
        return mutable.size();
    }

    protected void validateEntry(V value) {}

    protected void onFreeze() {}

    protected void onReset() {}
}
