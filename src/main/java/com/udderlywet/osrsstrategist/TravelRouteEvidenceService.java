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
        if (routeId == null || context == null || context.getData() == null)
            return false;
        StrategyDataBundle data = context.getData();
        if (data.getTransport() != null
                && data.getTransport().hasVerifiedRoute(routeId)) return true;
        if (data.getAccount() == null
                || data.getAccount().getMembershipStatus() != MembershipStatus.P2P)
            return false;
        TravelRouteEvidenceDefinition definition = catalog.get(routeId);
        if (definition == null) return false;
        if (definition.getRequiredCompletedQuest() != null
                && (data.getQuests() == null
                    || data.getQuests().statusOf(
                            definition.getRequiredCompletedQuest())
                        != QuestStatus.COMPLETE)) return false;
        ObservedItemIndex items = new ObservedItemIndex(data,
                context.isUseGroupStorage());
        for (String required : definition.getRequiredItems())
            if (!items.has(required)) return false;
        return true;
    }
}
