package com.udderlywet.osrsstrategist;

import java.util.Locale;
import net.runelite.api.Skill;

/**
 * The recommendation Strategist is currently watching for natural completion.
 *
 * <p>The player does not have to press Do This. If this recommendation remains
 * active and the live account reaches its target, the milestone is considered
 * complete automatically.</p>
 */
public final class TrackedMilestone
{
    private final String activityId;
    private final String title;
    private final String skillName;
    private final int startedAtLevel;
    private final int targetLevel;

    public TrackedMilestone(
            String activityId,
            String title,
            String skillName,
            int startedAtLevel,
            int targetLevel)
    {
        this.activityId = activityId;
        this.title = title;
        this.skillName = skillName;
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

    public String getSkillName()
    {
        return skillName;
    }

    public int getStartedAtLevel()
    {
        return startedAtLevel;
    }

    public int getTargetLevel()
    {
        return targetLevel;
    }

    public Skill getSkill()
    {
        if (skillName == null)
        {
            return null;
        }

        try
        {
            return Skill.valueOf(skillName.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex)
        {
            return null;
        }
    }
}
