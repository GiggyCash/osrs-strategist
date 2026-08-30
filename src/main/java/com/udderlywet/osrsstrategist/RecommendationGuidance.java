package com.udderlywet.osrsstrategist;

import lombok.Getter;

/**
 * Account-specific instructions attached to a ranked recommendation.
 *
 * <p>The training-method catalog describes a route in general. This object
 * turns that route into concrete instructions for the current milestone and
 * the supplies Compass has actually observed.</p>
 */
public final class RecommendationGuidance
{
    @Getter
    private final String action;
    @Getter
    private final String supplies;
    @Getter
    private final String location;
    @Getter
    private final String progress;
    @Getter
    private final String note;
    @Getter
    private final MethodBankingBehavior bankingBehavior;
    @Getter
    private final UimStorageDecision storageDecision;
    @Getter
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







    public StorageCapability getStorageCapability()
    {
        return storageDecision == null ? null
                : storageDecision.getCapability();
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
