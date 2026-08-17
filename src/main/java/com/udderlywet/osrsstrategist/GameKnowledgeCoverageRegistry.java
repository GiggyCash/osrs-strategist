package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Granular, honest product-completeness registry.
 *
 * <p>This class is deliberately conservative. An area is marked VERIFIED only
 * when production behavior is backed by sufficiently complete, validated game
 * data and tests. PARTIAL means useful production support exists but additional
 * methods/content/readers remain. SCAFFOLDED means Strategist has a conceptual
 * home for the area but should not claim comprehensive support.</p>
 *
 * <p>The registry is not used to hide incomplete features from developers. Its
 * purpose is the opposite: make missing game knowledge visible and searchable
 * so contributors cannot accidentally assume a broad feature label means the
 * underlying data is exhaustive.</p>
 */
@Singleton
public class GameKnowledgeCoverageRegistry
{
    private final Map<GameKnowledgeArea, KnowledgeCoverage> coverage;

    public GameKnowledgeCoverageRegistry()
    {
        EnumMap<GameKnowledgeArea, KnowledgeCoverage> values =
                new EnumMap<>(GameKnowledgeArea.class);

        for (GameKnowledgeArea area : GameKnowledgeArea.values())
        {
            values.put(area, KnowledgeCoverage.SCAFFOLDED);
        }

        // Live account-state foundations.
        partial(values,
                GameKnowledgeArea.ACCOUNT_TYPES,
                GameKnowledgeArea.MEMBERSHIP_ACCESS,
                GameKnowledgeArea.RESTRICTED_BUILDS,
                GameKnowledgeArea.SKILL_LEVELS_AND_XP,
                GameKnowledgeArea.QUEST_POINTS,
                GameKnowledgeArea.INVENTORY,
                GameKnowledgeArea.EQUIPMENT,
                GameKnowledgeArea.BANK,
                GameKnowledgeArea.GROUP_STORAGE,
                GameKnowledgeArea.UIM_STORAGE,
                GameKnowledgeArea.GP_AND_LIQUIDITY,
                GameKnowledgeArea.PROTECTED_ITEMS);

        // Training catalogs currently contain useful level-aware methods across
        // all skills, but they are intentionally not called exhaustive yet.
        partial(values,
                GameKnowledgeArea.ATTACK_TRAINING,
                GameKnowledgeArea.STRENGTH_TRAINING,
                GameKnowledgeArea.DEFENCE_TRAINING,
                GameKnowledgeArea.HITPOINTS_TRAINING,
                GameKnowledgeArea.RANGED_TRAINING,
                GameKnowledgeArea.PRAYER_TRAINING,
                GameKnowledgeArea.MAGIC_TRAINING,
                GameKnowledgeArea.RUNECRAFT_TRAINING,
                GameKnowledgeArea.CONSTRUCTION_TRAINING,
                GameKnowledgeArea.AGILITY_TRAINING,
                GameKnowledgeArea.HERBLORE_TRAINING,
                GameKnowledgeArea.THIEVING_TRAINING,
                GameKnowledgeArea.CRAFTING_TRAINING,
                GameKnowledgeArea.FLETCHING_TRAINING,
                GameKnowledgeArea.SLAYER_TRAINING,
                GameKnowledgeArea.HUNTER_TRAINING,
                GameKnowledgeArea.MINING_TRAINING,
                GameKnowledgeArea.SMITHING_TRAINING,
                GameKnowledgeArea.FISHING_TRAINING,
                GameKnowledgeArea.COOKING_TRAINING,
                GameKnowledgeArea.FIREMAKING_TRAINING,
                GameKnowledgeArea.WOODCUTTING_TRAINING,
                GameKnowledgeArea.FARMING_TRAINING,
                GameKnowledgeArea.SAILING_TRAINING);

        // Progression candidate/readers.
        partial(values,
                GameKnowledgeArea.QUESTS,
                GameKnowledgeArea.ACHIEVEMENT_DIARIES,
                GameKnowledgeArea.COMBAT_ACHIEVEMENTS,
                GameKnowledgeArea.COLLECTION_LOG,
                GameKnowledgeArea.SLAYER_UNLOCKS,
                GameKnowledgeArea.FARMING_PATCHES,
                GameKnowledgeArea.FARMING_RUNS,
                GameKnowledgeArea.FARMING_CONTRACTS,
                GameKnowledgeArea.TOOL_LEPRECHAUN,
                GameKnowledgeArea.BIRDHOUSE_RUNS,
                GameKnowledgeArea.CLUE_TIERS,
                GameKnowledgeArea.CLUE_REQUIREMENTS,
                GameKnowledgeArea.CLUE_STASH_UNITS,
                GameKnowledgeArea.CLUE_WILDERNESS_RISK);

        // PvM/minigame/equipment foundations.
        partial(values,
                GameKnowledgeArea.BOSSES,
                GameKnowledgeArea.DEMIBOSSES,
                GameKnowledgeArea.SLAYER_BOSSES,
                GameKnowledgeArea.RAIDS,
                GameKnowledgeArea.PVM_REQUIREMENTS,
                GameKnowledgeArea.PVM_GEAR_LOADOUTS,
                GameKnowledgeArea.PVM_SUPPLIES,
                GameKnowledgeArea.PVM_RISK,
                GameKnowledgeArea.MINIGAMES,
                GameKnowledgeArea.SKILLING_BOSSES,
                GameKnowledgeArea.MINIGAME_REWARDS,
                GameKnowledgeArea.MINIGAME_CURRENCIES,
                GameKnowledgeArea.MELEE_GEAR_PROGRESSION,
                GameKnowledgeArea.RANGED_GEAR_PROGRESSION,
                GameKnowledgeArea.MAGIC_GEAR_PROGRESSION,
                GameKnowledgeArea.SKILLING_OUTFITS,
                GameKnowledgeArea.GRACEFUL,
                GameKnowledgeArea.RAIMENTS_OF_THE_EYE,
                GameKnowledgeArea.PROSPECTOR,
                GameKnowledgeArea.ANGLER,
                GameKnowledgeArea.FARMERS_OUTFIT,
                GameKnowledgeArea.LUMBERJACK,
                GameKnowledgeArea.PYROMANCER,
                GameKnowledgeArea.SMITHS_UNIFORM,
                GameKnowledgeArea.ROGUES_OUTFIT,
                GameKnowledgeArea.USEFUL_UNTRADEABLES);

        // World/resource/economy foundations.
        partial(values,
                GameKnowledgeArea.TELEPORT_SPELLS,
                GameKnowledgeArea.TELEPORT_JEWELLERY,
                GameKnowledgeArea.FAIRY_RINGS,
                GameKnowledgeArea.SPIRIT_TREES,
                GameKnowledgeArea.POH_TELEPORTS,
                GameKnowledgeArea.WORLD_ACCESS_REQUIREMENTS,
                GameKnowledgeArea.POH_STORAGE,
                GameKnowledgeArea.RESOURCE_SOURCES,
                GameKnowledgeArea.SHOPS,
                GameKnowledgeArea.ITEM_SPAWNS,
                GameKnowledgeArea.MONSTER_DROPS,
                GameKnowledgeArea.PROCESSING_CHAINS,
                GameKnowledgeArea.GRAND_EXCHANGE_BUY_PATHS,
                GameKnowledgeArea.IRONMAN_SOURCE_PATHS,
                GameKnowledgeArea.UIM_SOURCE_PATHS,
                GameKnowledgeArea.MONEY_MAKING,
                GameKnowledgeArea.HIGH_ALCH_OPTIONS,
                GameKnowledgeArea.TEARS_OF_GUTHIX,
                GameKnowledgeArea.KINGDOM_OF_MISCELLANIA,
                GameKnowledgeArea.BATTLESTAVES,
                GameKnowledgeArea.DYNAMITE,
                GameKnowledgeArea.DIARY_DAILIES,
                GameKnowledgeArea.COOLDOWN_ACTIVITIES);

        // Planner behavior can be verified independently of exhaustive game
        // data. These are architectural guarantees with focused tests.
        verified(values,
                GameKnowledgeArea.SESSION_LENGTH_FIT,
                GameKnowledgeArea.AFK_FIT,
                GameKnowledgeArea.ATTENTION_FIT,
                GameKnowledgeArea.PREFERENCE_LEARNING,
                GameKnowledgeArea.RECOMMENDATION_COOLDOWNS,
                GameKnowledgeArea.HEALTHY_VARIETY,
                GameKnowledgeArea.GOAL_DEPENDENCIES,
                GameKnowledgeArea.PREPARATION_CHECKLISTS,
                GameKnowledgeArea.CONFIDENCE_AND_EVIDENCE,
                GameKnowledgeArea.MEMBERSHIP_FILTERING,
                GameKnowledgeArea.ACCOUNT_MODE_FILTERING,
                GameKnowledgeArea.RISK_WARNINGS);

        this.coverage = Collections.unmodifiableMap(values);
    }

