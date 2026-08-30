package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Current boss/raid identity plus Compass safety metadata. */
public final class PvmActivityDefinition
{
    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final boolean wilderness;
    @Getter
    private final boolean raid;
    @Getter
    private final boolean freeToPlay;
    @Getter
    private final RiskLevel riskLevel;
    @Getter
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

}
