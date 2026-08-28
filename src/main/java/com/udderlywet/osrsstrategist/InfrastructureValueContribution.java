package com.udderlywet.osrsstrategist;

/** Property-level explanation of an infrastructure value assessment. */
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

    public InfrastructureBenefit getBenefit() { return benefit; }
    public AccountStrategicDimension getDimension() { return dimension; }
    public StrategicPriority getAccountPriority() { return accountPriority; }
    public StrategicPriority getMilestoneUtility() { return milestoneUtility; }
    public StrategicPriority getEffectivePriority() { return effectivePriority; }
}
