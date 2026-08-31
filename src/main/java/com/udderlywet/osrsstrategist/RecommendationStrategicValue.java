package com.udderlywet.osrsstrategist;

import java.util.*;

/**
 * Typed strategic properties attached by candidate producers. Player-facing
 * wording and method IDs must not change these values.
 */
public final class RecommendationStrategicValue
{
    private static final RecommendationStrategicValue NEUTRAL =
            new Builder().build();

    private final double accountModeFit;
    private final double infrastructureValue;
    private final double unlockValue;
    private final double travelFit;
    private final double resourceFit;
    private final double setupReuse;
    private final double sharedDependencyValue;
    private final double riskBurden;
    private final double opportunityCost;
    private final List<String> evidenceIds;

    private RecommendationStrategicValue(Builder builder)
    {
        accountModeFit = signed(builder.accountModeFit);
        infrastructureValue = unit(builder.infrastructureValue);
        unlockValue = unit(builder.unlockValue);
        travelFit = signed(builder.travelFit);
        resourceFit = signed(builder.resourceFit);
        setupReuse = unit(builder.setupReuse);
        sharedDependencyValue = unit(builder.sharedDependencyValue);
        riskBurden = unit(builder.riskBurden);
        opportunityCost = unit(builder.opportunityCost);
        evidenceIds = Collections.unmodifiableList(
                new ArrayList<>(builder.evidenceIds));
    }

    public static RecommendationStrategicValue neutral() { return NEUTRAL; }
    public static Builder builder() { return new Builder(); }

    public RecommendationStrategicValue merge(RecommendationStrategicValue other)
    {
        if (other == null || other == NEUTRAL) return this;
        Builder builder = builder()
                .accountModeFit(strongerSigned(accountModeFit,
                        other.accountModeFit))
                .infrastructureValue(Math.max(infrastructureValue,
                        other.infrastructureValue))
                .unlockValue(Math.max(unlockValue, other.unlockValue))
                .travelFit(strongerSigned(travelFit, other.travelFit))
                .resourceFit(strongerSigned(resourceFit, other.resourceFit))
                .setupReuse(Math.max(setupReuse, other.setupReuse))
                .sharedDependencyValue(Math.max(sharedDependencyValue,
                        other.sharedDependencyValue))
                .riskBurden(Math.max(riskBurden, other.riskBurden))
                .opportunityCost(Math.max(opportunityCost,
                        other.opportunityCost));
        for (String id : evidenceIds) builder.evidence(id);
        for (String id : other.evidenceIds) builder.evidence(id);
        return builder.build();
    }

    public boolean hasTypedEvidence() { return !evidenceIds.isEmpty(); }

    /** Bounded contribution at the common final decision layer. */
    public double scoreDelta()
    {
        return accountModeFit * 10.0
                + infrastructureValue * 14.0
                + unlockValue * 12.0
                + travelFit * 7.0
                + resourceFit * 8.0
                + setupReuse * 7.0
                + sharedDependencyValue * 10.0
                - riskBurden * 18.0
                - opportunityCost * 12.0;
    }

    public double getAccountModeFit() { return accountModeFit; }
    public double getInfrastructureValue() { return infrastructureValue; }
    public double getUnlockValue() { return unlockValue; }
    public double getTravelFit() { return travelFit; }
    public double getResourceFit() { return resourceFit; }
    public double getSetupReuse() { return setupReuse; }
    public double getSharedDependencyValue() { return sharedDependencyValue; }
    public double getRiskBurden() { return riskBurden; }
    public double getOpportunityCost() { return opportunityCost; }
    public List<String> getEvidenceIds() { return evidenceIds; }

    private static double unit(double value)
    {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double signed(double value)
    {
        return Math.max(-1.0, Math.min(1.0, value));
    }

    private static double strongerSigned(double left, double right)
    {
        return Math.abs(left) >= Math.abs(right) ? left : right;
    }

    public static final class Builder
    {
        private double accountModeFit;
        private double infrastructureValue;
        private double unlockValue;
        private double travelFit;
        private double resourceFit;
        private double setupReuse;
        private double sharedDependencyValue;
        private double riskBurden;
        private double opportunityCost;
        private final List<String> evidenceIds = new ArrayList<>();

        public Builder accountModeFit(double value) { accountModeFit = value; return this; }
        public Builder infrastructureValue(double value) { infrastructureValue = value; return this; }
        public Builder unlockValue(double value) { unlockValue = value; return this; }
        public Builder travelFit(double value) { travelFit = value; return this; }
        public Builder resourceFit(double value) { resourceFit = value; return this; }
        public Builder setupReuse(double value) { setupReuse = value; return this; }
        public Builder sharedDependencyValue(double value) { sharedDependencyValue = value; return this; }
        public Builder riskBurden(double value) { riskBurden = value; return this; }
        public Builder opportunityCost(double value) { opportunityCost = value; return this; }
        public Builder evidence(String id)
        {
            if (id != null && !id.trim().isEmpty()) evidenceIds.add(id.trim());
            return this;
        }
        public RecommendationStrategicValue build()
        {
            return new RecommendationStrategicValue(this);
        }
    }
}
