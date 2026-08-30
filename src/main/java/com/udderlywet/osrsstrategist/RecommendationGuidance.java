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
    private final String note;
    private final MethodBankingBehavior bankingBehavior;

    public RecommendationGuidance(
            String action,
            String supplies,
            String location,
            String note)
    {
        this(action, supplies, location, note,
                MethodBankingBehavior.UNKNOWN);
    }

    public RecommendationGuidance(
            String action,
            String supplies,
            String location,
            String note,
            MethodBankingBehavior bankingBehavior)
    {
        this.action = action;
        this.supplies = supplies;
        this.location = location;
        this.note = note;
        this.bankingBehavior = bankingBehavior == null
                ? MethodBankingBehavior.UNKNOWN : bankingBehavior;
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

    public MethodBankingBehavior getBankingBehavior()
    {
        return bankingBehavior;
    }

    public RecommendationGuidance withBankingBehavior(
            MethodBankingBehavior value)
    {
        return new RecommendationGuidance(action, supplies, location, note,
                value);
    }
}
