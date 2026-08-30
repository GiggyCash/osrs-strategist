package com.udderlywet.osrsstrategist;

/** Plan-relative inventory requirements; deliberately avoids fake precision. */
public final class MethodInventoryFootprint
{
    private final int minimumPracticalFreeSlots;
    private final int persistentRequiredSlots;
    private final int temporarySlots;
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

    public int getMinimumPracticalFreeSlots() { return minimumPracticalFreeSlots; }
    public int getPersistentRequiredSlots() { return persistentRequiredSlots; }
    public int getTemporarySlots() { return temporarySlots; }
    public InventoryFlow getFlow() { return flow; }
    public boolean tearsDownCurrentSetup() { return tearsDownCurrentSetup; }
}
