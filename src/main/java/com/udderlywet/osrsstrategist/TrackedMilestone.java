package com.udderlywet.osrsstrategist;

import java.util.Locale;

import lombok.Getter;

import net.runelite.api.Skill;

/** The recommendation Compass is currently watching for natural completion. */
public final class TrackedMilestone
{
    @Getter
    private final String activityId;
    @Getter
    private final String title;
    @Getter
    private final String skillName;
    @Getter
    private final int startedAtLevel;
    @Getter
    private final int targetLevel;
    @Getter
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
