package dev.maire.nourished.compat;

import com.google.gson.annotations.SerializedName;
import dev.maire.nourished.api.ApiStatus;

import java.util.List;

/**
 * Root JSON structure for compat_registry.json files.
 */
@ApiStatus.Internal
public record CompatRegistry(
        @SerializedName("entries")
        List<JsonCompatEntry> entries
) {}
