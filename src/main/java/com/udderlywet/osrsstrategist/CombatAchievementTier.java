package com.udderlywet.osrsstrategist;

public enum CombatAchievementTier
{
    EASY(41),
    MEDIUM(161),
    HARD(416),
    ELITE(1064),
    MASTER(1904),
    GRANDMASTER(2630);

    private final int rewardPoints;

    CombatAchievementTier(int rewardPoints)
    {
        this.rewardPoints = rewardPoints;
    }

    public int getRewardPoints()
    {
        return rewardPoints;
    }
}
