package dev.maire.nourished.compat;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Root JSON structure for compat_registry.json files.
 */
public record CompatRegistry(
        @SerializedName("entries")
        List<JsonCompatEntry> entries
) {}
