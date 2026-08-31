package com.udderlywet.osrsstrategist;

import java.util.Locale;

import lombok.Getter;

import net.runelite.api.Skill;

/** The recommendation Compass is currently watching for natural completion. */
@Getter
public final class TrackedMilestone
{
    private final String activityId;
    private final String title;
    private final String skillName;
    private final int startedAtLevel;
    private final int targetLevel;
    private final boolean progressionProtected;

    public TrackedMilestone(
            String activityId,
            String title,
            String skillName,
            int startedAtLevel,
            int targetLevel)
    {
        this(activityId, title, skillName, startedAtLevel,
                targetLevel, false);
    }

    public TrackedMilestone(
            String activityId,
            String title,
            String skillName,
            int startedAtLevel,
            int targetLevel,
            boolean progressionProtected)
    {
        this.activityId = activityId;
        this.title = title;
        this.skillName = skillName;
        this.startedAtLevel = startedAtLevel;
        this.targetLevel = targetLevel;
        this.progressionProtected = progressionProtected;
    }


    public Skill getSkill()
    {
        if (skillName == null) return null;
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
