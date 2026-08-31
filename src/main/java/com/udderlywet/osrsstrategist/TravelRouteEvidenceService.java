package com.udderlywet.osrsstrategist;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Proves exact routes from an observation or all typed deterministic gates. */
@Singleton
public final class TravelRouteEvidenceService
{
    private final TravelRouteEvidenceCatalog catalog;

    @Inject
    public TravelRouteEvidenceService(TravelRouteEvidenceCatalog catalog)
    {
        this.catalog = catalog;
    }

    public TravelRouteEvidenceService()
    {
        this(new TravelRouteEvidenceCatalog());
    }

    public boolean verified(String routeId, StrategyContext context)
    {
        if (routeId == null || context == null || context.data() == null)
            return false;
        var data = context.data();
        if (data.transport() != null
                && data.transport().hasVerifiedRoute(routeId)) return true;
        if (data.account() == null
                || data.account().getMembershipStatus() != MembershipStatus.P2P)
            return false;
        var definition = catalog.get(routeId);
        if (definition == null) return false;
        if (definition.getRequiredCompletedQuest() != null
                && (data.quests() == null
                    || data.quests().statusOf(
                            definition.getRequiredCompletedQuest())
                        != QuestStatus.COMPLETE)) return false;
        ItemIndex items = new ItemIndex(data,
                context.isUseGroupStorage());
        for (String required : definition.getRequiredItems())
            if (!items.has(required)) return false;
        return true;
    }
}
