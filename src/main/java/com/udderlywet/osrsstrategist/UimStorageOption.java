package com.udderlywet.osrsstrategist;

import lombok.RequiredArgsConstructor;
import lombok.Getter;

/** Evidence for one possible resolution of a plan-specific UIM slot shortfall. */
@RequiredArgsConstructor
@Getter
public final class UimStorageOption
{
    private final StorageCapability capability;
    private final CapabilityState itemCompatibility;
    private final CapabilityState capacityOrPreconditions;
    private final boolean requiresConstruction;
    private final StrategicPriority recurringInfrastructureValue;
    private final boolean majorProgressionTransition;


}
