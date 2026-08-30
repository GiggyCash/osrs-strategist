package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
        addExact(new MethodStrategyProfile(
                "smithing_f2p_uim_bronze",
                StrategyKnowledgeTier.VERIFIED_ACCOUNT_SPECIFIC,
                EnumSet.of(AccountMode.ULTIMATE_IRONMAN),
                MethodBankingBehavior.LOCAL_PROCESSING,
                new MethodInventoryFootprint(3, 2, 2,
                        InventoryFlow.REPLACES_INPUTS_WITH_OUTPUTS, false),
                0.85,
                "Nearby copper and tin make this a cheap first Smithing level without conventional banking.",
                Arrays.asList(StrategySourceId.UIM_SMITHING,
                        StrategySourceId.F2P_IRONMAN_GENERAL,
                        StrategySourceId.RUNELITE_MECHANICS)));
        addExact(new MethodStrategyProfile(
                "cooking_f2p_uim_carried_fish",
                StrategyKnowledgeTier.VERIFIED_ACCOUNT_SPECIFIC,
                EnumSet.of(AccountMode.ULTIMATE_IRONMAN),
                MethodBankingBehavior.LOCAL_PROCESSING,
                new MethodInventoryFootprint(0, 1, 0,
                        InventoryFlow.CONSUMES_CARRIED_INPUTS, false),
                0.8,
                "Cooking carried or locally caught fish turns the current inventory into useful food without a bank setup.",
                Arrays.asList(StrategySourceId.UIM_COOKING,
                        StrategySourceId.F2P_IRONMAN_GENERAL,
                        StrategySourceId.F2P_SKILL_TRAINING)));
        addExact(new MethodStrategyProfile(
                "runecraft_f2p_uim_local",
                StrategyKnowledgeTier.VERIFIED_ACCOUNT_SPECIFIC,
                EnumSet.of(AccountMode.ULTIMATE_IRONMAN),
                MethodBankingBehavior.LOCAL_PROCESSING,
                new MethodInventoryFootprint(0, 1, 0,
                        InventoryFlow.REPLACES_INPUTS_WITH_OUTPUTS, false),
                0.75,
                "Mining and immediately crafting essence is a concrete F2P UIM route that never assumes a banked essence reserve.",
                Arrays.asList(StrategySourceId.UIM_RUNECRAFT,
                        StrategySourceId.F2P_IRONMAN_GENERAL,
                        StrategySourceId.F2P_SKILL_TRAINING)));
        addExact(uim("crafting_charter_glass", 4,
                InventoryFlow.REPLACES_INPUTS_WITH_OUTPUTS,
                "Charter-shop glassblowing sources and processes materials in one repeatable UIM loop.",
                StrategySourceId.UIM_CRAFTING));
        addExact(sharedForMain("smithing_giants_foundry", 3,
                "Giants' Foundry is a reviewed profit/resource-efficiency alternative when buying faster Smithing inputs is poor value.",
                StrategySourceId.GIANTS_FOUNDRY));
        addExact(sharedIronUim(
                "smithing_giants_foundry", 3,
                "Giants' Foundry stretches self-sourced metal and advances a useful permanent outfit.",
                StrategySourceId.IRONMAN_SMITHING,
                StrategySourceId.UIM_SMITHING));
        addExact(sharedForMain("construction_mahogany_homes", 4,
                "Mahogany Homes is a reviewed lower-cost alternative when saving tradeable planks matters more than maximum conventional speed.",
                StrategySourceId.MAHOGANY_HOMES));
        addExact(sharedIronUim(
                "construction_mahogany_homes", 4,
                "Mahogany Homes reduces plank burn while building high-value account infrastructure.",
                StrategySourceId.IRONMAN_CONSTRUCTION,
                StrategySourceId.UIM_CONSTRUCTION));
        addExact(sharedForNonUim("prayer_bonecrusher_passive", 0,
                "A charged bonecrusher can turn a verified compatible combat plan into passive Prayer progress without a separate training detour.",
                StrategySourceId.GENERAL_SKILL_TRAINING));
        addExact(uim(
                "prayer_bonecrusher_passive", 0,
                InventoryFlow.NEUTRAL,
                "The carried bonecrusher converts compatible combat drops into Prayer progress without growing inventory.",
                StrategySourceId.UIM_PRAYER));
        addExact(uimNoBank(
                "thieving_uim_lumbridge_people",
                "Pickpocketing Lumbridge men and women with monastery healing is a slow but concrete bank-free early fallback.",
                StrategySourceId.UIM_THIEVING));
        addExact(uimNoBank(
                "thieving_uim_fruit_stalls",
                "Hosidius fruit stalls provide a safe bank-free fallback whose freshly stolen fruit can be dropped without dismantling carried setup.",
                StrategySourceId.UIM_THIEVING));
        addExact(shared(
                "fishing_f2p_fly", MethodBankingBehavior.NONE, 1,
                "Dropping the catch is a concrete low-setup route when food supply is not the current goal."));
        addExact(shared(
                "mining_f2p_iron", MethodBankingBehavior.NONE, 1,
                "Power-mining iron is a concrete low-setup route when the ore is not needed by another plan."));
        addExact(shared(
                "agility_rooftops", MethodBankingBehavior.NONE, 0,
                "Rooftop training needs little inventory and advances reusable movement progression."));
        addExact(new MethodStrategyProfile(
                "sailing_salvage_small",
                StrategyKnowledgeTier.VERIFIED_SHARED, ALL_KNOWN,
                MethodBankingBehavior.NONE,
                new MethodInventoryFootprint(3, 1, 2,
                        InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS, false),
                0.45,
                "Small-shipwreck salvaging is an exact low-intensity baseline; higher wrecks require separately proven boat and water-hazard capability.",
                Arrays.asList(StrategySourceId.SAILING_TRAINING,
                        StrategySourceId.SHIPWRECK_SALVAGING)));
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
                ? "This method has a concrete self-sufficient resource route for the current level band."
                : "This is a reviewed practical method for the current level, session, and play style.";
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

    private static MethodStrategyProfile uim(String id, int slots,
            InventoryFlow flow, String reason, StrategySourceId source)
    {
        return new MethodStrategyProfile(id,
                StrategyKnowledgeTier.VERIFIED_ACCOUNT_SPECIFIC,
                EnumSet.of(AccountMode.ULTIMATE_IRONMAN),
                MethodBankingBehavior.LOCAL_PROCESSING,
                new MethodInventoryFootprint(slots, 1, Math.max(0, slots - 1),
                        flow, slots >= 8), 0.8, reason,
                Arrays.asList(source, StrategySourceId.UIM_SKILL_GUIDES,
                        StrategySourceId.UIM_ITEM_MANAGEMENT));
    }

    private static MethodStrategyProfile sharedIronUim(String id, int slots,
            String reason, StrategySourceId ironSource,
            StrategySourceId uimSource)
    {
        return new MethodStrategyProfile(id,
                StrategyKnowledgeTier.VERIFIED_ACCOUNT_SPECIFIC,
                EnumSet.of(AccountMode.IRONMAN, AccountMode.GROUP_IRONMAN,
                        AccountMode.UNRANKED_GROUP_IRONMAN,
                        AccountMode.HARDCORE_IRONMAN,
                        AccountMode.HARDCORE_GROUP_IRONMAN,
                        AccountMode.ULTIMATE_IRONMAN),
                MethodBankingBehavior.LOCAL_PROCESSING,
                new MethodInventoryFootprint(slots, 2,
                        Math.max(0, slots - 2),
                        InventoryFlow.REPLACES_INPUTS_WITH_OUTPUTS, false),
                0.75, reason,
                Arrays.asList(ironSource, uimSource,
                        StrategySourceId.IRONMAN_SKILL_GUIDES,
                        StrategySourceId.UIM_SKILL_GUIDES));
    }

    private static MethodStrategyProfile sharedForMain(String id, int slots,
            String reason, StrategySourceId source)
    {
        return new MethodStrategyProfile(id,
                StrategyKnowledgeTier.VERIFIED_SHARED,
                EnumSet.of(AccountMode.MAIN), MethodBankingBehavior.NONE,
                new MethodInventoryFootprint(slots, Math.min(2, slots),
                        Math.max(0, slots - 2),
                        InventoryFlow.REPLACES_INPUTS_WITH_OUTPUTS, false),
                0.45, reason, Arrays.asList(source,
                        StrategySourceId.GENERAL_SKILL_TRAINING));
    }

    private static MethodStrategyProfile sharedForNonUim(String id, int slots,
            String reason, StrategySourceId source)
    {
        EnumSet<AccountMode> modes = EnumSet.allOf(AccountMode.class);
        modes.remove(AccountMode.ULTIMATE_IRONMAN);
        return new MethodStrategyProfile(id,
                StrategyKnowledgeTier.VERIFIED_SHARED, modes,
                MethodBankingBehavior.NONE,
                new MethodInventoryFootprint(slots, slots > 0 ? 1 : 0, 0,
                        InventoryFlow.NEUTRAL, false), 0.45, reason,
                Collections.singletonList(source));
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

    private static MethodStrategyProfile uimNoBank(String id, String reason,
            StrategySourceId source)
    {
        return new MethodStrategyProfile(id,
                StrategyKnowledgeTier.VERIFIED_ACCOUNT_SPECIFIC,
                EnumSet.of(AccountMode.ULTIMATE_IRONMAN),
                MethodBankingBehavior.NONE,
                MethodInventoryFootprint.lowPressure(), 0.35, reason,
                Arrays.asList(source, StrategySourceId.UIM_ITEM_MANAGEMENT));
    }

    private static MethodStrategyProfile shared(String id,
            MethodBankingBehavior banking, int slots, String reason)
    {
        return new MethodStrategyProfile(id,
                StrategyKnowledgeTier.VERIFIED_SHARED, ALL_KNOWN, banking,
                new MethodInventoryFootprint(slots, slots > 0 ? 1 : 0, 0,
                        InventoryFlow.NEUTRAL, false), 0.55, reason,
                Collections.singletonList(StrategySourceId.GENERAL_SKILL_TRAINING));
    }

    private void addExact(MethodStrategyProfile profile)
    {
        exact.computeIfAbsent(profile.getMethodId(),
                ignored -> new java.util.ArrayList<>()).add(profile);
    }
}
