package com.udderlywet.osrsstrategist;

/**
 * Typed inputs used to decide whether an otherwise known activity is worth
 * doing now. Values are normalized properties, not activity identities.
 */
public final class ActivityValueProfile
{
    private final boolean legal;
    private final RecommendationConfidence readiness;
    private final boolean ordinaryPreparationKnown;
    private final int setupMinutes;
    private final int sessionMinutes;
    private final double goalValue;
    private final double accountValue;
    private final double setupReuseValue;
    private final double travelValue;
    private final double resourceValue;
    private final double riskBurden;
    private final double opportunityCost;

    private ActivityValueProfile(Builder builder)
    {
        legal = builder.legal;
        readiness = builder.readiness == null
                ? RecommendationConfidence.CHECK_NEEDED : builder.readiness;
        ordinaryPreparationKnown = builder.ordinaryPreparationKnown;
        setupMinutes = Math.max(0, builder.setupMinutes);
        sessionMinutes = Math.max(0, builder.sessionMinutes);
        goalValue = unit(builder.goalValue);
        accountValue = unit(builder.accountValue);
        setupReuseValue = unit(builder.setupReuseValue);
        travelValue = signedUnit(builder.travelValue);
        resourceValue = signedUnit(builder.resourceValue);
        riskBurden = unit(builder.riskBurden);
        opportunityCost = unit(builder.opportunityCost);
    }

    public static Builder builder() { return new Builder(); }

    public boolean isLegal() { return legal; }
    public RecommendationConfidence getReadiness() { return readiness; }
    public boolean isOrdinaryPreparationKnown() { return ordinaryPreparationKnown; }
    public int getSetupMinutes() { return setupMinutes; }
    public int getSessionMinutes() { return sessionMinutes; }
    public double getGoalValue() { return goalValue; }
    public double getAccountValue() { return accountValue; }
    public double getSetupReuseValue() { return setupReuseValue; }
    public double getTravelValue() { return travelValue; }
    public double getResourceValue() { return resourceValue; }
    public double getRiskBurden() { return riskBurden; }
    public double getOpportunityCost() { return opportunityCost; }

    private static double unit(double value)
    {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double signedUnit(double value)
    {
        return Math.max(-1.0, Math.min(1.0, value));
    }

    public static final class Builder
    {
        private boolean legal = true;
        private RecommendationConfidence readiness =
                RecommendationConfidence.CHECK_NEEDED;
        private boolean ordinaryPreparationKnown;
        private int setupMinutes;
        private int sessionMinutes;
        private double goalValue;
        private double accountValue;
        private double setupReuseValue;
        private double travelValue;
        private double resourceValue;
        private double riskBurden;
        private double opportunityCost;

        public Builder legal(boolean value) { legal = value; return this; }
        public Builder readiness(RecommendationConfidence value) { readiness = value; return this; }
        public Builder ordinaryPreparationKnown(boolean value) { ordinaryPreparationKnown = value; return this; }
        public Builder setupMinutes(int value) { setupMinutes = value; return this; }
        public Builder sessionMinutes(int value) { sessionMinutes = value; return this; }
        public Builder goalValue(double value) { goalValue = value; return this; }
        public Builder accountValue(double value) { accountValue = value; return this; }
        public Builder setupReuseValue(double value) { setupReuseValue = value; return this; }
        public Builder travelValue(double value) { travelValue = value; return this; }
        public Builder resourceValue(double value) { resourceValue = value; return this; }
        public Builder riskBurden(double value) { riskBurden = value; return this; }
        public Builder opportunityCost(double value) { opportunityCost = value; return this; }
        public ActivityValueProfile build() { return new ActivityValueProfile(this); }
    }
}
