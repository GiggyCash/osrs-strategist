package compass;

import java.util.*;
import lombok.Getter;
import lombok.Singular;

/** Typed, bounded account-value evidence used by the final decision layer. */
@Getter
public final class StrategicValue
{
    private static final StrategicValue NEUTRAL = builder().build();
    private final double accountModeFit, infrastructureValue, unlockValue,
            travelFit, resourceFit, setupReuse, sharedDependencyValue,
            riskBurden, opportunityCost;
    private final List<String> evidenceIds;

    @lombok.Builder(builderClassName = "Builder")
    private StrategicValue(double accountModeFit, double infrastructureValue,
            double unlockValue, double travelFit, double resourceFit,
            double setupReuse, double sharedDependencyValue,
            double riskBurden, double opportunityCost,
            @Singular("evidence") List<String> evidenceIds)
    {
        this.accountModeFit = signed(accountModeFit);
        this.infrastructureValue = unit(infrastructureValue);
        this.unlockValue = unit(unlockValue);
        this.travelFit = signed(travelFit);
        this.resourceFit = signed(resourceFit);
        this.setupReuse = unit(setupReuse);
        this.sharedDependencyValue = unit(sharedDependencyValue);
        this.riskBurden = unit(riskBurden);
        this.opportunityCost = unit(opportunityCost);
        this.evidenceIds = evidenceIds;
    }

    public static StrategicValue neutral() { return NEUTRAL; }

    public StrategicValue merge(StrategicValue other)
    {
        if (other == null || other == NEUTRAL) return this;
        Builder value = builder()
                .accountModeFit(stronger(accountModeFit, other.accountModeFit))
                .infrastructureValue(Math.max(infrastructureValue, other.infrastructureValue))
                .unlockValue(Math.max(unlockValue, other.unlockValue))
                .travelFit(stronger(travelFit, other.travelFit))
                .resourceFit(stronger(resourceFit, other.resourceFit))
                .setupReuse(Math.max(setupReuse, other.setupReuse))
                .sharedDependencyValue(Math.max(sharedDependencyValue, other.sharedDependencyValue))
                .riskBurden(Math.max(riskBurden, other.riskBurden))
                .opportunityCost(Math.max(opportunityCost, other.opportunityCost));
        evidenceIds.forEach(value::evidence);
        other.evidenceIds.forEach(value::evidence);
        return value.build();
    }

    public boolean hasTypedEvidence() { return !evidenceIds.isEmpty(); }

    public double scoreDelta()
    {
        return accountModeFit * 10 + infrastructureValue * 14 + unlockValue * 12
                + travelFit * 7 + resourceFit * 8 + setupReuse * 7
                + sharedDependencyValue * 10 - riskBurden * 18
                - opportunityCost * 12;
    }

    private static double unit(double value)
    {
        return Math.max(0, Math.min(1, value));
    }

    private static double signed(double value)
    {
        return Math.max(-1, Math.min(1, value));
    }

    private static double stronger(double left, double right)
    {
        return Math.abs(left) >= Math.abs(right) ? left : right;
    }
}
