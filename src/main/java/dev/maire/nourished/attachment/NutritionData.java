package dev.maire.nourished.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

public class NutritionData {

    public static final Codec<NutritionData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("protein").forGetter(d -> d.protein),
                    Codec.FLOAT.fieldOf("carbs").forGetter(d -> d.carbs),
                    Codec.FLOAT.fieldOf("fats").forGetter(d -> d.fats),
                    Codec.FLOAT.fieldOf("vitamins").forGetter(d -> d.vitamins),
                    Codec.FLOAT.fieldOf("hydration").forGetter(d -> d.hydration)
            ).apply(instance, NutritionData::new)
    );

    public float protein;
    public float carbs;
    public float fats;
    public float vitamins;
    public float hydration;

    public NutritionData() {}

    public NutritionData(float protein, float carbs, float fats, float vitamins, float hydration) {
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
        this.vitamins = vitamins;
        this.hydration = hydration;
    }

    public void addProtein(float v)    { protein   = Mth.clamp(protein   + v, 0f, 1f); }
    public void addCarbs(float v)      { carbs     = Mth.clamp(carbs     + v, 0f, 1f); }
    public void addFats(float v)       { fats      = Mth.clamp(fats      + v, 0f, 1f); }
    public void addVitamins(float v)   { vitamins  = Mth.clamp(vitamins  + v, 0f, 1f); }
    public void addHydration(float v)  { hydration = Mth.clamp(hydration + v, 0f, 1f); }

    public void decay(float rate) {
        protein   = Math.max(0f, protein   - rate);
        carbs     = Math.max(0f, carbs     - rate);
        fats      = Math.max(0f, fats      - rate);
        vitamins  = Math.max(0f, vitamins  - rate);
        hydration = Math.max(0f, hydration - rate);
    }

    @Override
    public String toString() {
        return String.format("NutritionData{protein=%.2f, carbs=%.2f, fats=%.2f, vitamins=%.2f, hydration=%.2f}",
                protein, carbs, fats, vitamins, hydration);
    }
}
