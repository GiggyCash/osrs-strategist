package com.udderlywet.osrsstrategist;

/** Current boss/raid identity plus Strategist safety metadata. */
public final class PvmActivityDefinition
{
    private final String id;
    private final String name;
    private final boolean wilderness;
    private final boolean raid;
    private final boolean freeToPlay;
    private final RiskLevel riskLevel;
    private final boolean hardcoreSafeByDefault;

    public PvmActivityDefinition(String id, String name, boolean wilderness,
            boolean raid, RiskLevel riskLevel, boolean hardcoreSafeByDefault)
    {
        this(id, name, wilderness, raid, false, riskLevel, hardcoreSafeByDefault);
    }

    public PvmActivityDefinition(String id, String name, boolean wilderness,
            boolean raid, boolean freeToPlay, RiskLevel riskLevel,
            boolean hardcoreSafeByDefault)
    {
        this.id = id;
        this.name = name;
        this.wilderness = wilderness;
        this.raid = raid;
        this.freeToPlay = freeToPlay;
        this.riskLevel = riskLevel;
        this.hardcoreSafeByDefault = hardcoreSafeByDefault;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isWilderness() { return wilderness; }
    public boolean isRaid() { return raid; }
    public boolean isFreeToPlay() { return freeToPlay; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public boolean isHardcoreSafeByDefault() { return hardcoreSafeByDefault; }
}
