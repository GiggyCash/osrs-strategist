package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.runelite.api.Prayer;

/** Directly observed spellbook selector and prayer state. */
public final class CombatEvidenceSnapshot
{
    private final int spellbookSelector;
    private final Set<Prayer> activePrayers;
    private final boolean rigourUnlocked;
    private final boolean auguryUnlocked;
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

    public int getSpellbookSelector() { return spellbookSelector; }
    public Set<Prayer> getActivePrayers() { return activePrayers; }
    public boolean isRigourUnlocked() { return rigourUnlocked; }
    public boolean isAuguryUnlocked() { return auguryUnlocked; }
    public boolean isPreserveUnlocked() { return preserveUnlocked; }
}
