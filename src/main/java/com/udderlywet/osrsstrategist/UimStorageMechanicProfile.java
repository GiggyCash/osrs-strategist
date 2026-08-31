package com.udderlywet.osrsstrategist;

import lombok.RequiredArgsConstructor;
import lombok.Getter;

/**
 * Reviewed local mechanics for one UIM retrieval/storage system. These facts
 * do not prove that the live character has access or that a particular item is
 * compatible; {@link UimCapabilityService} still requires both observations.
 */
@RequiredArgsConstructor
@Getter
public final class UimStorageMechanicProfile
{
    private final StorageCapability capability;
    private final String location;
    private final String accessRequirements;
    private final String eligibleItems;
    private final String insertionOrDepositRules;
    private final String retrievalRules;
    private final String cost;
    private final String expiration;
    private final String secondDeathBehavior;
    private final RiskLevel risk;
    private final StrategySourceId source;
    private final boolean recommendationEligible;



    public boolean hasCompleteRecommendationRules()
    {
        return recommendationEligible && capability != null && source != null
                && text(location) && text(accessRequirements)
                && text(eligibleItems) && text(insertionOrDepositRules)
                && text(retrievalRules) && text(cost) && text(expiration)
                && text(secondDeathBehavior);
    }

    private static boolean text(String value)
    {
        return value != null && !value.trim().isEmpty();
    }
}
