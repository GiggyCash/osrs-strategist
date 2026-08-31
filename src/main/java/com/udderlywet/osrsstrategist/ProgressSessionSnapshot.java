package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

import net.runelite.api.Skill;

/** Immutable render-safe view of the current local progress session. */
@Getter
public final class ProgressSessionSnapshot
{
    private final long startedAtMillis;
    private final long updatedAtMillis;
    private final long activeDurationMillis;
    private final Map<Skill, SkillSessionProgress> skills;
    private final List<ProgressTimeBucket> buckets;
    private final List<ProgressMilestone> milestones;
    private final ProgressTargetProjection targetProjection;

    ProgressSessionSnapshot(
            long startedAtMillis,
            long updatedAtMillis,
            long activeDurationMillis,
            Map<Skill, SkillSessionProgress> skills,
            List<ProgressTimeBucket> buckets,
            List<ProgressMilestone> milestones,
            ProgressTargetProjection targetProjection)
    {
        this.startedAtMillis = startedAtMillis;
        this.updatedAtMillis = updatedAtMillis;
        this.activeDurationMillis = Math.max(0L, activeDurationMillis);
        EnumMap<Skill, SkillSessionProgress> skillCopy =
                new EnumMap<>(Skill.class);
        if (skills != null) skillCopy.putAll(skills);
        this.skills = Collections.unmodifiableMap(skillCopy);
        this.buckets = Collections.unmodifiableList(new ArrayList<>(buckets));
        this.milestones = Collections.unmodifiableList(
                new ArrayList<>(milestones));
        this.targetProjection = targetProjection;
    }

    public long getSessionDurationMillis()
    {
        return Math.max(0L, updatedAtMillis - startedAtMillis);
    }

    public long getTotalXpGained()
    {
        var result = 0L;
        for (SkillSessionProgress progress : skills.values())
            result += progress.getXpGained();
        return result;
    }

    public int getLevelsGained()
    {
        var result = 0;
        for (SkillSessionProgress progress : skills.values())
            result += progress.getLevelsGained();
        return result;
    }
}
