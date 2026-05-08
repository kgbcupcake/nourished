package dev.maire.nourished.data;

/**
 * Constants describing Nourished datapack schema locations and keys.
 *
 * <p>All JSON files are loaded from {@code data/<namespace>/nourished/}.</p>
 */
public final class DatapackSchema {

    private DatapackSchema() {}

    /** Base directory under a datapack namespace. */
    public static final String ROOT = "nourished";

    /** Path: {@code data/<namespace>/nourished/nutrients/<id>.json}. */
    public static final String NUTRIENTS_DIR = "nutrients";
    /** Required string key for nutrient display name. */
    public static final String KEY_DISPLAY_NAME = "display_name";

    /** Path: {@code data/<namespace>/nourished/food_classifications/<id>.json}. */
    public static final String FOOD_CLASSIFICATIONS_DIR = "food_classifications";
    /** Required string key for nutrient target. */
    public static final String KEY_NUTRIENT_KEY = "nutrient_key";
    /** Required numeric key for nutrient amount. */
    public static final String KEY_AMOUNT = "amount";
    /** Optional item id mapping key. */
    public static final String KEY_ITEM = "item";
    /** Optional item tag mapping key. */
    public static final String KEY_TAG = "tag";

    /** Path: {@code data/<namespace>/nourished/effects/<id>.json}. */
    public static final String EFFECTS_DIR = "effects";
    /** Required numeric key for trigger threshold. */
    public static final String KEY_THRESHOLD = "threshold";
    /** Required enum key for threshold type (CRITICAL/LOW/EXCESS/BONUS). */
    public static final String KEY_THRESHOLD_TYPE = "threshold_type";
    /** Required effect id key. */
    public static final String KEY_EFFECT_ID = "effect_id";
    /** Optional integer key for effect amplifier. */
    public static final String KEY_AMPLIFIER = "amplifier";
    /** Optional integer key for effect duration in ticks. */
    public static final String KEY_DURATION = "duration";

    /** Path: {@code data/<namespace>/nourished/synergies/<id>.json}. */
    public static final String SYNERGIES_DIR = "synergies";
    /** Required first nutrient key. */
    public static final String KEY_NUTRIENT_A_KEY = "nutrient_a_key";
    /** Required first nutrient condition key (HIGH/LOW/OPTIMAL). */
    public static final String KEY_NUTRIENT_A_CONDITION = "nutrient_a_condition";
    /** Required second nutrient key. */
    public static final String KEY_NUTRIENT_B_KEY = "nutrient_b_key";
    /** Required second nutrient condition key (HIGH/LOW/OPTIMAL). */
    public static final String KEY_NUTRIENT_B_CONDITION = "nutrient_b_condition";
    /** Optional synergy effect id key. */
    public static final String KEY_BONUS_EFFECT_ID = "bonus_effect_id";
    /** Optional synergy effect duration key. */
    public static final String KEY_EFFECT_DURATION = "effect_duration";
    /** Optional penalty flag key. */
    public static final String KEY_IS_PENALTY = "is_penalty";

    /** Path: {@code data/<namespace>/nourished/food_synergies/<id>.json}. */
    public static final String FOOD_SYNERGIES_DIR = "food_synergies";
    /** Required first item key. */
    public static final String KEY_FOOD_A = "food_a";
    /** Required second item key. */
    public static final String KEY_FOOD_B = "food_b";
    /** Optional combo time window key in ticks. */
    public static final String KEY_TIME_WINDOW_TICKS = "time_window_ticks";
    /** Required nutrient key that receives combo bonus. */
    public static final String KEY_BONUS_NUTRIENT_KEY = "bonus_nutrient_key";
    /** Required numeric combo bonus amount key. */
    public static final String KEY_BONUS_AMOUNT = "bonus_amount";

    /** Path: {@code data/<namespace>/nourished/milestones/<id>.json}. */
    public static final String MILESTONES_DIR = "milestones";
    /** Required cumulative nutrient goal key. */
    public static final String KEY_CUMULATIVE_GOAL = "cumulative_goal";
    /** Optional milestone reward effect id key. */
    public static final String KEY_REWARD_EFFECT_ID = "reward_effect_id";
    /** Optional milestone reward duration key in ticks. */
    public static final String KEY_REWARD_DURATION = "reward_duration";
    /** Optional advancement id key. */
    public static final String KEY_ADVANCEMENT_ID = "advancement_id";

    /** Path: {@code data/<namespace>/nourished/diet_profiles/<id>.json}. */
    public static final String DIET_PROFILES_DIR = "diet_profiles";
    /** Optional profile description key. */
    public static final String KEY_DESCRIPTION = "description";
    /** Optional object key containing nutrient threshold overrides. */
    public static final String KEY_CUSTOM_THRESHOLDS = "custom_thresholds";
    /** Optional object key containing nutrient decay overrides. */
    public static final String KEY_CUSTOM_DECAY_RATES = "custom_decay_rates";
    /** Optional array key of bonus effect ids. */
    public static final String KEY_BONUS_EFFECTS = "bonus_effects";

    /** Path: {@code data/<namespace>/nourished/compat/<id>.json}. */
    public static final String COMPAT_DIR = "compat";
    /** Required target mod id key for compat entries. */
    public static final String KEY_MOD_ID = "mod_id";
    /** Optional compat category key (FOOD_MOD/FARMING_MOD/SURVIVAL_OVERHAUL). */
    public static final String KEY_CATEGORY = "category";
    /** Optional object key mapping item ids to nutrient keys. */
    public static final String KEY_MAPPINGS = "mappings";
}
