package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Property-level explanation of an infrastructure value assessment. */
public final class InfrastructureValueContribution
{
    @Getter
    private final InfrastructureBenefit benefit;
    @Getter
    private final AccountStrategicDimension dimension;
    @Getter
    private final StrategicPriority accountPriority;
    @Getter
    private final StrategicPriority milestoneUtility;
    @Getter
    private final StrategicPriority effectivePriority;

    InfrastructureValueContribution(InfrastructureBenefit benefit,
            StrategicPriority accountPriority,
            StrategicPriority milestoneUtility)
    {
        this.benefit = benefit;
        this.dimension = benefit.getDimension();
        this.accountPriority = accountPriority;
        this.milestoneUtility = milestoneUtility;
        this.effectivePriority = StrategicPriority.lowerOf(accountPriority,
                milestoneUtility);
    }

}
