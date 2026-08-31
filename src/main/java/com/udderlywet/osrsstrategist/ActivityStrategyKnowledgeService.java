package com.udderlywet.osrsstrategist;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Applies cross-domain sourced strategy and plan-relative UIM inventory fit. */
@Singleton
public final class ActivityStrategyKnowledgeService
{
    private final ActivityStrategyKnowledgeCatalog catalog;
    private final UimInventoryResolutionService inventoryResolution;

    @Inject
    public ActivityStrategyKnowledgeService(ActivityStrategyKnowledgeCatalog catalog,
            UimInventoryResolutionService inventoryResolution)
    {
        this.catalog = catalog == null
                ? new ActivityStrategyKnowledgeCatalog() : catalog;
        this.inventoryResolution = inventoryResolution == null
                ? new UimInventoryResolutionService() : inventoryResolution;
    }

    public ActivityStrategyKnowledgeService(ActivityStrategyKnowledgeCatalog catalog)
    {
        this(catalog, new UimInventoryResolutionService());
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
                recommendation.getId(), context.accountMode());
        if (profile == null) return recommendation;

        if (context.accountMode() == AccountMode.ULTIMATE_IRONMAN
                && !fitsObservedInventory(context.data(), profile,
                        inventoryResolution))
            return null;

        StrategicValue.Builder sourced =
                StrategicValue.builder()
                        .setupReuse(profile.getSetupReuse());
        for (StrategySourceId source : profile.getSources())
            sourced.evidence("strategy-source:" + source.name());
        return recommendation.withStrategicValue(
                recommendation.getStrategicValue().merge(sourced.build()));
    }

    private static boolean fitsObservedInventory(GameData data,
            ActivityStrategyProfile profile,
            UimInventoryResolutionService inventoryResolution)
    {
        var inventory = data == null ? null : data.inventory();
        var footprint = profile.getInventoryFootprint();
        if (footprint == null) return true;
        if (inventory == null || !inventory.hasCompleteSlotObservation())
            return footprint.getMinimumPracticalFreeSlots() == 0;
        UimInventoryResolution result = inventoryResolution.resolve(data,
                footprint, false, false, java.util.Collections.emptyList());
        return result.getKind() == UimInventoryResolutionKind.USE_AS_IS;
    }
}
