package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class CombatAchievementSnapshot
{
    private final int completedTasks;
    private final int earnedPoints;
    private final Set<CombatAchievementTier> completedRewardTiers;

    public CombatAchievementSnapshot(
            int completedTasks,
            int earnedPoints)
    {
        this(completedTasks, earnedPoints,
                Collections.emptySet());
    }

    public CombatAchievementSnapshot(
            int completedTasks,
            int earnedPoints,
            Set<CombatAchievementTier> completedRewardTiers)
    {
        this.completedTasks = Math.max(0, completedTasks);
        this.earnedPoints = Math.max(0, earnedPoints);
        EnumSet<CombatAchievementTier> tiers = EnumSet.noneOf(CombatAchievementTier.class);
        if (completedRewardTiers != null) tiers.addAll(completedRewardTiers);
        this.completedRewardTiers = Collections.unmodifiableSet(tiers);
    }

    public int getCompletedTasks()
    {
        return completedTasks;
    }

    public int getEarnedPoints()
    {
        return earnedPoints;
    }

    public Set<CombatAchievementTier> getCompletedRewardTiers()
    {
        return completedRewardTiers;
    }

    public boolean isRewardTierComplete(CombatAchievementTier tier)
    {
        return completedRewardTiers.contains(tier);
    }

    public CombatAchievementTier nextRewardTier()
    {
        for (CombatAchievementTier tier : CombatAchievementTier.values())
            if (!completedRewardTiers.contains(tier)) return tier;
        return null;
    }
}
