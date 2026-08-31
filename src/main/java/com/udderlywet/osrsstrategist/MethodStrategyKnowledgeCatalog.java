package com.udderlywet.osrsstrategist;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;

/**
 * Indexed sourced method strategy. Shared methods stay shared; material
 * account differences are represented before ranking.
 */
@Singleton
public final class MethodStrategyKnowledgeCatalog
{
    private static final Set<AccountMode> ALL_KNOWN =
            EnumSet.allOf(AccountMode.class);
    private static final Set<String> CONVENTIONAL_BANK_LOOPS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    "prayer_f2p_bones",
                    "cooking_f2p_fish", "cooking_f2p_fish_baseline",
                    "cooking_hosidius", "cooking_wines",
                    "fishing_karambwan", "mining_mlm",
                    "firemaking_f2p_logs",
                    "woodcutting_draynor_oaks",
                    "smithing_f2p_bronze", "smithing_f2p_iron",
                    "smithing_f2p_steel", "smithing_f2p_platebodies",
                    "smithing_f2p_platebody_baseline",
                    "crafting_f2p_gold_amulets", "crafting_f2p_tiaras",
                    "crafting_gems", "crafting_dhide", "fletching_bows",
                    "woodcutting_f2p_oaks", "woodcutting_f2p_willows",
                    "woodcutting_f2p_willows_baseline",
                    "herblore_low_potions", "runecraft_f2p_air",
                    "runecraft_f2p_mind", "runecraft_f2p_water",
                    "runecraft_f2p_earth", "runecraft_f2p_fire",
                    "runecraft_f2p_body", "thieving_lumbridge_people",
                    "thieving_ardy_knights", "thieving_vyres")));

    private final Map<String, java.util.List<MethodStrategyProfile>> exact =
            new HashMap<>();
    private final Map<String, MethodStrategyProfile> generated =
            new ConcurrentHashMap<>();
    private final MethodExecutionProfileCatalog executionProfiles =
            new MethodExecutionProfileCatalog();

    public MethodStrategyKnowledgeCatalog()
    {
        for (MethodStrategyProfile profile : BundledCatalogLoader.array(
                PlayerText.get("MSKC1"),
                MethodStrategyProfile[].class))
        {
            if (profile.getMethodId() == null || profile.getTier() == null)
                throw new IllegalStateException(PlayerText.get("MSKC2"));
            addExact(profile);
        }
    }

    public MethodStrategyProfile profileFor(TrainingMethod method,
            TrainingMethodMetadata metadata, AccountMode mode)
    {
        if (method == null || metadata == null || mode == null) return null;
        java.util.List<MethodStrategyProfile> specific = exact.get(
                method.getId());
        if (specific != null)
        {
            MethodStrategyProfile selected = null;
            for (MethodStrategyProfile profile : specific)
                if (profile.supports(mode)
                        && (selected == null || profile.getTier().ordinal()
                                < selected.getTier().ordinal()))
                    selected = profile;
            return selected;
        }

        boolean bankLoop = CONVENTIONAL_BANK_LOOPS.contains(method.getId());
        if (mode == AccountMode.UNKNOWN && bankLoop) return null;
        if (mode == AccountMode.ULTIMATE_IRONMAN
                && (bankLoop || !metadata.isUimFriendly())) return null;

        Set<AccountMode> modes = EnumSet.copyOf(ALL_KNOWN);
        if (!metadata.isUimFriendly() || bankLoop)
            modes.remove(AccountMode.ULTIMATE_IRONMAN);
        if (!modes.contains(mode)) return null;

        String key = mode.name() + ':' + method.getId();
        return generated.computeIfAbsent(key, ignored -> genericProfile(
                method, metadata, mode, bankLoop, modes,
                executionProfiles.forMethod(method.getId())));
    }

    private static MethodStrategyProfile genericProfile(TrainingMethod method,
            TrainingMethodMetadata metadata, AccountMode mode,
            boolean bankLoop, Set<AccountMode> modes,
            MethodExecutionProfile executionProfile)
    {
        StrategySourceId source = accountSkillSource(
                method.getSkill(), mode, metadata.isFreeToPlayAllowed());
        String reason = metadata.isSelfSourceFriendly() && mode.isIronLike()
                ? PlayerText.get("MSKC3")
                : PlayerText.get("MSKC4");
        return new MethodStrategyProfile(method.getId(),
                StrategyKnowledgeTier.VERIFIED_SHARED, modes,
                bankLoop ? MethodBankingBehavior.CONVENTIONAL_BANK_LOOP
                        : MethodBankingBehavior.NONE,
                typedFootprint(method, metadata, executionProfile),
                metadata.isSelfSourceFriendly() && mode.isIronLike() ? 0.55 : 0.35,
                reason, Collections.singletonList(source));
    }

    /**
     * Conservative family defaults use typed skill/input/setup properties.
     * Account-specific routes with materially different behavior stay in the
     * exact sourced records above; method names and IDs never change a
     * footprint.
     */
    private static MethodInventoryFootprint typedFootprint(
            TrainingMethod method, TrainingMethodMetadata metadata,
            MethodExecutionProfile executionProfile)
    {
        boolean tearsDown = method.getSetupMinutes() >= 8;
        switch (method.getSkill())
        {
            case AGILITY:
            case ATTACK:
            case STRENGTH:
            case DEFENCE:
            case HITPOINTS:
            case RANGED:
            case MAGIC:
            case SLAYER:
            case THIEVING:
                return new MethodInventoryFootprint(0, 0, 0,
                        InventoryFlow.NEUTRAL, tearsDown);
            case MINING:
            case FISHING:
            case WOODCUTTING:
                return new MethodInventoryFootprint(1, 1, 0,
                        InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS, tearsDown);
            case HUNTER:
                return new MethodInventoryFootprint(3, 2, 1,
                        InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS, tearsDown);
            default:
                break;
        }
        boolean consumesInputs = executionProfile != null
                && !executionProfile.getInputs().isEmpty();
        return new MethodInventoryFootprint(2, 1, 1,
                consumesInputs ? InventoryFlow.REPLACES_INPUTS_WITH_OUTPUTS
                        : InventoryFlow.NEUTRAL,
                tearsDown);
    }

    private static StrategySourceId accountSkillSource(
            net.runelite.api.Skill skill, AccountMode mode, boolean f2p)
    {
        if (skill == net.runelite.api.Skill.SAILING)
            return StrategySourceId.SAILING_TRAINING;
        if (skill == null || mode == null || !mode.isIronLike())
            return f2p ? StrategySourceId.F2P_SKILL_TRAINING
                    : StrategySourceId.GENERAL_SKILL_TRAINING;
        boolean uim = mode == AccountMode.ULTIMATE_IRONMAN;
        switch (skill)
        {
            case SMITHING: return uim ? StrategySourceId.UIM_SMITHING
                    : StrategySourceId.IRONMAN_SMITHING;
            case CRAFTING: return uim ? StrategySourceId.UIM_CRAFTING
                    : StrategySourceId.IRONMAN_CRAFTING;
            case HERBLORE: return uim ? StrategySourceId.UIM_HERBLORE
                    : StrategySourceId.IRONMAN_HERBLORE;
            case CONSTRUCTION: return uim ? StrategySourceId.UIM_CONSTRUCTION
                    : StrategySourceId.IRONMAN_CONSTRUCTION;
            case RUNECRAFT: return uim ? StrategySourceId.UIM_RUNECRAFT
                    : StrategySourceId.IRONMAN_RUNECRAFT;
            case PRAYER: return uim ? StrategySourceId.UIM_PRAYER
                    : StrategySourceId.IRONMAN_PRAYER;
            case FARMING: return uim ? StrategySourceId.UIM_FARMING
                    : StrategySourceId.IRONMAN_FARMING;
            case COOKING: return uim ? StrategySourceId.UIM_COOKING
                    : StrategySourceId.IRONMAN_COOKING;
            case FLETCHING: return uim ? StrategySourceId.UIM_FLETCHING
                    : StrategySourceId.IRONMAN_FLETCHING;
            case FISHING: return uim ? StrategySourceId.UIM_FISHING
                    : StrategySourceId.IRONMAN_FISHING;
            case MINING: return uim ? StrategySourceId.UIM_MINING
                    : StrategySourceId.IRONMAN_MINING;
            case WOODCUTTING: return uim ? StrategySourceId.UIM_WOODCUTTING
                    : StrategySourceId.IRONMAN_WOODCUTTING;
            case HUNTER: return uim ? StrategySourceId.UIM_HUNTER
                    : StrategySourceId.IRONMAN_HUNTER;
            case FIREMAKING: return uim ? StrategySourceId.UIM_FIREMAKING
                    : StrategySourceId.IRONMAN_FIREMAKING;
            case THIEVING: return uim ? StrategySourceId.UIM_THIEVING
                    : StrategySourceId.IRONMAN_THIEVING;
            default: return uim ? StrategySourceId.UIM_SKILL_GUIDES
                    : StrategySourceId.IRONMAN_SKILL_GUIDES;
        }
    }

    private void addExact(MethodStrategyProfile profile)
    {
        exact.computeIfAbsent(profile.getMethodId(),
                ignored -> new java.util.ArrayList<>()).add(profile);
    }
}
