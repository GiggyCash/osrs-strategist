package com.udderlywet.osrsstrategist;

public enum GoalType
{
    AUTOMATIC,
    MAX,
    QUEST_CAPE,
    BARROWS_GLOVES,
    PRIFDDINAS,
    BOWFA,
    INFERNAL_CAPE,
    DIARY_CAPE,
    ELITE_COMBAT_ACHIEVEMENTS,
    RAID_READY,
    TOTAL_2000,
    SLAYER_85,
    BASE_70S,
    GEAR_TARGET,
    CUSTOM;

    @Override
    public String toString()
    {
        if (this == AUTOMATIC) return "Automatic";
        if (this == BOWFA) return "Bowfa";
        String lower = name().toLowerCase(java.util.Locale.ROOT)
                .replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
