package com.udderlywet.osrsstrategist;

import lombok.Getter;

/**
 * Reviewed local mechanics for one UIM retrieval/storage system. These facts
 * do not prove that the live character has access or that a particular item is
 * compatible; {@link UimCapabilityService} still requires both observations.
 */
public final class UimStorageMechanicProfile
{
    @Getter
    private final StorageCapability capability;
    @Getter
    private final String location;
    @Getter
    private final String accessRequirements;
    @Getter
    private final String eligibleItems;
    @Getter
    private final String insertionOrDepositRules;
    @Getter
    private final String retrievalRules;
    @Getter
    private final String cost;
    @Getter
    private final String expiration;
    @Getter
    private final String secondDeathBehavior;
    @Getter
    private final RiskLevel risk;
    @Getter
    private final StrategySourceId source;
    @Getter
    private final boolean recommendationEligible;

    public UimStorageMechanicProfile(StorageCapability capability,
            String location, String accessRequirements, String eligibleItems,
            String insertionOrDepositRules, String retrievalRules, String cost,
            String expiration, String secondDeathBehavior, RiskLevel risk,
            StrategySourceId source, boolean recommendationEligible)
    {
        this.capability = capability;
        this.location = location;
        this.accessRequirements = accessRequirements;
        this.eligibleItems = eligibleItems;
        this.insertionOrDepositRules = insertionOrDepositRules;
        this.retrievalRules = retrievalRules;
        this.cost = cost;
        this.expiration = expiration;
        this.secondDeathBehavior = secondDeathBehavior;
        this.risk = risk == null ? RiskLevel.IRREVERSIBLE : risk;
        this.source = source;
        this.recommendationEligible = recommendationEligible;
    }


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
