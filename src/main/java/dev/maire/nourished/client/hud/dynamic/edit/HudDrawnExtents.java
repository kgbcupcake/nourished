package dev.maire.nourished.client.hud.dynamic.edit;

import dev.marie.framework.ui.geometry.Bounds;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Where each Nutrient HUD row actually drew its icon, name and bar (bar plus percentage) on the last
 * frame, with every offset and size already applied. Edit mode's dashed move outlines (the panel-wide
 * Move Text/Icons/Bars/All modes and "Move Nutrient") wrap these recorded extents instead of
 * recomputing positions from the layout, so they always hug what's really on screen however the rows
 * have been moved or resized.
 */
public final class HudDrawnExtents {

    /** The movable parts of a row. */
    public enum Part { TEXT, ICON, BAR }

    private static final Map<String, EnumMap<Part, Bounds>> DRAWN = new HashMap<>();

    private HudDrawnExtents() {}

    /** Forgets last frame's extents; called once per panel render before the rows draw. */
    public static void beginFrame() {
        DRAWN.clear();
    }

    public static void record(String key, Part part, int x, int y, int width, int height) {
        DRAWN.computeIfAbsent(key, k -> new EnumMap<>(Part.class)).put(part, new Bounds(x, y, Math.max(1, width), Math.max(1, height)));
    }

    /** Everything {@code key}'s row drew, or {@code null} if it drew nothing. */
    public static Bounds row(String key) {
        EnumMap<Part, Bounds> parts = DRAWN.get(key);
        Bounds union = null;
        if (parts != null) {
            for (Bounds b : parts.values()) {
                union = union(union, b);
            }
        }
        return union;
    }

    /** {@code part} across every row, or {@code null} if no row drew one. */
    public static Bounds part(Part part) {
        Bounds union = null;
        for (EnumMap<Part, Bounds> parts : DRAWN.values()) {
            union = union(union, parts.get(part));
        }
        return union;
    }

    /** Every part of every row, or {@code null} if nothing was drawn. */
    public static Bounds all() {
        Bounds union = null;
        for (String key : DRAWN.keySet()) {
            union = union(union, row(key));
        }
        return union;
    }

    private static Bounds union(Bounds a, Bounds b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        int x = Math.min(a.x(), b.x());
        int y = Math.min(a.y(), b.y());
        int right = Math.max(a.x() + a.width(), b.x() + b.width());
        int bottom = Math.max(a.y() + a.height(), b.y() + b.height());
        return new Bounds(x, y, right - x, bottom - y);
    }
}
