package com.udderlywet.osrsstrategist;

/** Measured properties used to decide whether a nearby detour clears value. */
public final class SmartDetourProfile
{
    private final boolean legal;
    private final RecommendationConfidence readiness;
    private final boolean ordinaryPreparationKnown;
    private final int detourMinutes;
    private final int sessionMinutes;
    private final int travelMinutesSaved;
    private final int setupMinutesSaved;
    private final int setupMinutesRequired;
    private final double goalValue;
    private final double accountValue;
    private final double resourceValue;
    private final double riskBurden;
    private final double interruptionCost;

    private SmartDetourProfile(Builder builder)
    {
        legal = builder.legal;
        readiness = builder.readiness == null
                ? RecommendationConfidence.CHECK_NEEDED : builder.readiness;
        ordinaryPreparationKnown = builder.ordinaryPreparationKnown;
        detourMinutes = Math.max(0, builder.detourMinutes);
        sessionMinutes = Math.max(0, builder.sessionMinutes);
        travelMinutesSaved = Math.max(0, builder.travelMinutesSaved);
        setupMinutesSaved = Math.max(0, builder.setupMinutesSaved);
        setupMinutesRequired = Math.max(0, builder.setupMinutesRequired);
        goalValue = unit(builder.goalValue);
        accountValue = unit(builder.accountValue);
        resourceValue = signed(builder.resourceValue);
        riskBurden = unit(builder.riskBurden);
        interruptionCost = unit(builder.interruptionCost);
    }

    public static Builder builder() { return new Builder(); }
    public boolean isLegal() { return legal; }
    public RecommendationConfidence getReadiness() { return readiness; }
    public boolean isOrdinaryPreparationKnown() { return ordinaryPreparationKnown; }
    public int getDetourMinutes() { return detourMinutes; }
    public int getSessionMinutes() { return sessionMinutes; }
    public int getTravelMinutesSaved() { return travelMinutesSaved; }
    public int getSetupMinutesSaved() { return setupMinutesSaved; }
    public int getSetupMinutesRequired() { return setupMinutesRequired; }
    public double getGoalValue() { return goalValue; }
    public double getAccountValue() { return accountValue; }
    public double getResourceValue() { return resourceValue; }
    public double getRiskBurden() { return riskBurden; }
    public double getInterruptionCost() { return interruptionCost; }

    private static double unit(double value)
    {
        return Math.max(0.0, Math.min(1.0, value));
    }
    private static double signed(double value)
    {
        return Math.max(-1.0, Math.min(1.0, value));
    }

    public static final class Builder
    {
        private boolean legal = true;
        private RecommendationConfidence readiness =
                RecommendationConfidence.CHECK_NEEDED;
        private boolean ordinaryPreparationKnown;
        private int detourMinutes;
        private int sessionMinutes;
        private int travelMinutesSaved;
        private int setupMinutesSaved;
        private int setupMinutesRequired;
        private double goalValue;
        private double accountValue;
        private double resourceValue;
        private double riskBurden;
        private double interruptionCost;

        public Builder legal(boolean value) { legal = value; return this; }
        public Builder readiness(RecommendationConfidence value) { readiness = value; return this; }
        public Builder ordinaryPreparationKnown(boolean value) { ordinaryPreparationKnown = value; return this; }
        public Builder detourMinutes(int value) { detourMinutes = value; return this; }
        public Builder sessionMinutes(int value) { sessionMinutes = value; return this; }
        public Builder travelMinutesSaved(int value) { travelMinutesSaved = value; return this; }
        public Builder setupMinutesSaved(int value) { setupMinutesSaved = value; return this; }
        public Builder setupMinutesRequired(int value) { setupMinutesRequired = value; return this; }
        public Builder goalValue(double value) { goalValue = value; return this; }
        public Builder accountValue(double value) { accountValue = value; return this; }
        public Builder resourceValue(double value) { resourceValue = value; return this; }
        public Builder riskBurden(double value) { riskBurden = value; return this; }
        public Builder interruptionCost(double value) { interruptionCost = value; return this; }
        public SmartDetourProfile build() { return new SmartDetourProfile(this); }
    }
}
