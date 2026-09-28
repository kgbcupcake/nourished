package dev.maire.nourished.api.impl;

import dev.marie.framework.api.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Startup-only collector for registrations made during {@code NourishedRegisterEvent}. */
@ApiStatus.Internal
public final class RegistrationBatch {

    // Declaration order is apply order: nutrients must land before anything that references them.
    public enum Kind { NUTRIENT, FOOD, COMPAT, EFFECT, NUTRIENT_SYNERGY, FOOD_SYNERGY, PROFILE, MILESTONE }

    public record Entry(String owner, Kind kind, Object definition) {}

    public record FoodClassification(String sourceId, String valueKey, float amount) {}

    private static volatile RegistrationBatch active;

    private final List<Entry> entries = new ArrayList<>();
    private String owner = "unknown";
    private boolean closed;

    public static synchronized RegistrationBatch open() {
        if (active != null) {
            throw new IllegalStateException("A Nourished registration batch is already open");
        }
        active = new RegistrationBatch();
        return active;
    }

    /** Routes a NourishedAPI call into the open batch; false when no event is running. */
    public static boolean stage(Kind kind, Object definition) {
        RegistrationBatch batch = active;
        if (batch == null) {
            return false;
        }
        batch.submit(kind, definition);
        return true;
    }

    public static boolean isCollecting() {
        return active != null;
    }

    public synchronized void setOwner(String owner) {
        this.owner = owner;
    }

    public synchronized void submit(Kind kind, Object definition) {
        if (closed) {
            throw new IllegalStateException("NourishedRegisterEvent has finished — "
                    + kind + " registrations must happen inside the event listener");
        }
        Objects.requireNonNull(definition, kind + " definition");
        entries.add(new Entry(owner, kind, definition));
    }

    public synchronized void close() {
        closed = true;
        if (active == this) {
            active = null;
        }
    }

    public synchronized List<Entry> entries() {
        return List.copyOf(entries);
    }
}
