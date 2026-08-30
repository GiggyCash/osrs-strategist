package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.inject.Singleton;

/**
 * Small indexed bridge from sourced strategy families to existing providers.
 * Providers retain live-state authority; these profiles constrain practical
 * candidate fit before the common ranking layer.
 */
@Singleton
public final class ActivityStrategyKnowledgeCatalog
{
    private static final EnumSet<AccountMode> ALL =
            EnumSet.allOf(AccountMode.class);
    private final List<ActivityStrategyProfile> profiles;

    public ActivityStrategyKnowledgeCatalog()
    {
        List<ActivityStrategyProfile> values = new ArrayList<>();
        values.add(profile("slayer:do-task", 0, 4, 0,
                InventoryFlow.NEUTRAL, 0.9,
                "Continuing the observed Slayer assignment preserves the current combat setup; task mechanics and supplies remain live-state decisions.",
                StrategySourceId.SLAYER_TRAINING,
                StrategySourceId.IRONMAN_SLAYER));
        values.add(profile("slayer:", 2, 2, 0,
                InventoryFlow.NEUTRAL, 0.45,
                "The live Slayer assignment and point state stay authoritative while reviewed task strategy informs the available decision family.",
                StrategySourceId.SLAYER_TRAINING,
                StrategySourceId.IRONMAN_SLAYER));
        values.add(profile("quest:", 3, 1, 3,
                InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS, 0.1,
                "Quest value is strategic rather than a hard prerequisite; UIM execution also needs room for required, temporary, and reward items.",
                StrategySourceId.OPTIMAL_QUEST_GUIDE,
                StrategySourceId.UIM_ITEM_MANAGEMENT));
        values.add(profile("clue:", 2, 1, 2,
                InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS, 0.25,
                "Clue interruption value depends on the observed step, its exact equipment, travel, risk, and any verified STASH—not an assumed storage state.",
                StrategySourceId.CLUE_STASH,
                StrategySourceId.UIM_ITEM_MANAGEMENT));
        values.add(profile("pvm:", 6, 5, 4,
                InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS, 0.2,
                "Encounter readiness requires an executable loadout and supply space; stats alone do not prove readiness.",
                StrategySourceId.PVM_STRATEGY,
                StrategySourceId.UIM_ITEM_MANAGEMENT));
        values.add(profile("pvm:the_gauntlet", 0, 0, 0,
                InventoryFlow.NEUTRAL, 0.85,
                "The Gauntlet supplies its temporary setup inside the activity, so a normal external loadout is not an execution requirement.",
                StrategySourceId.PVM_STRATEGY,
                StrategySourceId.UIM_ITEM_MANAGEMENT));
        values.add(profile("pvm:the_corrupted_gauntlet", 0, 0, 0,
                InventoryFlow.NEUTRAL, 0.85,
                "The Corrupted Gauntlet supplies its temporary setup inside the activity, while death risk still remains a separate account-mode decision.",
                StrategySourceId.PVM_STRATEGY,
                StrategySourceId.UIM_ITEM_MANAGEMENT));
        values.add(profile("minigame:gauntlet", 0, 0, 0,
                InventoryFlow.NEUTRAL, 0.85,
                "The Gauntlet supplies its temporary setup inside the activity, so an external bank loadout is not an execution requirement.",
                StrategySourceId.MINIGAME_GUIDES));
        values.add(profile("minigame:", 4, 2, 3,
                InventoryFlow.REPLACES_INPUTS_WITH_OUTPUTS, 0.35,
                "Minigame value is weighed against its observed requirements, setup, rewards, and inventory behavior.",
                StrategySourceId.MINIGAME_GUIDES,
                StrategySourceId.UIM_ITEM_MANAGEMENT));
        values.add(profile("upgrade:", 1, 1, 1,
                InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS, 0.3,
                "An upgrade must justify its acquisition chain, replacement horizon, and—for UIM—the slot or verified storage it will occupy.",
                StrategySourceId.PVM_STRATEGY,
                StrategySourceId.UIM_ITEM_MANAGEMENT));
        values.add(profile("prepare:infrastructure:", 6, 2, 5,
                InventoryFlow.REPLACES_INPUTS_WITH_OUTPUTS, 0.75,
                "Reusable POH infrastructure can reduce future travel and setup costs, but only observed personal-house capabilities count.",
                StrategySourceId.POH_STORAGE,
                StrategySourceId.UIM_GENERAL));
        values.add(profile("verify:poh-build-mode", 0, 0, 0,
                InventoryFlow.NEUTRAL, 0.0,
                "A build-mode scan verifies the character's own POH without inventing furniture or teammate infrastructure.",
                StrategySourceId.POH_STORAGE,
                StrategySourceId.RUNELITE_MECHANICS));
        profiles = Collections.unmodifiableList(values);
    }

    public ActivityStrategyProfile profileFor(String candidateId,
            AccountMode mode)
    {
        if (candidateId == null || mode == null) return null;
        ActivityStrategyProfile best = null;
        for (ActivityStrategyProfile profile : profiles)
        {
            if (!profile.supports(mode)
                    || !candidateId.startsWith(profile.getCandidatePrefix()))
                continue;
            if (best == null || profile.getCandidatePrefix().length()
                    > best.getCandidatePrefix().length()) best = profile;
        }
        return best;
    }

    public List<ActivityStrategyProfile> all()
    {
        return profiles;
    }

    private static ActivityStrategyProfile profile(String prefix,
            int slots, int persistent, int temporary, InventoryFlow flow,
            double setupReuse, String reason, StrategySourceId... sources)
    {
        return new ActivityStrategyProfile(prefix, ALL,
                new MethodInventoryFootprint(slots, persistent, temporary,
                        flow, setupReuse < 0.25), setupReuse, reason,
                Arrays.asList(sources));
    }
}
