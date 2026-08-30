package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
                    "smithing_f2p_bronze", "smithing_f2p_iron",
                    "smithing_f2p_steel", "smithing_f2p_platebodies",
                    "smithing_f2p_platebody_baseline",
                    "crafting_f2p_gold_amulets", "crafting_f2p_tiaras",
                    "crafting_gems", "fletching_bows",
                    "woodcutting_f2p_oaks", "woodcutting_f2p_willows",
                    "woodcutting_f2p_willows_baseline",
                    "herblore_low_potions", "runecraft_f2p_air",
                    "runecraft_f2p_mind", "runecraft_f2p_water",
                    "runecraft_f2p_earth", "runecraft_f2p_fire",
                    "runecraft_f2p_body")));

    private final Map<String, MethodStrategyProfile> exact = new HashMap<>();

    public MethodStrategyKnowledgeCatalog()
    {
        exact.put("smithing_f2p_uim_bronze", new MethodStrategyProfile(
                "smithing_f2p_uim_bronze",
                StrategyKnowledgeTier.VERIFIED_ACCOUNT_SPECIFIC,
                EnumSet.of(AccountMode.ULTIMATE_IRONMAN),
                MethodBankingBehavior.LOCAL_PROCESSING,
                new MethodInventoryFootprint(3, 2, 2,
                        InventoryFlow.REPLACES_INPUTS_WITH_OUTPUTS, false),
                0.85,
                "Nearby copper and tin make this a cheap first Smithing level without conventional banking.",
                Arrays.asList(StrategySourceId.F2P_IRONMAN_GENERAL,
                        StrategySourceId.RUNELITE_MECHANICS)));
        exact.put("cooking_f2p_uim_carried_fish", new MethodStrategyProfile(
                "cooking_f2p_uim_carried_fish",
                StrategyKnowledgeTier.VERIFIED_ACCOUNT_SPECIFIC,
                EnumSet.of(AccountMode.ULTIMATE_IRONMAN),
                MethodBankingBehavior.LOCAL_PROCESSING,
                new MethodInventoryFootprint(0, 1, 0,
                        InventoryFlow.CONSUMES_CARRIED_INPUTS, false),
                0.8,
                "Cooking carried or locally caught fish turns the current inventory into useful food without a bank setup.",
                Arrays.asList(StrategySourceId.F2P_IRONMAN_GENERAL,
                        StrategySourceId.F2P_SKILL_TRAINING)));
        exact.put("runecraft_f2p_uim_local", new MethodStrategyProfile(
                "runecraft_f2p_uim_local",
                StrategyKnowledgeTier.VERIFIED_ACCOUNT_SPECIFIC,
                EnumSet.of(AccountMode.ULTIMATE_IRONMAN),
                MethodBankingBehavior.LOCAL_PROCESSING,
                new MethodInventoryFootprint(0, 1, 0,
                        InventoryFlow.REPLACES_INPUTS_WITH_OUTPUTS, false),
                0.75,
                "Mining and immediately crafting essence is a concrete F2P UIM route that never assumes a banked essence reserve.",
                Arrays.asList(StrategySourceId.F2P_IRONMAN_GENERAL,
                        StrategySourceId.F2P_SKILL_TRAINING)));
        exact.put("crafting_charter_glass", uim("crafting_charter_glass", 4,
                InventoryFlow.REPLACES_INPUTS_WITH_OUTPUTS,
                "Charter-shop glassblowing sources and processes materials in one repeatable UIM loop."));
        exact.put("smithing_giants_foundry", sharedIronUim(
                "smithing_giants_foundry", 3,
                "Giants' Foundry stretches self-sourced metal and advances a useful permanent outfit."));
        exact.put("construction_mahogany_homes", sharedIronUim(
                "construction_mahogany_homes", 4,
                "Mahogany Homes reduces plank burn while building high-value account infrastructure."));
        exact.put("prayer_bonecrusher_passive", uim(
                "prayer_bonecrusher_passive", 0,
                InventoryFlow.NEUTRAL,
                "The carried bonecrusher converts compatible combat drops into Prayer progress without growing inventory."));
        exact.put("fishing_f2p_fly", shared(
                "fishing_f2p_fly", MethodBankingBehavior.NONE, 1,
                "Dropping the catch is a concrete low-setup route when food supply is not the current goal."));
        exact.put("mining_f2p_iron", shared(
                "mining_f2p_iron", MethodBankingBehavior.NONE, 1,
                "Power-mining iron is a concrete low-setup route when the ore is not needed by another plan."));
        exact.put("agility_rooftops", shared(
                "agility_rooftops", MethodBankingBehavior.NONE, 0,
                "Rooftop training needs little inventory and advances reusable movement progression."));
    }

    public MethodStrategyProfile profileFor(TrainingMethod method,
            TrainingMethodMetadata metadata, AccountMode mode)
    {
        if (method == null || metadata == null || mode == null) return null;
        MethodStrategyProfile specific = exact.get(method.getId());
        if (specific != null) return specific.supports(mode) ? specific : null;

        boolean bankLoop = CONVENTIONAL_BANK_LOOPS.contains(method.getId());
        if (mode == AccountMode.UNKNOWN && bankLoop) return null;
        if (mode == AccountMode.ULTIMATE_IRONMAN
                && (bankLoop || !metadata.isUimFriendly())) return null;

        Set<AccountMode> modes = EnumSet.copyOf(ALL_KNOWN);
        if (!metadata.isUimFriendly() || bankLoop)
            modes.remove(AccountMode.ULTIMATE_IRONMAN);
        if (!modes.contains(mode)) return null;

        StrategySourceId source = metadata.isFreeToPlayAllowed()
                ? StrategySourceId.F2P_SKILL_TRAINING
                : StrategySourceId.GENERAL_SKILL_TRAINING;
        String reason = metadata.isSelfSourceFriendly() && mode.isIronLike()
                ? "This method has a concrete self-sufficient resource route for the current level band."
                : "This is a reviewed practical method for the current level, session, and play style.";
        return new MethodStrategyProfile(method.getId(),
                StrategyKnowledgeTier.VERIFIED_SHARED, modes,
                bankLoop ? MethodBankingBehavior.CONVENTIONAL_BANK_LOOP
                        : MethodBankingBehavior.NONE,
                inferredFootprint(method, metadata),
                metadata.isSelfSourceFriendly() && mode.isIronLike() ? 0.55 : 0.35,
                reason, Collections.singletonList(source));
    }

    private static MethodInventoryFootprint inferredFootprint(
            TrainingMethod method, TrainingMethodMetadata metadata)
    {
        String id = method.getId() == null ? "" : method.getId();
        if (id.contains("agility") || id.contains("thieving")
                || id.contains("combat") || id.contains("slayer"))
            return MethodInventoryFootprint.lowPressure();
        if (id.contains("mining") || id.contains("fishing")
                || id.contains("woodcutting"))
            return new MethodInventoryFootprint(1, 1, 0,
                    InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS, false);
        return new MethodInventoryFootprint(2, 1, 1,
                InventoryFlow.REPLACES_INPUTS_WITH_OUTPUTS,
                method.getSetupMinutes() >= 8);
    }

    private static MethodStrategyProfile uim(String id, int slots,
            InventoryFlow flow, String reason)
    {
        return new MethodStrategyProfile(id,
                StrategyKnowledgeTier.VERIFIED_ACCOUNT_SPECIFIC,
                EnumSet.of(AccountMode.ULTIMATE_IRONMAN),
                MethodBankingBehavior.LOCAL_PROCESSING,
                new MethodInventoryFootprint(slots, 1, Math.max(0, slots - 1),
                        flow, slots >= 8), 0.8, reason,
                Arrays.asList(StrategySourceId.UIM_SKILL_GUIDES,
                        StrategySourceId.UIM_ITEM_MANAGEMENT));
    }

    private static MethodStrategyProfile sharedIronUim(String id, int slots,
            String reason)
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
                Arrays.asList(StrategySourceId.IRONMAN_SKILL_GUIDES,
                        StrategySourceId.UIM_SKILL_GUIDES));
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
}
