package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Property-level explanation of an infrastructure value assessment. */
@Getter
public final class InfrastructureValueContribution
{
    private final InfrastructureBenefit benefit;
    private final AccountStrategicDimension dimension;
    private final StrategicPriority accountPriority;
    private final StrategicPriority milestoneUtility;
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
