package dev.maire.nourished.compat;

import com.google.gson.annotations.SerializedName;
import dev.maire.nourished.api.ApiStatus;

@ApiStatus.Internal
public record ConflictBehavior(
        @SerializedName("disable_effects")
        boolean disableEffects,

        @SerializedName("disable_decay")
        boolean disableDecay,

        @SerializedName("disable_memory")
        boolean disableMemory,

        @SerializedName("disable_hud")
        boolean disableHud
) {
    public static final ConflictBehavior NONE = new ConflictBehavior(false, false, false, false);

    public ConflictBehavior merge(ConflictBehavior other) {
        if (other == null) return this;
        return new ConflictBehavior(
                this.disableEffects || other.disableEffects,
                this.disableDecay || other.disableDecay,
                this.disableMemory || other.disableMemory,
                this.disableHud || other.disableHud
        );
    }
}
