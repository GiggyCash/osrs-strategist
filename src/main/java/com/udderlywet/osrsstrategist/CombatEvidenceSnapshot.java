package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

import net.runelite.api.Prayer;

/** Directly observed spellbook selector and prayer state. */
public final class CombatEvidenceSnapshot
{
    @Getter
    private final int spellbookSelector;
    @Getter
    private final Set<Prayer> activePrayers;
    @Getter
    private final boolean rigourUnlocked;
    @Getter
    private final boolean auguryUnlocked;
    @Getter
    private final boolean preserveUnlocked;

    public CombatEvidenceSnapshot(int spellbookSelector,
            Set<Prayer> activePrayers, boolean rigourUnlocked,
            boolean auguryUnlocked, boolean preserveUnlocked)
    {
        this.spellbookSelector = spellbookSelector;
        this.activePrayers = Collections.unmodifiableSet(activePrayers == null
                || activePrayers.isEmpty() ? EnumSet.noneOf(Prayer.class)
                : EnumSet.copyOf(activePrayers));
        this.rigourUnlocked = rigourUnlocked;
        this.auguryUnlocked = auguryUnlocked;
        this.preserveUnlocked = preserveUnlocked;
    }

}
