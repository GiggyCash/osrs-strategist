package com.udderlywet.osrsstrategist;

/** Current point-threshold reward tiers for Combat Achievements. */
public enum CombatAchievementTier
{
    EASY(41),
    MEDIUM(161),
    HARD(416),
    ELITE(1064),
    MASTER(1904),
    GRANDMASTER(2630);

    private final int pointsRequired;

    CombatAchievementTier(int pointsRequired)
    {
        this.pointsRequired = pointsRequired;
    }

    public int getPointsRequired()
    {
        return pointsRequired;
    }
}
