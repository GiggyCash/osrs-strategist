package com.udderlywet.osrsstrategist;

import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** The skill checkpoint currently being executed by the active plan. */
public final class ProgressTarget
{
    private final String activityId;
    private final String methodId;
    private final Skill skill;
    private final int targetLevel;
    private final int targetXp;

    public ProgressTarget(
            String activityId,
            String methodId,
            Skill skill,
            int targetLevel)
    {
        if (skill == null || targetLevel < 2 || targetLevel > 126)
        {
            throw new IllegalArgumentException("Invalid skill target");
        }
        this.activityId = activityId;
        this.methodId = methodId;
        this.skill = skill;
        this.targetLevel = targetLevel;
        this.targetXp = Experience.getXpForLevel(targetLevel);
    }

    public String getActivityId() { return activityId; }
    public String getMethodId() { return methodId; }
    public Skill getSkill() { return skill; }
    public int getTargetLevel() { return targetLevel; }
    public int getTargetXp() { return targetXp; }
}
