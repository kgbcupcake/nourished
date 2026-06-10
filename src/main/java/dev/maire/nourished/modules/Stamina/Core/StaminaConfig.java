package dev.maire.nourished.modules.Stamina.Core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.marie.MariesLib.util.MarieJsonUtils;
import dev.marie.MariesLib.util.MarieResourceLoader;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Configuration stack for the Stamina module.
 */
@ApiStatus.Internal
public final class StaminaConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_RESOURCE_PATH = "/data/" + Nourished.MODID + "/config/stamina.json";

    private static float initialPhysicalMax = 100.0f;
    private static float initialMentalMax = 100.0f;
    private static float minStamina = 10.0f;
    private static float maxStamina = 200.0f;

    private static float maxFatiguePenalty = 50.0f;
    private static float fatigueThreshold = 0.25f;
    private static float fatigueBuildRate = 0.1f;
    private static float fatigueDecayRate = 0.05f;
    private static int fatigueDurationTicks = 180;

    private static float maxDebt = 30.0f;
    private static float debtRepayRate = 0.3f;

    private static float bonusDecayRate = 0.05f;

    private static float minNutritionModifier = 0.5f;
    private static float maxNutritionModifier = 1.25f;
    private static float minGutModifier = 0.6f;

    private static float basePhysicalRegen = 0.36f;
    private static float baseMentalRegen = 0.25f;
    private static int regenDelay = 50;
    private static float regenRestMultiplier = 1.45f;

    private static boolean enableSprint = true;
    private static float sprintCost = 0.15f;
    private static boolean enableJump = true;
    private static float jumpCost = 0.85f;
    private static boolean enableAttack = true;
    private static float attackCost = 3.45f;
    private static boolean enableMissedAttack = true;
    private static float missedAttackCost = 1.0f;
    private static boolean enableElytra = true;
    private static float elytraCost = 0.25f;
    private static boolean enableSwim = true;
    private static float swimCost = 0.05f;
    private static boolean enableClimb = true;
    private static float climbCost = 0.7f;
    private static boolean enableTakeDamage = true;
    private static float takeDamageCost = 0.5f;

    private static boolean enableMine = true;
    private static float mineCost = 0.3f;
    private static boolean enablePlace = true;
    private static float placeCost = 0.2f;
    private static boolean enableFish = true;
    private static float fishCost = 0.5f;
    private static boolean enableEat = true;
    private static float eatCost = 0.3f;
    private static boolean enableRawEatPenalty = true;
    private static float rawEatCostMultiplier = 2.0f;
    private static boolean enableUseItem = true;
    private static float useItemCost = 0.2f;

    private StaminaConfig() {}

    public static float initialPhysicalMax() {
        return initialPhysicalMax;
    }

    public static float initialMentalMax() {
        return initialMentalMax;
    }

    public static float minStamina() {
        return minStamina;
    }

    public static float maxStamina() {
        return maxStamina;
    }

    public static float maxFatiguePenalty() {
        return maxFatiguePenalty;
    }

    public static float fatigueThreshold() {
        return fatigueThreshold;
    }

    public static float fatigueBuildRate() {
        return fatigueBuildRate;
    }

    public static float fatigueDecayRate() {
        return fatigueDecayRate;
    }

    public static int fatigueDurationTicks() {
        return fatigueDurationTicks;
    }

    public static float maxDebt() {
        return maxDebt;
    }

    public static float debtRepayRate() {
        return debtRepayRate;
    }

    public static float bonusDecayRate() {
        return bonusDecayRate;
    }

    public static float minNutritionModifier() {
        return minNutritionModifier;
    }

    public static float maxNutritionModifier() {
        return maxNutritionModifier;
    }

    public static float minGutModifier() {
        return minGutModifier;
    }

    public static float basePhysicalRegen() {
        return basePhysicalRegen;
    }

    public static float baseMentalRegen() {
        return baseMentalRegen;
    }

    public static int regenDelay() {
        return regenDelay;
    }

    public static float regenRestMultiplier() {
        return regenRestMultiplier;
    }

    public static boolean enableSprint() {
        return enableSprint;
    }

    public static float sprintCost() {
        return sprintCost;
    }

    public static boolean enableJump() {
        return enableJump;
    }

    public static float jumpCost() {
        return jumpCost;
    }

    public static boolean enableAttack() {
        return enableAttack;
    }

    public static float attackCost() {
        return attackCost;
    }

    public static boolean enableMissedAttack() {
        return enableMissedAttack;
    }

    public static float missedAttackCost() {
        return missedAttackCost;
    }

    public static boolean enableElytra() {
        return enableElytra;
    }

    public static float elytraCost() {
        return elytraCost;
    }

    public static boolean enableSwim() {
        return enableSwim;
    }

    public static float swimCost() {
        return swimCost;
    }

    public static boolean enableClimb() {
        return enableClimb;
    }

    public static float climbCost() {
        return climbCost;
    }

    public static boolean enableTakeDamage() {
        return enableTakeDamage;
    }

    public static float takeDamageCost() {
        return takeDamageCost;
    }

    public static boolean enableMine() {
        return enableMine;
    }

    public static float mineCost() {
        return mineCost;
    }

    public static boolean enablePlace() {
        return enablePlace;
    }

    public static float placeCost() {
        return placeCost;
    }

    public static boolean enableFish() {
        return enableFish;
    }

    public static float fishCost() {
        return fishCost;
    }

    public static boolean enableEat() {
        return enableEat;
    }

    public static float eatCost() {
        return eatCost;
    }

    public static boolean enableRawEatPenalty() {
        return enableRawEatPenalty;
    }

    public static float rawEatCostMultiplier() {
        return rawEatCostMultiplier;
    }

    public static boolean enableUseItem() {
        return enableUseItem;
    }

    public static float useItemCost() {
        return useItemCost;
    }

    public static void setEnableSprint(boolean value) {
        enableSprint = value;
    }

    public static void setSprintCost(float value) {
        sprintCost = value;
        sanitize();
    }

    public static void setEnableJump(boolean value) {
        enableJump = value;
    }

    public static void setJumpCost(float value) {
        jumpCost = value;
        sanitize();
    }

    public static void setEnableAttack(boolean value) {
        enableAttack = value;
    }

    public static void setAttackCost(float value) {
        attackCost = value;
        sanitize();
    }

    public static void setEnableMissedAttack(boolean value) {
        enableMissedAttack = value;
    }

    public static void setMissedAttackCost(float value) {
        missedAttackCost = value;
        sanitize();
    }

    public static void setEnableElytra(boolean value) {
        enableElytra = value;
    }

    public static void setElytraCost(float value) {
        elytraCost = value;
        sanitize();
    }

    public static void setEnableSwim(boolean value) {
        enableSwim = value;
    }

    public static void setSwimCost(float value) {
        swimCost = value;
        sanitize();
    }

    public static void setEnableClimb(boolean value) {
        enableClimb = value;
    }

    public static void setClimbCost(float value) {
        climbCost = value;
        sanitize();
    }

    public static void setEnableTakeDamage(boolean value) {
        enableTakeDamage = value;
    }

    public static void setTakeDamageCost(float value) {
        takeDamageCost = value;
        sanitize();
    }

    public static void setEnableMine(boolean value) {
        enableMine = value;
    }

    public static void setMineCost(float value) {
        mineCost = value;
        sanitize();
    }

    public static void setEnablePlace(boolean value) {
        enablePlace = value;
    }

    public static void setPlaceCost(float value) {
        placeCost = value;
        sanitize();
    }

    public static void setEnableFish(boolean value) {
        enableFish = value;
    }

    public static void setFishCost(float value) {
        fishCost = value;
        sanitize();
    }

    public static void setEnableEat(boolean value) {
        enableEat = value;
    }

    public static void setEatCost(float value) {
        eatCost = value;
        sanitize();
    }

    public static void setEnableRawEatPenalty(boolean value) {
        enableRawEatPenalty = value;
    }

    public static void setRawEatCostMultiplier(float value) {
        rawEatCostMultiplier = value;
        sanitize();
    }

    public static void setEnableUseItem(boolean value) {
        enableUseItem = value;
    }

    public static void setUseItemCost(float value) {
        useItemCost = value;
        sanitize();
    }

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("stamina.json");

        resetToDefaults();
        try {
            parseBundledDefaults();
            Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                writeDefaults(file);
                Nourished.LOGGER.info("[StaminaConfig] Wrote default stamina.json");
            } else {
                parse(file);
            }
            sanitize();
            Nourished.LOGGER.info("[StaminaConfig] Loaded stamina.json from config stack");
        } catch (IOException e) {
            Nourished.LOGGER.error("[StaminaConfig] Failed to load stamina.json, using built-in defaults", e);
            loadDefaults();
        }
    }

    public static void loadFromDatapack(ResourceManager resourceManager) {
        MarieResourceLoader.loadFromModConfig(
                resourceManager,
                "config/stamina.json",
                reader -> {
                    load();
                    parseFromReader(reader);
                    sanitize();
                    return true;
                },
                StaminaConfig::load,
                "[StaminaConfig] Loaded from datapack override",
                "[StaminaConfig] Failed to load from datapack, falling back to config folder",
                "[StaminaConfig] Loaded from config folder"
        );
    }

    public static void reload() {
        Nourished.LOGGER.info("[StaminaConfig] Reloading stamina.json");
        load();
    }

    public static void save() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Nourished.MODID);
        Path file = configDir.resolve("stamina.json");
        sanitize();
        try {
            Files.createDirectories(configDir);
            writeCurrent(file);
            Nourished.LOGGER.info("[StaminaConfig] Saved stamina.json");
        } catch (IOException e) {
            Nourished.LOGGER.error("[StaminaConfig] Failed to save stamina.json", e);
        }
    }

    private static void parse(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            parseFromReader(reader);
        }
    }

    private static void parseBundledDefaults() throws IOException {
        try (InputStream in = StaminaConfig.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
            if (in == null) {
                return;
            }
            try (Reader reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
                parseFromReader(reader);
            }
        }
    }

    private static void parseFromReader(Reader reader) {
        JsonObject root = GSON.fromJson(reader, JsonObject.class);
        if (root == null) {
            Nourished.LOGGER.warn("[StaminaConfig] stamina.json was empty, keeping lower-priority values");
            return;
        }

        initialPhysicalMax = MarieJsonUtils.getOptionalFloat(root, "initialPhysicalMax", initialPhysicalMax);
        initialMentalMax = MarieJsonUtils.getOptionalFloat(root, "initialMentalMax", initialMentalMax);
        minStamina = MarieJsonUtils.getOptionalFloat(root, "minStamina", minStamina);
        maxStamina = MarieJsonUtils.getOptionalFloat(root, "maxStamina", maxStamina);
        maxFatiguePenalty = MarieJsonUtils.getOptionalFloat(root, "maxFatiguePenalty", maxFatiguePenalty);
        fatigueThreshold = MarieJsonUtils.getOptionalFloat(root, "fatigueThreshold", fatigueThreshold);
        fatigueBuildRate = MarieJsonUtils.getOptionalFloat(root, "fatigueBuildRate", fatigueBuildRate);
        fatigueDecayRate = MarieJsonUtils.getOptionalFloat(root, "fatigueDecayRate", fatigueDecayRate);
        fatigueDurationTicks = MarieJsonUtils.getOptionalInt(root, "fatigueDurationTicks", fatigueDurationTicks);
        maxDebt = MarieJsonUtils.getOptionalFloat(root, "maxDebt", maxDebt);
        debtRepayRate = MarieJsonUtils.getOptionalFloat(root, "debtRepayRate", debtRepayRate);
        bonusDecayRate = MarieJsonUtils.getOptionalFloat(root, "bonusDecayRate", bonusDecayRate);
        minNutritionModifier = MarieJsonUtils.getOptionalFloat(root, "minNutritionModifier", minNutritionModifier);
        maxNutritionModifier = MarieJsonUtils.getOptionalFloat(root, "maxNutritionModifier", maxNutritionModifier);
        minGutModifier = MarieJsonUtils.getOptionalFloat(root, "minGutModifier", minGutModifier);
        basePhysicalRegen = MarieJsonUtils.getOptionalFloat(root, "basePhysicalRegen", basePhysicalRegen);
        baseMentalRegen = MarieJsonUtils.getOptionalFloat(root, "baseMentalRegen", baseMentalRegen);
        regenDelay = MarieJsonUtils.getOptionalInt(root, "regenDelay", regenDelay);
        regenRestMultiplier = MarieJsonUtils.getOptionalFloat(root, "regenRestMultiplier", regenRestMultiplier);
        enableSprint = MarieJsonUtils.getOptionalBoolean(root, "enableSprint", enableSprint);
        sprintCost = MarieJsonUtils.getOptionalFloat(root, "sprintCost", sprintCost);
        enableJump = MarieJsonUtils.getOptionalBoolean(root, "enableJump", enableJump);
        jumpCost = MarieJsonUtils.getOptionalFloat(root, "jumpCost", jumpCost);
        enableAttack = MarieJsonUtils.getOptionalBoolean(root, "enableAttack", enableAttack);
        attackCost = MarieJsonUtils.getOptionalFloat(root, "attackCost", attackCost);
        enableMissedAttack = MarieJsonUtils.getOptionalBoolean(root, "enableMissedAttack", enableMissedAttack);
        missedAttackCost = MarieJsonUtils.getOptionalFloat(root, "missedAttackCost", missedAttackCost);
        enableElytra = MarieJsonUtils.getOptionalBoolean(root, "enableElytra", enableElytra);
        elytraCost = MarieJsonUtils.getOptionalFloat(root, "elytraCost", elytraCost);
        enableSwim = MarieJsonUtils.getOptionalBoolean(root, "enableSwim", enableSwim);
        swimCost = MarieJsonUtils.getOptionalFloat(root, "swimCost", swimCost);
        enableClimb = MarieJsonUtils.getOptionalBoolean(root, "enableClimb", enableClimb);
        climbCost = MarieJsonUtils.getOptionalFloat(root, "climbCost", climbCost);
        enableTakeDamage = MarieJsonUtils.getOptionalBoolean(root, "enableTakeDamage", enableTakeDamage);
        takeDamageCost = MarieJsonUtils.getOptionalFloat(root, "takeDamageCost", takeDamageCost);
        enableMine = MarieJsonUtils.getOptionalBoolean(root, "enableMine", enableMine);
        mineCost = MarieJsonUtils.getOptionalFloat(root, "mineCost", mineCost);
        enablePlace = MarieJsonUtils.getOptionalBoolean(root, "enablePlace", enablePlace);
        placeCost = MarieJsonUtils.getOptionalFloat(root, "placeCost", placeCost);
        enableFish = MarieJsonUtils.getOptionalBoolean(root, "enableFish", enableFish);
        fishCost = MarieJsonUtils.getOptionalFloat(root, "fishCost", fishCost);
        enableEat = MarieJsonUtils.getOptionalBoolean(root, "enableEat", enableEat);
        eatCost = MarieJsonUtils.getOptionalFloat(root, "eatCost", eatCost);
        enableRawEatPenalty = MarieJsonUtils.getOptionalBoolean(root, "enableRawEatPenalty", enableRawEatPenalty);
        rawEatCostMultiplier = MarieJsonUtils.getOptionalFloat(root, "rawEatCostMultiplier", rawEatCostMultiplier);
        enableUseItem = MarieJsonUtils.getOptionalBoolean(root, "enableUseItem", enableUseItem);
        useItemCost = MarieJsonUtils.getOptionalFloat(root, "useItemCost", useItemCost);
    }

    private static void loadDefaults() {
        resetToDefaults();
        try {
            parseBundledDefaults();
            sanitize();
        } catch (IOException e) {
            Nourished.LOGGER.warn("[StaminaConfig] Failed to load bundled stamina defaults: {}", e.getMessage());
            sanitize();
        }
    }

    private static void resetToDefaults() {
        initialPhysicalMax = 100.0f;
        initialMentalMax = 100.0f;
        minStamina = 10.0f;
        maxStamina = 200.0f;
        maxFatiguePenalty = 50.0f;
        fatigueThreshold = 0.25f;
        fatigueBuildRate = 0.1f;
        fatigueDecayRate = 0.05f;
        fatigueDurationTicks = 180;
        maxDebt = 30.0f;
        debtRepayRate = 0.3f;
        bonusDecayRate = 0.05f;
        minNutritionModifier = 0.5f;
        maxNutritionModifier = 1.25f;
        minGutModifier = 0.6f;
        basePhysicalRegen = 0.36f;
        baseMentalRegen = 0.25f;
        regenDelay = 50;
        regenRestMultiplier = 1.45f;
        enableSprint = true;
        sprintCost = 0.15f;
        enableJump = true;
        jumpCost = 0.85f;
        enableAttack = true;
        attackCost = 3.45f;
        enableMissedAttack = true;
        missedAttackCost = 1.0f;
        enableElytra = true;
        elytraCost = 0.25f;
        enableSwim = true;
        swimCost = 0.05f;
        enableClimb = true;
        climbCost = 0.7f;
        enableTakeDamage = true;
        takeDamageCost = 0.5f;
        enableMine = true;
        mineCost = 0.3f;
        enablePlace = true;
        placeCost = 0.2f;
        enableFish = true;
        fishCost = 0.5f;
        enableEat = true;
        eatCost = 0.3f;
        enableRawEatPenalty = true;
        rawEatCostMultiplier = 2.0f;
        enableUseItem = true;
        useItemCost = 0.2f;
    }

    private static void sanitize() {
        minStamina = Math.max(0.0f, minStamina);
        maxStamina = Math.max(minStamina, maxStamina);
        initialPhysicalMax = clampNonNegative(initialPhysicalMax);
        initialMentalMax = clampNonNegative(initialMentalMax);
        maxFatiguePenalty = clampNonNegative(maxFatiguePenalty);
        fatigueThreshold = clamp01(fatigueThreshold);
        fatigueBuildRate = clampNonNegative(fatigueBuildRate);
        fatigueDecayRate = clampNonNegative(fatigueDecayRate);
        fatigueDurationTicks = Math.max(0, fatigueDurationTicks);
        maxDebt = clampNonNegative(maxDebt);
        debtRepayRate = clampNonNegative(debtRepayRate);
        bonusDecayRate = clampNonNegative(bonusDecayRate);
        minNutritionModifier = clampNonNegative(minNutritionModifier);
        maxNutritionModifier = Math.max(minNutritionModifier, maxNutritionModifier);
        minGutModifier = clamp01(minGutModifier);
        basePhysicalRegen = clampNonNegative(basePhysicalRegen);
        baseMentalRegen = clampNonNegative(baseMentalRegen);
        regenDelay = Math.max(0, regenDelay);
        regenRestMultiplier = clampNonNegative(regenRestMultiplier);
        sprintCost = clampNonNegative(sprintCost);
        jumpCost = clampNonNegative(jumpCost);
        attackCost = clampNonNegative(attackCost);
        missedAttackCost = clampNonNegative(missedAttackCost);
        elytraCost = clampNonNegative(elytraCost);
        swimCost = clampNonNegative(swimCost);
        climbCost = clampNonNegative(climbCost);
        takeDamageCost = clampNonNegative(takeDamageCost);
        mineCost = clampNonNegative(mineCost);
        placeCost = clampNonNegative(placeCost);
        fishCost = clampNonNegative(fishCost);
        eatCost = clampNonNegative(eatCost);
        rawEatCostMultiplier = clampNonNegative(rawEatCostMultiplier);
        useItemCost = clampNonNegative(useItemCost);
    }

    private static float clampNonNegative(float value) {
        return Math.max(0.0f, value);
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static void writeDefaults(Path file) throws IOException {
        try (InputStream in = StaminaConfig.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
            if (in == null) {
                throw new IOException("Missing bundled " + DEFAULT_RESOURCE_PATH);
            }
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void writeCurrent(Path file) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("initialPhysicalMax", initialPhysicalMax);
        root.addProperty("initialMentalMax", initialMentalMax);
        root.addProperty("minStamina", minStamina);
        root.addProperty("maxStamina", maxStamina);
        root.addProperty("maxFatiguePenalty", maxFatiguePenalty);
        root.addProperty("fatigueThreshold", fatigueThreshold);
        root.addProperty("fatigueBuildRate", fatigueBuildRate);
        root.addProperty("fatigueDecayRate", fatigueDecayRate);
        root.addProperty("fatigueDurationTicks", fatigueDurationTicks);
        root.addProperty("maxDebt", maxDebt);
        root.addProperty("debtRepayRate", debtRepayRate);
        root.addProperty("bonusDecayRate", bonusDecayRate);
        root.addProperty("minNutritionModifier", minNutritionModifier);
        root.addProperty("maxNutritionModifier", maxNutritionModifier);
        root.addProperty("minGutModifier", minGutModifier);
        root.addProperty("basePhysicalRegen", basePhysicalRegen);
        root.addProperty("baseMentalRegen", baseMentalRegen);
        root.addProperty("regenDelay", regenDelay);
        root.addProperty("regenRestMultiplier", regenRestMultiplier);
        root.addProperty("enableSprint", enableSprint);
        root.addProperty("sprintCost", sprintCost);
        root.addProperty("enableJump", enableJump);
        root.addProperty("jumpCost", jumpCost);
        root.addProperty("enableAttack", enableAttack);
        root.addProperty("attackCost", attackCost);
        root.addProperty("enableMissedAttack", enableMissedAttack);
        root.addProperty("missedAttackCost", missedAttackCost);
        root.addProperty("enableElytra", enableElytra);
        root.addProperty("elytraCost", elytraCost);
        root.addProperty("enableSwim", enableSwim);
        root.addProperty("swimCost", swimCost);
        root.addProperty("enableClimb", enableClimb);
        root.addProperty("climbCost", climbCost);
        root.addProperty("enableTakeDamage", enableTakeDamage);
        root.addProperty("takeDamageCost", takeDamageCost);
        root.addProperty("enableMine", enableMine);
        root.addProperty("mineCost", mineCost);
        root.addProperty("enablePlace", enablePlace);
        root.addProperty("placeCost", placeCost);
        root.addProperty("enableFish", enableFish);
        root.addProperty("fishCost", fishCost);
        root.addProperty("enableEat", enableEat);
        root.addProperty("eatCost", eatCost);
        root.addProperty("enableRawEatPenalty", enableRawEatPenalty);
        root.addProperty("rawEatCostMultiplier", rawEatCostMultiplier);
        root.addProperty("enableUseItem", enableUseItem);
        root.addProperty("useItemCost", useItemCost);
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(root, writer);
        }
    }
}
