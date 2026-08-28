package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;

/** Immutable render-safe view of the current local progress session. */
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

    public long getStartedAtMillis() { return startedAtMillis; }
    public long getUpdatedAtMillis() { return updatedAtMillis; }
    public long getSessionDurationMillis()
    {
        return Math.max(0L, updatedAtMillis - startedAtMillis);
    }
    public long getActiveDurationMillis() { return activeDurationMillis; }
    public Map<Skill, SkillSessionProgress> getSkills() { return skills; }
    public List<ProgressTimeBucket> getBuckets() { return buckets; }
    public List<ProgressMilestone> getMilestones() { return milestones; }
    public ProgressTargetProjection getTargetProjection()
    {
        return targetProjection;
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
