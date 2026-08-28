package com.udderlywet.osrsstrategist;

/** Optional live evidence not currently carried by StrategyDataBundle. */
public final class CurrentSetupEvidence
{
    private final String regionId;
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

    public String getRegionId() { return regionId; }
    public String getSpellbookId() { return spellbookId; }
    public boolean hasRegion() { return regionId != null; }
    public boolean hasSpellbook() { return spellbookId != null; }

    private static String clean(String value)
    {
        return value == null || value.trim().isEmpty()
                ? null : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
