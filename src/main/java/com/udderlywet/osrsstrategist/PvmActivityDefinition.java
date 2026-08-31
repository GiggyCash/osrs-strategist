package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** Current boss/raid identity plus Compass safety metadata. */
@Getter
public final class PvmActivityDefinition
{
    private String id;
    private String name;
    private boolean wilderness;
    private boolean raid;
    private boolean freeToPlay;
    private RiskLevel riskLevel;
    private boolean hardcoreSafeByDefault;

}
