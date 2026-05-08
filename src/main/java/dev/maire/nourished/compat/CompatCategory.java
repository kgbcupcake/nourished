package dev.maire.nourished.compat;

import com.google.gson.annotations.SerializedName;
import dev.maire.nourished.api.ApiStatus;

@ApiStatus.Internal
public enum CompatCategory {
    @SerializedName("survival_overhaul")
    SURVIVAL_OVERHAUL,

    @SerializedName("food_mod")
    FOOD_MOD,

    @SerializedName("farming_mod")
    FARMING_MOD,

    @SerializedName("magic_mod")
    MAGIC_MOD,

    @SerializedName("unknown")
    UNKNOWN
}
