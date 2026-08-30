package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Session-local evidence that distinct UIM setups hit the same constraints. */
public final class UimRecurringPressureAssessment
{
    private final int distinctObservedLayouts;
    private final List<String> blockedFamilies;

    UimRecurringPressureAssessment(int distinctObservedLayouts,
            List<String> blockedFamilies)
    {
        this.distinctObservedLayouts = Math.max(0, distinctObservedLayouts);
        this.blockedFamilies = Collections.unmodifiableList(new ArrayList<>(
                blockedFamilies == null ? Collections.emptyList()
                        : blockedFamilies));
    }

    public int getDistinctObservedLayouts() { return distinctObservedLayouts; }
    public List<String> getBlockedFamilies() { return blockedFamilies; }
    public boolean isRepeated()
    {
        return distinctObservedLayouts >= 2 && blockedFamilies.size() >= 2;
    }
}
