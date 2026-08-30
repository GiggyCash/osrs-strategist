package com.udderlywet.osrsstrategist;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Applies cross-domain sourced strategy and plan-relative UIM inventory fit. */
@Singleton
public final class ActivityStrategyKnowledgeService
{
    private final ActivityStrategyKnowledgeCatalog catalog;

    @Inject
    public ActivityStrategyKnowledgeService(ActivityStrategyKnowledgeCatalog catalog)
    {
        this.catalog = catalog == null
                ? new ActivityStrategyKnowledgeCatalog() : catalog;
    }

    public ActivityStrategyKnowledgeService()
    {
        this(new ActivityStrategyKnowledgeCatalog());
    }

    /** Returns null when exact live inventory proves the plan cannot fit. */
    public Recommendation attach(Recommendation recommendation,
            StrategyContext context)
    {
        if (recommendation == null || context == null) return recommendation;
        ActivityStrategyProfile profile = catalog.profileFor(
                recommendation.getId(), context.getAccountMode());
        if (profile == null) return recommendation;

        if (context.getAccountMode() == AccountMode.ULTIMATE_IRONMAN
                && !fitsObservedInventory(context.getData(), profile))
            return null;

        RecommendationStrategicValue.Builder sourced =
                RecommendationStrategicValue.builder()
                        .setupReuse(profile.getSetupReuse());
        for (StrategySourceId source : profile.getSources())
            sourced.evidence("strategy-source:" + source.name());
        return recommendation.withStrategicValue(
                recommendation.getStrategicValue().merge(sourced.build()));
    }

    private static boolean fitsObservedInventory(StrategyDataBundle data,
            ActivityStrategyProfile profile)
    {
        InventorySnapshot inventory = data == null ? null : data.getInventory();
        if (inventory == null || !inventory.hasCompleteSlotObservation())
            return true;
        int free = Math.max(0, 28
                - UimSetupCostService.occupiedInventorySlots(inventory));
        MethodInventoryFootprint footprint = profile.getInventoryFootprint();
        return footprint == null
                || free >= footprint.getMinimumPracticalFreeSlots();
    }
}
