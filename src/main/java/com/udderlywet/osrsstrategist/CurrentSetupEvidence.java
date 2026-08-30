package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Optional live evidence not currently carried by StrategyDataBundle. */
public final class CurrentSetupEvidence
{
    @Getter
    private final String regionId;
    @Getter
    private final String spellbookId;

    public CurrentSetupEvidence(String regionId, String spellbookId)
    {
        this.regionId = clean(regionId);
        this.spellbookId = clean(spellbookId);
    }

    public static CurrentSetupEvidence unknown()
    {
        return new CurrentSetupEvidence(null, null);
    }

    public boolean hasRegion() { return regionId != null; }
    public boolean hasSpellbook() { return spellbookId != null; }

    private static String clean(String value)
    {
        return value == null || value.trim().isEmpty()
                ? null : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
