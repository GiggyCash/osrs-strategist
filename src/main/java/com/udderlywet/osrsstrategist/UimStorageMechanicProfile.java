package com.udderlywet.osrsstrategist;

/**
 * Reviewed local mechanics for one UIM retrieval/storage system. These facts
 * do not prove that the live character has access or that a particular item is
 * compatible; {@link UimCapabilityService} still requires both observations.
 */
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

    public StorageCapability getCapability() { return capability; }
    public String getLocation() { return location; }
    public String getAccessRequirements() { return accessRequirements; }
    public String getEligibleItems() { return eligibleItems; }
    public String getInsertionOrDepositRules() { return insertionOrDepositRules; }
    public String getRetrievalRules() { return retrievalRules; }
    public String getCost() { return cost; }
    public String getExpiration() { return expiration; }
    public String getSecondDeathBehavior() { return secondDeathBehavior; }
    public RiskLevel getRisk() { return risk; }
    public StrategySourceId getSource() { return source; }
    public boolean isRecommendationEligible() { return recommendationEligible; }

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
