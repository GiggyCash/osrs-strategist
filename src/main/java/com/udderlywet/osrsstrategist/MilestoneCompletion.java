package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;

/**
 * One detected completion event shown briefly in the sidebar.
 */
public final class MilestoneCompletion
{
    private final String activityId;
    private final String title;
    private final Skill skill;
    private final int startedAtLevel;
    private final int targetLevel;

    public MilestoneCompletion(
            String activityId,
            String title,
            Skill skill,
            int startedAtLevel,
            int targetLevel)
    {
        this.activityId = activityId;
        this.title = title;
        this.skill = skill;
        this.startedAtLevel = startedAtLevel;
        this.targetLevel = targetLevel;
    }

    public String getActivityId()
    {
        return activityId;
    }

    public String getTitle()
    {
        return title;
    }

    public Skill getSkill()
    {
        return skill;
    }

    public int getStartedAtLevel()
    {
        return startedAtLevel;
    }

    public int getTargetLevel()
    {
        return targetLevel;
    }
}
