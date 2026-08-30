package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

import net.runelite.api.Skill;

/** Immutable render-safe view of the current local progress session. */
public final class ProgressSessionSnapshot
{
    @Getter
    private final long startedAtMillis;
    @Getter
    private final long updatedAtMillis;
    @Getter
    private final long activeDurationMillis;
    @Getter
    private final Map<Skill, SkillSessionProgress> skills;
    @Getter
    private final List<ProgressTimeBucket> buckets;
    @Getter
    private final List<ProgressMilestone> milestones;
    @Getter
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
        long result = 0L;
        for (SkillSessionProgress progress : skills.values())
            result += progress.getXpGained();
        return result;
    }

    public int getLevelsGained()
    {
        int result = 0;
        for (SkillSessionProgress progress : skills.values())
            result += progress.getLevelsGained();
        return result;
    }
}
