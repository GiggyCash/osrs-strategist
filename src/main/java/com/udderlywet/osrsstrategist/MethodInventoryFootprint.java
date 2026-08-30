package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Plan-relative inventory requirements; deliberately avoids fake precision. */
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

    public MethodInventoryFootprint(int minimumPracticalFreeSlots,
            int persistentRequiredSlots, int temporarySlots,
            InventoryFlow flow, boolean tearsDownCurrentSetup)
    {
        this.minimumPracticalFreeSlots = Math.max(0,
                minimumPracticalFreeSlots);
        this.persistentRequiredSlots = Math.max(0, persistentRequiredSlots);
        this.temporarySlots = Math.max(0, temporarySlots);
        this.flow = flow == null ? InventoryFlow.NEUTRAL : flow;
        this.tearsDownCurrentSetup = tearsDownCurrentSetup;
    }

    public static MethodInventoryFootprint lowPressure()
    {
        return new MethodInventoryFootprint(0, 0, 0,
                InventoryFlow.NEUTRAL, false);
    }

    public boolean tearsDownCurrentSetup() { return tearsDownCurrentSetup; }
}
