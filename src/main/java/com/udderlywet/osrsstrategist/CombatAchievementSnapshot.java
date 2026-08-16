package com.udderlywet.osrsstrategist;

public final class CombatAchievementSnapshot
{
    private final int completedTasks;
    private final int earnedPoints;

    public CombatAchievementSnapshot(
            int completedTasks,
            int earnedPoints)
    {
        this.completedTasks = completedTasks;
        this.earnedPoints = earnedPoints;
    }

    public int getCompletedTasks()
    {
        return completedTasks;
    }

    public int getEarnedPoints()
    {
        return earnedPoints;
    }
}
