package com.udderlywet.osrsstrategist;

/**
 * Account-specific instructions attached to a ranked recommendation.
 *
 * <p>The training-method catalog describes a route in general. This object
 * turns that route into concrete instructions for the current milestone and
 * the supplies Compass has actually observed.</p>
 */
public final class RecommendationGuidance
{
    private final String action;
    private final String supplies;
    private final String location;
    private final String progress;
    private final String note;
    private final MethodBankingBehavior bankingBehavior;
    private final UimStorageDecision storageDecision;
    private final RecommendationRiskDisclosure riskDisclosure;

    public RecommendationGuidance(
            String action,
            String supplies,
            String location,
            String note)
    {
        this(action, supplies, location, null, note,
                MethodBankingBehavior.UNKNOWN, null, null);
    }

    public RecommendationGuidance(
            String action,
            String supplies,
            String location,
            String note,
            MethodBankingBehavior bankingBehavior)
    {
        this(action, supplies, location, null, note, bankingBehavior, null, null);
    }

    public RecommendationGuidance(
            String action,
            String supplies,
            String location,
            String note,
            MethodBankingBehavior bankingBehavior,
            UimStorageDecision storageDecision,
            RecommendationRiskDisclosure riskDisclosure)
    {
        this(action, supplies, location, null, note, bankingBehavior,
                storageDecision, riskDisclosure);
    }

    private RecommendationGuidance(
            String action,
            String supplies,
            String location,
            String progress,
            String note,
            MethodBankingBehavior bankingBehavior,
            UimStorageDecision storageDecision,
            RecommendationRiskDisclosure riskDisclosure)
    {
        this.action = action;
        this.supplies = supplies;
        this.location = location;
        this.progress = progress;
        this.note = note;
        this.bankingBehavior = bankingBehavior == null
                ? MethodBankingBehavior.UNKNOWN : bankingBehavior;
        this.storageDecision = storageDecision;
        this.riskDisclosure = riskDisclosure;
    }

    public String getAction()
    {
        return action;
    }

    public String getSupplies()
    {
        return supplies;
    }

    public String getLocation()
    {
        return location;
    }

    public String getNote()
    {
        return note;
    }

    public String getProgress()
    {
        return progress;
    }

    public MethodBankingBehavior getBankingBehavior()
    {
        return bankingBehavior;
    }

    public StorageCapability getStorageCapability()
    {
        return storageDecision == null ? null
                : storageDecision.getCapability();
    }

    public UimStorageDecision getStorageDecision()
    {
        return storageDecision;
    }

    public RecommendationRiskDisclosure getRiskDisclosure()
    {
        return riskDisclosure;
    }

    public RecommendationGuidance withBankingBehavior(
            MethodBankingBehavior value)
    {
        return new RecommendationGuidance(action, supplies, location, progress,
                note,
                value, storageDecision, riskDisclosure);
    }

    public RecommendationGuidance withStorageDecision(
            UimStorageDecision decision,
            RecommendationRiskDisclosure disclosure)
    {
        return new RecommendationGuidance(action, supplies, location, progress,
                note,
                bankingBehavior, decision, disclosure);
    }

    public RecommendationGuidance withProgress(String value)
    {
        return new RecommendationGuidance(action, supplies, location, value,
                note, bankingBehavior, storageDecision, riskDisclosure);
    }
}
