package dev.maire.nourished.core.deficiency;

/** The optional calorie row: a pseudo-group under {@link #KEY} in the pressure map. */
public final class DeficiencyCalories {

    public static final String KEY = "calories";

    private DeficiencyCalories() {}

    /**
     * +1 builds (yesterday and today both short), -1 recovers (today or yesterday reached the
     * recover share), 0 holds. {@code yesterday} below zero means no history yet, which never builds.
     */
    public static int state(DeficiencySettings.CalorieRule rule, float yesterday, float today, float max) {
        if (!rule.enabled() || max <= 0f) return 0;
        float min = rule.minFraction() * max;
        float recover = rule.recoverFraction() * max;
        if (today >= recover || yesterday >= recover) return -1;
        if (yesterday >= 0f && yesterday < min && today < min) return 1;
        return 0;
    }
}
