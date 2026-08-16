package com.udderlywet.osrsstrategist;

public final class GearUpgrade
{
    private final String slotOrPurpose;
    private final String currentItem;
    private final String nextPracticalUpgrade;
    private final String longTermUpgrade;
    private final String bisTarget;

    public GearUpgrade(
            String slotOrPurpose,
            String currentItem,
            String nextPracticalUpgrade,
            String longTermUpgrade,
            String bisTarget)
    {
        this.slotOrPurpose = slotOrPurpose;
        this.currentItem = currentItem;
        this.nextPracticalUpgrade = nextPracticalUpgrade;
        this.longTermUpgrade = longTermUpgrade;
        this.bisTarget = bisTarget;
    }

    public String getSlotOrPurpose()
    {
        return slotOrPurpose;
    }

    public String getCurrentItem()
    {
        return currentItem;
    }

    public String getNextPracticalUpgrade()
    {
        return nextPracticalUpgrade;
    }

    public String getLongTermUpgrade()
    {
        return longTermUpgrade;
    }

    public String getBisTarget()
    {
        return bisTarget;
    }
}
