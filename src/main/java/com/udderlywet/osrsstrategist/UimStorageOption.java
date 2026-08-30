package com.udderlywet.osrsstrategist;

/** Evidence for one possible resolution of a plan-specific UIM slot shortfall. */
public final class UimStorageOption
{
    private final StorageCapability capability;
    private final CapabilityState itemCompatibility;
    private final CapabilityState capacityOrPreconditions;
    private final boolean requiresConstruction;
    private final StrategicPriority recurringInfrastructureValue;
    private final boolean majorProgressionTransition;

    public UimStorageOption(StorageCapability capability,
            CapabilityState itemCompatibility,
            CapabilityState capacityOrPreconditions,
            boolean requiresConstruction,
            StrategicPriority recurringInfrastructureValue,
            boolean majorProgressionTransition)
    {
        this.capability = capability;
        this.itemCompatibility = itemCompatibility == null
                ? CapabilityState.UNKNOWN : itemCompatibility;
        this.capacityOrPreconditions = capacityOrPreconditions == null
                ? CapabilityState.UNKNOWN : capacityOrPreconditions;
        this.requiresConstruction = requiresConstruction;
        this.recurringInfrastructureValue = recurringInfrastructureValue == null
                ? StrategicPriority.NONE : recurringInfrastructureValue;
        this.majorProgressionTransition = majorProgressionTransition;
    }

    public StorageCapability getCapability() { return capability; }
    public CapabilityState getItemCompatibility() { return itemCompatibility; }
    public CapabilityState getCapacityOrPreconditions()
    {
        return capacityOrPreconditions;
    }
    public boolean isRequiresConstruction() { return requiresConstruction; }
    public StrategicPriority getRecurringInfrastructureValue()
    {
        return recurringInfrastructureValue;
    }
    public boolean isMajorProgressionTransition()
    {
        return majorProgressionTransition;
    }
}
