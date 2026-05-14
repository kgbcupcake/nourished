package dev.maire.nourished.core.nutrition;

import dev.maire.nourished.core.nutrition.cache.RunningAverage;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Context passed through the resolution pipeline to each stage handler.
 * Mutable to allow earlier stages to deposit signals for later stages.
 */
public final class StageContext {
    private final Holder<Item> holder;
    private final ResourceLocation itemId;
    @Nullable private final RecipeManager recipeManager;
    private final Map<String, RunningAverage> namespacePeers;
    private final Set<String> validKeys;
    private final Map<String, Float> communityTagSignal = new HashMap<>();

    public StageContext(Holder<Item> holder, ResourceLocation itemId,
                        @Nullable RecipeManager recipeManager,
                        Map<String, RunningAverage> namespacePeers,
                        Set<String> validKeys) {
        this.holder = holder;
        this.itemId = itemId;
        this.recipeManager = recipeManager;
        this.namespacePeers = namespacePeers;
        this.validKeys = validKeys;
    }

    public Holder<Item> holder() { return holder; }
    public ResourceLocation itemId() { return itemId; }
    @Nullable public RecipeManager recipeManager() { return recipeManager; }
    public Map<String, RunningAverage> namespacePeers() { return namespacePeers; }
    public Set<String> validKeys() { return validKeys; }
    public Map<String, Float> communityTagSignal() { return communityTagSignal; }
}
