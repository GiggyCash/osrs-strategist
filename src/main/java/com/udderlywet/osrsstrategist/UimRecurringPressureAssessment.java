package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** Session-local evidence that distinct UIM setups hit the same constraints. */
public final class UimRecurringPressureAssessment
{
    @Getter
    private final int distinctObservedLayouts;
    @Getter
    private final List<String> blockedFamilies;

    UimRecurringPressureAssessment(int distinctObservedLayouts,
            List<String> blockedFamilies)
    {
        this.distinctObservedLayouts = Math.max(0, distinctObservedLayouts);
        this.blockedFamilies = Collections.unmodifiableList(new ArrayList<>(
                blockedFamilies == null ? Collections.emptyList()
                        : blockedFamilies));
    }

    public boolean isRepeated()
    {
        return distinctObservedLayouts >= 2 && blockedFamilies.size() >= 2;
    }
}
