package com.udderlywet.osrsstrategist;

import lombok.RequiredArgsConstructor;
import lombok.Getter;

/** Plan-relative inventory requirements; deliberately avoids fake precision. */
@RequiredArgsConstructor
public final class MethodInventoryFootprint
{
    @Getter
    private final int minimumPracticalFreeSlots;
    @Getter
    private final int persistentRequiredSlots;
    @Getter
    private final int temporarySlots;
    @Getter
    private final InventoryFlow flow;
    private final boolean tearsDownCurrentSetup;


    public static MethodInventoryFootprint lowPressure()
    {
        return new MethodInventoryFootprint(0, 0, 0,
                InventoryFlow.NEUTRAL, false);
    }

    public boolean tearsDownCurrentSetup() { return tearsDownCurrentSetup; }
}