    public KnowledgeCoverage coverageOf(GameKnowledgeArea area)
    {
        if (area == null) return KnowledgeCoverage.SCAFFOLDED;
        return coverage.getOrDefault(area, KnowledgeCoverage.SCAFFOLDED);
    }

    public Map<GameKnowledgeArea, KnowledgeCoverage> all()
    {
        return coverage;
    }

    public List<GameKnowledgeArea> areasWith(KnowledgeCoverage level)
    {
        List<GameKnowledgeArea> result = new ArrayList<>();
        for (Map.Entry<GameKnowledgeArea, KnowledgeCoverage> entry
                : coverage.entrySet())
        {
            if (entry.getValue() == level) result.add(entry.getKey());
        }
        return Collections.unmodifiableList(result);
    }

    public List<GameKnowledgeArea> notVerified()
    {
        List<GameKnowledgeArea> result = new ArrayList<>();
        for (Map.Entry<GameKnowledgeArea, KnowledgeCoverage> entry
                : coverage.entrySet())
        {
            if (entry.getValue() != KnowledgeCoverage.VERIFIED)
            {
                result.add(entry.getKey());
            }
        }
        return Collections.unmodifiableList(result);
    }

    public boolean containsEveryDeclaredArea()
    {
        for (GameKnowledgeArea area : GameKnowledgeArea.values())
        {
            if (!coverage.containsKey(area)) return false;
        }
        return true;
    }

    private static void partial(
            EnumMap<GameKnowledgeArea, KnowledgeCoverage> values,
            GameKnowledgeArea... areas)
    {
        set(values, KnowledgeCoverage.PARTIAL, areas);
    }

    private static void verified(
            EnumMap<GameKnowledgeArea, KnowledgeCoverage> values,
            GameKnowledgeArea... areas)
    {
        set(values, KnowledgeCoverage.VERIFIED, areas);
    }

    private static void set(
            EnumMap<GameKnowledgeArea, KnowledgeCoverage> values,
            KnowledgeCoverage level,
            GameKnowledgeArea... areas)
    {
        for (GameKnowledgeArea area : areas)
        {
            values.put(area, level);
        }
    }
}
