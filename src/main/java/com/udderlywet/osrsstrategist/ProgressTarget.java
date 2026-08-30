package com.udderlywet.osrsstrategist;

import lombok.Getter;

import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** The skill checkpoint currently being executed by the active plan. */
public final class ProgressTarget
{
    @Getter
    private final String activityId;
    @Getter
    private final String methodId;
    @Getter
    private final Skill skill;
    @Getter
    private final int targetLevel;
    @Getter
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

}
