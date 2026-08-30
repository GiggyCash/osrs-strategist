package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Evidence for one possible resolution of a plan-specific UIM slot shortfall. */
public final class UimStorageOption
{
    @Getter
    private final StorageCapability capability;
    @Getter
    private final CapabilityState itemCompatibility;
    @Getter
    private final CapabilityState capacityOrPreconditions;
    @Getter
    private final boolean requiresConstruction;
    @Getter
    private final StrategicPriority recurringInfrastructureValue;
    @Getter
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

}
