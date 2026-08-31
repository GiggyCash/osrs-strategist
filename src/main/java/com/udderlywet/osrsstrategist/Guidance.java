package com.udderlywet.osrsstrategist;

import lombok.Getter;

/**
 * Account-specific instructions attached to a ranked recommendation.
 *
 * <p>The training-method catalog describes a route in general. This object
 * turns that route into concrete instructions for the current milestone and
 * the supplies Compass has actually observed.</p>
 */
@Getter
public final class Guidance
{
    private final String action;
    private final String supplies;
    private final String location;
    private final String progress;
    private final String note;
    private final MethodBankingBehavior bankingBehavior;
    private final UimStorageDecision storageDecision;
    private final RecommendationRiskDisclosure riskDisclosure;

    public Guidance(
            String action,
            String supplies,
            String location,
            String note)
    {
        this(action, supplies, location, null, note,
                MethodBankingBehavior.UNKNOWN, null, null);
    }

    public Guidance(
            String action,
            String supplies,
            String location,
            String note,
            MethodBankingBehavior bankingBehavior)
    {
        this(action, supplies, location, null, note, bankingBehavior, null, null);
    }

    public Guidance(
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

    private Guidance(
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



    public Guidance withBankingBehavior(
            MethodBankingBehavior value)
    {
        return new Guidance(action, supplies, location, progress,
                note,
                value, storageDecision, riskDisclosure);
    }

    public Guidance withStorageDecision(
            UimStorageDecision decision,
            RecommendationRiskDisclosure disclosure)
    {
        return new Guidance(action, supplies, location, progress,
                note,
                bankingBehavior, decision, disclosure);
    }

    public Guidance withProgress(String value)
    {
        return new Guidance(action, supplies, location, value,
                note, bankingBehavior, storageDecision, riskDisclosure);
    }
}
